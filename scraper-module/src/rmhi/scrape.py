import asyncio
import json
import logging
import os
import re
from datetime import datetime
from typing import Dict, List, Optional, Set

import aiohttp
import httpx
import requests
from bs4 import BeautifulSoup

from .db import (
    get_discovered_hackathons,
    get_ended_hackathons,
    get_existing_hackathon_urls,
    insert_into_hackathon,
    insert_into_hackathon_scrape_run,
    insert_into_project,
    update_states_to_ended,
)

logger = logging.getLogger(__name__)


def env_int(name: str, default: int) -> int:
    value = os.getenv(name, str(default)).strip()
    try:
        return int(value)
    except ValueError:
        return default


DEVPOST_API_URL = "https://devpost.com/api/hackathons"
batch_limit = env_int("SCRAPE_BATCH_LIMIT", 500)
start_page = env_int("DEVPOST_START_PAGE", 1)
end_page = env_int("DEVPOST_END_PAGE", 1500)
project_page_limit = env_int("PROJECT_PAGE_LIMIT", 1000)
max_ended_hackathons = env_int("MAX_ENDED_HACKATHONS", 0)


async def scrape_projects_from_gallery(url: str, page: int) -> List[str]:
    target_url = f"{url}?page={page}"
    project_urls: List[str] = []

    try:
        timeout = aiohttp.ClientTimeout(total=30)
        async with aiohttp.ClientSession(timeout=timeout) as session:
            async with session.get(target_url) as response:
                response.raise_for_status()
                html_content = await response.text()
    except aiohttp.ClientError as e:
        logger.error(f"Failed to fetch gallery {target_url}: {e}")
        return []

    soup = BeautifulSoup(html_content, "html.parser")
    link_elements = soup.select("a.block-wrapper-link.link-to-software")

    for link in link_elements:
        href = link.get("href")
        if href:
            project_urls.append(href)

    return project_urls


async def scrape_project_info(project_url: str) -> Optional[Dict[str, str]]:
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    }
    async with httpx.AsyncClient() as client:
        try:
            response = await client.get(
                project_url, headers=headers, follow_redirects=True, timeout=15
            )
            response.raise_for_status()
        except httpx.RequestError as e:
            logger.error(f"Error fetching URL {project_url}: {e}")
            return None

    soup = BeautifulSoup(response.text, "html.parser")

    def get_text_safely(element):
        return element.get_text(strip=True) if element else ""

    project_name = get_text_safely(soup.find("h1", id="app-title"))
    short_description = get_text_safely(soup.select_one("#software-header p.large"))

    if not project_name:
        logger.warning(f"Missing project name for URL {project_url}")
        return None

    details_container = soup.find("div", id="app-details-left")
    problem_description = ""
    solution = ""
    technical_details = ""

    if details_container:

        def extract_section(header_pattern: str) -> str:
            start_node = details_container.find(
                lambda tag: tag.name in ["h2", "p"]
                and re.search(header_pattern, tag.get_text(), re.IGNORECASE)
            )
            if not start_node:
                return ""

            content_parts = []
            for sibling in start_node.find_next_siblings():
                if sibling.name == "h2" or (
                    sibling.name == "p"
                    and sibling.get_text(strip=True).startswith("##")
                ):
                    break
                content_parts.append(sibling.get_text(separator="\n", strip=True))
            return "\n".join(content_parts).strip()

        problem_description = extract_section(r"Inspiration")
        solution = extract_section(r"What it does")
        technical_details = extract_section(r"How we built it")

    return {
        "project_url": project_url,
        "project_name": project_name,
        "short_description": short_description,
        "problem_description": problem_description,
        "solution": solution,
        "technical_details": technical_details,
    }


async def scrape_hackathon_projects(project_gallery_url: str) -> None:
    logger.info(f"Scraping projects from {project_gallery_url}")
    try:
        for i in range(1, project_page_limit + 1):
            project_urls = await scrape_projects_from_gallery(project_gallery_url, i)
            logger.info(f"Found {len(project_urls)} projects on page {i}")
            if len(project_urls) == 0:
                break

            hackathon_projects = []
            for url in project_urls:
                project_info = await scrape_project_info(url)
                if project_info is None or len(project_info) == 0:
                    logger.error(f"Failed to scrape project info for URL {url}")
                    continue
                hackathon_projects.append(project_info)

                if len(hackathon_projects) >= batch_limit:
                    await insert_into_project(hackathon_projects)
                    logger.info(
                        f"Inserted {len(hackathon_projects)} projects into the database"
                    )
                    hackathon_projects.clear()
            if hackathon_projects:
                await insert_into_project(hackathon_projects)
                logger.info(
                    f"Inserted {len(hackathon_projects)} projects into the database"
                )
                hackathon_projects.clear()

    except Exception as e:
        logger.error(
            f"An error occurred while fetching the URL {project_gallery_url}: {e}"
        )


