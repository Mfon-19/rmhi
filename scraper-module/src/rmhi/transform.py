import asyncio
import json
import logging
import os
from typing import Dict, Any, List, Optional

from google import genai
from google.genai import types

from .db import (
    advisory_lock,
    get_untransformed_projects,
    has_untransformed_projects,
    insert_transformed_projects_and_update_flags,
)
logger = logging.getLogger(__name__)

try:
    from google.api_core.exceptions import ResourceExhausted, TooManyRequests

    _QUOTA_EXCEPTION_TYPES = (ResourceExhausted, TooManyRequests)
except Exception:
    _QUOTA_EXCEPTION_TYPES = ()

from .settings import get_settings

_GENAI_CLIENT: Optional[genai.Client] = None

TRANSFORM_LOCK_KEY = 9123845
DEFAULT_MODEL_NAME = "gemini-2.5-flash"
DEFAULT_GEMINI_RPM = 5

PROMPT = """
You will receive project ideas in json that I scraped from a public site.
Goal: Create one fresh, idea for each json that keeps the source's core purpose but avoids any copied wording or obvious cloning.
Instructions:
Extract Essence – In 1-2 sentences, restate the fundamental problem the original idea solves.
Generate One Variation – Propose a single new concept that:
- pursues the same high-level objective,
- introduces at least one meaningful change (different audience, added feature, new tech stack, alternate domain, etc.),
- and is written entirely in your own words.
Rate Idea – Considering novelty, feasibility, potential impact, and technical complexity, give your generated idea an overall score out of 10 (10 = outstanding, 1 = very weak). Provide this number in the "rating" field.
"""

RESPONSE_SCHEMA = {
    "type": "object",
    "properties": {
        "project_name": {"type": "string"},
        "short_description": {"type": "string"},
        "created_by": {"type": "string"},
        "problem_description": {"type": "string"},
        "solution": {"type": "string"},
        "technical_details": {"type": "string"},
        "comments": {"type": "array", "items": {"type": "string"}},
        "likes": {"type": "integer"},
        "technologies": {"type": "array", "items": {"type": "string"}},
        "categories": {"type": "array", "items": {"type": "string"}},
        "rating": {"type": "number"},
    },
    "required": [
        "project_name",
        "short_description",
        "problem_description",
        "solution",
        "rating",
    ],
}

GENERATION_CONFIG = types.GenerateContentConfig(
    response_mime_type="application/json",
    response_schema=RESPONSE_SCHEMA,
)

QUOTA_ERROR_TOKENS = (
    "resource_exhausted",
    "quota",
    "too many requests",
    "rate limit",
    "429",
)


def _get_model_name() -> str:
    model_name = os.getenv("GEMINI_MODEL", "").strip()
    normalized = model_name or DEFAULT_MODEL_NAME
    if normalized.startswith("models/"):
        return normalized.split("/", 1)[1]
    return normalized


def _get_rate_limit_rpm() -> int:
    raw = os.getenv("GEMINI_RPM", str(DEFAULT_GEMINI_RPM)).strip()
    try:
        value = int(raw)
    except ValueError:
        value = DEFAULT_GEMINI_RPM
    return max(1, value)


class RateLimiter:
    def __init__(self, rpm: int) -> None:
        self._min_interval = 60.0 / rpm
        self._lock = asyncio.Lock()
        self._last_call = 0.0

    async def wait(self) -> None:
        async with self._lock:
            now = asyncio.get_running_loop().time()
            if self._last_call:
                wait_for = self._min_interval - (now - self._last_call)
                if wait_for > 0:
                    await asyncio.sleep(wait_for)
            self._last_call = asyncio.get_running_loop().time()


def _ensure_genai_configured() -> None:
    global _GENAI_CLIENT
    if _GENAI_CLIENT is not None:
        return
    cfg = get_settings()
    google_api_key = os.getenv("GOOGLE_API_KEY", cfg.GOOGLE_API_KEY or "")
    if not google_api_key:
        raise ValueError("GOOGLE_API_KEY environment variable not set.")
    _GENAI_CLIENT = genai.Client(api_key=google_api_key)


def _get_client() -> genai.Client:
    _ensure_genai_configured()
    assert _GENAI_CLIENT is not None
    return _GENAI_CLIENT


def _is_quota_error(error: Exception) -> bool:
    if _QUOTA_EXCEPTION_TYPES and isinstance(error, _QUOTA_EXCEPTION_TYPES):
        return True
    message = str(error).lower()
    return any(token in message for token in QUOTA_ERROR_TOKENS)


def _is_model_error(error: Exception) -> bool:
    message = str(error).lower()
    return "model" in message and "not found" in message


