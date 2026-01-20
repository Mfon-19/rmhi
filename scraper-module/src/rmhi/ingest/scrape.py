import asyncio
import logging
import httpx
import re
import json
import os
import requests
from datetime import datetime
from bs4 import BeautifulSoup
from typing import List, Dict, Optional, Set
from .state import flip_state_to_ended
from .gallery import scrape_projects_from_gallery
from .db_ops import (
    get_ended_hackathons,
    insert_into_project,
    get_existing_hackathon_urls,
    insert_into_hackathon,
    insert_into_hackathon_scrape_run,
)

logger = logging.getLogger(__name__)


def env_int(name: str, default: int) -> int:
    value = os.getenv(name, str(default)).strip()
    try:
        return int(value)
    except ValueError:
        return default


batch_limit = env_int("SCRAPE_BATCH_LIMIT", 500)
devpost_api_url = "https://devpost.com/api/hackathons"
start_page = env_int("DEVPOST_START_PAGE", 1)
end_page = env_int("DEVPOST_END_PAGE", 1500)
project_page_limit = env_int("PROJECT_PAGE_LIMIT", 1000)
max_ended_hackathons = env_int("MAX_ENDED_HACKATHONS", 0)


async def scrape_hackathon_projects(project_gallery_url: str):
    """
    Scrapes hackathon projects from a given project gallery URL.
    """
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
        return


async def scrape_project_info(project_url: str) -> Optional[Dict[str, str]]:
    """
    Scrapes project information from a given project URL.
    """
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
                requests.get, devpost_api_url + f"?page={i}", timeout=20
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

async def main():
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
        handlers=[
            logging.FileHandler("scrape.log"),
            logging.StreamHandler(),
        ],
    )

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

if __name__ == "__main__":
    asyncio.run(main())