async def scrape_devpost_daily_with_seen_stop() -> None:
    seen_urls: Set[str] = set()
    should_stop_after_consecutive_empty = 3
    consecutive_without_new = 0
    count = 0
    hackathon_data: List[Dict] = []
    pending_tasks: List[asyncio.Task] = []
    scrape_started = datetime.now()

    for i in range(start_page, end_page):
        logger.info(f"[SCRAPE_DEVPOST-DAILY] Scraping page {i}")
        try:
            request = await asyncio.to_thread(
                requests.get, DEVPOST_API_URL + f"?page={i}", timeout=20
            )
            result = request.json()
            hackathons = result.get("hackathons", [])

            if not hackathons:
                break

            page_candidates: List[Dict] = []
            page_urls: List[str] = []
            new_on_this_page = 0
            for hackathon in hackathons:
                url = hackathon.get("url")
                name = hackathon.get("title") or "No Name Provided"
                project_gallery_url = hackathon.get("submission_gallery_url") or (
                    url + "/project-gallery" if url else None
                )

                if not url or not project_gallery_url:
                    continue

                page_candidates.append(
                    {
                        "url": url,
                        "name": name,
                        "project_gallery_url": project_gallery_url,
                    }
                )
                page_urls.append(url)

            existing_urls = await get_existing_hackathon_urls(page_urls)
            for hackathon in page_candidates:
                url = hackathon["url"]
                if url in existing_urls or url in seen_urls:
                    continue
                seen_urls.add(url)
                new_on_this_page += 1
                hackathon_data.append(hackathon)

            count += len(hackathons)

            if new_on_this_page == 0:
                consecutive_without_new += 1
            else:
                consecutive_without_new = 0

            if len(hackathon_data) >= batch_limit:
                data_copy = hackathon_data.copy()
                task = asyncio.create_task(insert_into_hackathon(data_copy))
                pending_tasks.append(task)
                hackathon_data.clear()

            if consecutive_without_new >= should_stop_after_consecutive_empty:
                logger.info(
                    "[SCRAPE_DEVPOST-DAILY] Stopping after 3 pages without new hackathons"
                )
                break

            await asyncio.sleep(1)
        except (json.JSONDecodeError, requests.RequestException) as e:
            if hackathon_data:
                data_copy = hackathon_data.copy()
                task = asyncio.create_task(insert_into_hackathon(data_copy))
                pending_tasks.append(task)
                hackathon_data.clear()

            if pending_tasks:
                await asyncio.gather(*pending_tasks, return_exceptions=True)

            await insert_into_hackathon_scrape_run(
                scrape_started,
                datetime.now(),
                "daily",
                count,
                0,
                "error",
                "error",
                str(e),
            )
            return

    if hackathon_data:
        data_copy = hackathon_data.copy()
        task = asyncio.create_task(insert_into_hackathon(data_copy))
        pending_tasks.append(task)
        hackathon_data.clear()

    if pending_tasks:
        await asyncio.gather(*pending_tasks, return_exceptions=True)

    await insert_into_hackathon_scrape_run(
        scrape_started,
        datetime.now(),
        "daily",
        count,
        0,
        "end_of_pages",
        "success",
        "",
    )


async def flip_state_to_ended() -> None:
    if os.getenv("SKIP_STATE_FLIP", "").strip().lower() in {"1", "true", "yes"}:
        logger.info("[STATE_FLIP] Skipping state flip due to SKIP_STATE_FLIP")
        return

    max_checks = 0
    max_checks_raw = os.getenv("MAX_DISCOVERED_HACKATHONS", "0").strip()
    if max_checks_raw:
        try:
            max_checks = int(max_checks_raw)
        except ValueError:
            max_checks = 0

    try:
        project_gallery_urls = await get_discovered_hackathons()
        if not project_gallery_urls:
            return
        if max_checks > 0:
            project_gallery_urls = project_gallery_urls[:max_checks]

        hackathon_ended = []
        for url in project_gallery_urls:
            projects = await scrape_projects_from_gallery(url, 1)
            if len(projects) != 0:
                hackathon_ended.append(url)

        if not hackathon_ended:
            return

        await update_states_to_ended(hackathon_ended)
    except Exception as e:
        logger.error(f"Failed to flip hackathon states to ended: {e}")