def _get_response_text(response: Any) -> str:
    text = getattr(response, "text", None)
    if text:
        return text
    try:
        candidates = getattr(response, "candidates", None) or []
        for candidate in candidates:
            content = getattr(candidate, "content", None)
            parts = getattr(content, "parts", None) or []
            for part in parts:
                part_text = getattr(part, "text", None)
                if part_text:
                    return part_text
    except Exception:
        return ""
    return ""


async def transform_project(
    project_data: Dict[str, Any],
    semaphore: asyncio.Semaphore,
    rate_limiter: RateLimiter,
    stop_event: asyncio.Event,
) -> Dict[str, Any] | None:
    async with semaphore:
        try:
            if stop_event.is_set():
                return None
            await rate_limiter.wait()
            if stop_event.is_set():
                return None
            model_name = _get_model_name()
            input_text = f"{PROMPT}\n\nHere is the project data to transform:\n{json.dumps(project_data, indent=2)}"
            client = _get_client()
            response = await asyncio.to_thread(
                client.models.generate_content,
                model=model_name,
                contents=input_text,
                config=GENERATION_CONFIG,
            )

            response_text = _get_response_text(response)
            transformed_data = json.loads(response_text)
            transformed_data["original_project_id"] = project_data["id"]
            return transformed_data
        except Exception as e:
            if _is_quota_error(e):
                logger.warning(
                    f"Quota exhausted while transforming project ID {project_data.get('id')}: {e}"
                )
                raise
            if _is_model_error(e):
                logger.critical(
                    f"Invalid model configured ({_get_model_name()}): {e}"
                )
                raise
            if "API_KEY" in str(e).upper() or "PERMISSION_DENIED" in str(e).upper():
                logger.critical(
                    f"Critical API Error: {e}. This is likely a configuration issue."
                )
                raise
            logger.error(
                f"Failed to transform project ID {project_data.get('id')}: {e}"
            )
            return None


async def _run_transformations_loop() -> None:
    logger.info("Starting transformation process...")
    _ensure_genai_configured()
    rpm_limit = _get_rate_limit_rpm()
    concurrency_limit = min(
        max(1, int(os.getenv("GEMINI_CONCURRENCY", "1").strip() or "1")),
        rpm_limit,
    )
    batch_size = 100
    semaphore = asyncio.Semaphore(concurrency_limit)
    rate_limiter = RateLimiter(rpm_limit)

    while True:
        try:
            projects_to_transform = await get_untransformed_projects(
                batch_size=batch_size
            )
            if not projects_to_transform:
                logger.info("No more projects to transform. Exiting.")
                break

            logger.info(f"Fetched {len(projects_to_transform)} projects to transform.")

            stop_event = asyncio.Event()
            tasks = {
                asyncio.create_task(
                    transform_project(p, semaphore, rate_limiter, stop_event)
                )
                for p in projects_to_transform
            }
            successful_transformations: List[Dict[str, Any]] = []
            while tasks:
                done, tasks = await asyncio.wait(
                    tasks, return_when=asyncio.FIRST_COMPLETED
                )
                for task in done:
                    try:
                        result = task.result()
                    except asyncio.CancelledError:
                        continue
                    except Exception as exc:
                        stop_event.set()
                        if _is_quota_error(exc):
                            logger.warning(
                                "Quota exhausted. Stopping immediately to retry later."
                            )
                        else:
                            logger.critical(
                                f"Caught a critical exception: {exc}. Shutting down after this batch."
                            )
                        for pending in tasks:
                            pending.cancel()
                        if tasks:
                            await asyncio.gather(*tasks, return_exceptions=True)
                        if successful_transformations:
                            await insert_transformed_projects_and_update_flags(
                                successful_transformations
                            )
                        return

                    if result is not None:
                        successful_transformations.append(result)

            if successful_transformations:
                logger.info(
                    f"Successfully transformed {len(successful_transformations)} projects in this batch."
                )
                await insert_transformed_projects_and_update_flags(
                    successful_transformations
                )
            else:
                logger.warning(
                    "No projects were successfully transformed in this batch."
                )

        except Exception as e:
            logger.error(
                f"An unexpected error occurred in the main transformation loop: {e}"
            )
            await asyncio.sleep(30)

    logger.info("Transformation process finished.")


async def run_transformations() -> None:
    async with advisory_lock(TRANSFORM_LOCK_KEY) as acquired:
        if not acquired:
            logger.info("Transform already running. Exiting.")
            return
        await _run_transformations_loop()


async def run_transformations_if_pending() -> None:
    logger.info("Checking for untransformed projects...")
    if not await has_untransformed_projects():
        logger.info("No untransformed projects found. Exiting.")
        return
    await run_transformations()
