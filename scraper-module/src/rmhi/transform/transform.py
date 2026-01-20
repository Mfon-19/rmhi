import os
import json
import asyncio
import logging
import google.generativeai as genai
from typing import Dict, Any, List

from .db_ops import (
    get_untransformed_projects,
    insert_transformed_projects_and_update_flags,
)

logging.basicConfig(
    level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)


try:
    from dotenv import load_dotenv

    load_dotenv()
except ImportError:
    logger.info("dotenv not installed, skipping.")

GOOGLE_API_KEY = os.getenv("GOOGLE_API_KEY")
if not GOOGLE_API_KEY:
    raise ValueError("GOOGLE_API_KEY environment variable not set.")

genai.configure(api_key=GOOGLE_API_KEY)


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

generation_config = {
    "response_mime_type": "application/json",
    "response_schema": RESPONSE_SCHEMA,
}


async def transform_project(
    project_data: Dict[str, Any], semaphore: asyncio.Semaphore
) -> Dict[str, Any] | None:
    """
    Sends a single project to the Gemini API for transformation.
    """
    async with semaphore:
        try:
            model = genai.GenerativeModel(
                model_name="gemini-1.5-pro", generation_config=generation_config
            )
            input_text = f"{PROMPT}\n\nHere is the project data to transform:\n{json.dumps(project_data, indent=2)}"
            response = await model.generate_content_async(input_text)

            transformed_data = json.loads(response.text)
            transformed_data["original_project_id"] = project_data["id"]
            return transformed_data
        except Exception as e:
            if "API_KEY" in str(e).upper() or "PERMISSION_DENIED" in str(e).upper():
                logger.critical(
                    f"Critical API Error: {e}. This is likely a configuration issue."
                )
                raise
            logger.error(
                f"Failed to transform project ID {project_data.get('id')}: {e}"
            )
            return None


async def run_transformations():
    """
    Main function to fetch, transform, and store projects.
    """
    logger.info("Starting transformation process...")
    CONCURRENCY_LIMIT = 8
    BATCH_SIZE = 100
    semaphore = asyncio.Semaphore(CONCURRENCY_LIMIT)

    while True:
        try:
            projects_to_transform = await get_untransformed_projects(
                batch_size=BATCH_SIZE
            )
            if not projects_to_transform:
                logger.info("No more projects to transform. Exiting.")
                break

            logger.info(f"Fetched {len(projects_to_transform)} projects to transform.")

            tasks = [transform_project(p, semaphore) for p in projects_to_transform]
            results = await asyncio.gather(*tasks, return_exceptions=True)

            successful_transformations: List[Dict[str, Any]] = []
            for result in results:
                if isinstance(result, Exception):
                    logger.critical(
                        f"Caught a critical exception: {result}. Shutting down after this batch."
                    )
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


if __name__ == "__main__":
    asyncio.run(run_transformations())