async def run_daily_scrape() -> None:
    await scrape_devpost_daily_with_seen_stop()
    await flip_state_to_ended()

    project_gallery_urls = await get_ended_hackathons()
    if not project_gallery_urls:
        return
    if max_ended_hackathons > 0:
        project_gallery_urls = project_gallery_urls[:max_ended_hackathons]

    logger.info("[DAILY_SCRAPE] Starting daily scrape")
    for project_gallery_url in project_gallery_urls:
        await scrape_hackathon_projects(project_gallery_url)
    logger.info("[DAILY_SCRAPE] Finished daily scrape")


def _append_to_jsonl(filename: str, data_to_append: List[Dict]) -> None:
    try:
        with open(filename, "a", encoding="utf-8") as f:
            for item in data_to_append:
                f.write(json.dumps(item, ensure_ascii=False) + "\n")
    except (FileNotFoundError, json.JSONDecodeError) as e:
        logger.error(f"[APPEND_TO_JSONL] Failed to append to JSONL: {e}")


async def run_backfill_scrape() -> None:
    should_stop_scraping = False

    def handle_task_exception(task: asyncio.Task, hackathon_data=None) -> None:
        nonlocal should_stop_scraping
        try:
            task.result()
        except Exception as e:
            logger.error(f"[DB_ERROR] Failed to insert hackathons: {e}")

            if hackathon_data:
                _append_to_jsonl("failed_inserts.jsonl", hackathon_data)

            if any(
                keyword in str(e).lower()
                for keyword in ["connection", "pool", "timeout", "database"]
            ):
                logger.critical("[CRITICAL] Database connection issue.")
                should_stop_scraping = True

    hackathon_data = []
    count = 0
    pending_tasks: List[asyncio.Task] = []
    scrape_started = datetime.now()

    for i in range(start_page, end_page):
        if should_stop_scraping:
            logger.info("[SCRAPE_DEVPOST] Stopping due to critical database errors")
            break

        logger.info(f"[SCRAPE_DEVPOST] Scraping page {i}")
        try:
            request = await asyncio.to_thread(
                requests.get, DEVPOST_API_URL + f"?page={i}", timeout=20
            )
            result = request.json()
            hackathons = result["hackathons"]

            if len(hackathons) == 0:
                break

            for hackathon in hackathons:
                url = hackathon["url"]
                name = hackathon["title"] or "No Name Provided"
                project_gallery_url = (
                    hackathon["submission_gallery_url"] or url + "/project-gallery"
                )

                hackathon_data.append(
                    {
                        "url": url,
                        "name": name,
                        "project_gallery_url": project_gallery_url,
                    }
                )

            logger.info(
                f"[SCRAPE_DEVPOST] Found {len(hackathons)} hackathons on this page"
            )
            count += len(hackathons)

            if len(hackathon_data) >= batch_limit:
                logger.info(
                    f"[BATCH] Scheduling DB insert for {len(hackathon_data)} hackathons"
                )
                hackathon_data_copy = hackathon_data.copy()
                task = asyncio.create_task(insert_into_hackathon(hackathon_data_copy))
                task.add_done_callback(
                    lambda t: handle_task_exception(t, hackathon_data_copy)
                )
                pending_tasks.append(task)
                await asyncio.sleep(0)
                hackathon_data.clear()

            logger.info(f"[SCRAPE_DEVPOST] {count} hackathons scraped \n")
            await asyncio.sleep(1)
        except (json.JSONDecodeError, requests.RequestException) as e:
            if hackathon_data:
                hackathon_data_copy = hackathon_data.copy()
                task = asyncio.create_task(insert_into_hackathon(hackathon_data_copy))
                task.add_done_callback(
                    lambda t: handle_task_exception(t, hackathon_data_copy)
                )
                pending_tasks.append(task)
                await asyncio.sleep(0)
                hackathon_data.clear()

            if pending_tasks:
                await asyncio.gather(*pending_tasks, return_exceptions=True)

            await insert_into_hackathon_scrape_run(
                scrape_started,
                datetime.now(),
                "backfill",
                count,
                0,
                "error",
                "error",
                str(e),
            )
            return

    if hackathon_data:
        hackathon_data_copy = hackathon_data.copy()
        task = asyncio.create_task(insert_into_hackathon(hackathon_data_copy))
        task.add_done_callback(lambda t: handle_task_exception(t, hackathon_data_copy))
        pending_tasks.append(task)
        hackathon_data.clear()

    if pending_tasks:
        await asyncio.gather(*pending_tasks, return_exceptions=True)

    await insert_into_hackathon_scrape_run(
        scrape_started,
        datetime.now(),
        "backfill",
        count,
        0,
        "end_of_pages",
        "success",
        "",
    )

    await flip_state_to_ended()
