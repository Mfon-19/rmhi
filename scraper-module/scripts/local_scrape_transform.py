#!/usr/bin/env python3
import asyncio
import logging
import os
import sys
from pathlib import Path
from typing import Dict, List

import requests

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from rmhi.db import get_existing_hackathon_urls, insert_into_hackathon, insert_into_project
from rmhi.scrape import (
    DEVPOST_API_URL,
    scrape_project_info,
    scrape_projects_from_gallery,
)
from rmhi.transform import run_transformations


def env_int(name: str, default: int) -> int:
    raw = os.getenv(name, str(default)).strip()
    try:
        return int(raw)
    except ValueError:
        return default


HACKATHON_LIMIT = env_int("HACKATHON_LIMIT", 50)
START_PAGE = env_int("DEVPOST_START_PAGE", 1)
END_PAGE = env_int("DEVPOST_END_PAGE", 10)
PROJECTS_PER_HACKATHON = env_int("PROJECTS_PER_HACKATHON", 20)
PROJECT_CONCURRENCY = env_int("PROJECT_CONCURRENCY", 6)


async def fetch_hackathons(limit: int) -> List[Dict[str, str]]:
    collected: List[Dict[str, str]] = []

    for page in range(START_PAGE, END_PAGE + 1):
        logging.info("Fetching Devpost page %s", page)
        response = requests.get(f"{DEVPOST_API_URL}?page={page}", timeout=20)
        response.raise_for_status()
        payload = response.json()
        hackathons = payload.get("hackathons", [])
        if not hackathons:
            break

        page_candidates: List[Dict[str, str]] = []
        page_urls: List[str] = []
        for hackathon in hackathons:
            url = hackathon.get("url")
            name = hackathon.get("title") or "No Name Provided"
            gallery = hackathon.get("submission_gallery_url") or (
                f"{url}/project-gallery" if url else None
            )
            if not url or not gallery:
                continue
            page_candidates.append(
                {"url": url, "name": name, "project_gallery_url": gallery}
            )
            page_urls.append(url)

        existing = await get_existing_hackathon_urls(page_urls)
        for candidate in page_candidates:
            if candidate["url"] in existing:
                continue
            collected.append(candidate)
            if len(collected) >= limit:
                return collected

    return collected


async def scrape_projects(
    project_gallery_url: str, limit: int, semaphore: asyncio.Semaphore
) -> int:
    project_urls = await scrape_projects_from_gallery(project_gallery_url, 1)
    if not project_urls:
        return 0
    project_urls = project_urls[:limit]

    async def fetch(url: str):
        async with semaphore:
            return await scrape_project_info(url)

    tasks = [fetch(url) for url in project_urls]
    results = await asyncio.gather(*tasks, return_exceptions=True)

    projects: List[Dict[str, str]] = []
    for result in results:
        if isinstance(result, Exception) or result is None:
            continue
        projects.append(result)

    if projects:
        await insert_into_project(projects)
    return len(projects)


async def main() -> None:
    logging.basicConfig(
        level=os.getenv("LOG_LEVEL", "INFO").upper(),
        format="%(asctime)s - %(levelname)s - %(message)s",
    )

    logging.info("Scraping up to %s hackathons for local testing.", HACKATHON_LIMIT)
    hackathons = await fetch_hackathons(HACKATHON_LIMIT)
    if not hackathons:
        logging.warning("No hackathons found to scrape.")
        return

    await insert_into_hackathon(hackathons)

    semaphore = asyncio.Semaphore(PROJECT_CONCURRENCY)
    total_projects = 0
    for hackathon in hackathons:
        count = await scrape_projects(
            hackathon["project_gallery_url"], PROJECTS_PER_HACKATHON, semaphore
        )
        total_projects += count
        logging.info(
            "Scraped %s projects from %s", count, hackathon["project_gallery_url"]
        )

    logging.info("Scraped %s total projects. Starting transform.", total_projects)
    await run_transformations()
    logging.info("Done.")


if __name__ == "__main__":
    asyncio.run(main())
