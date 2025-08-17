import logging
import aiohttp
import httpx
import re
from bs4 import BeautifulSoup
from typing import List, Dict, Optional

from scraper.app.db_ops import insert_into_project

logger = logging.getLogger(__name__)
batch_limit = 500


async def scrape_hackathon_projects(project_gallery_url: str):
    logger.info(f"Scraping projects from {project_gallery_url}")
    try:
        for i in range(1, 1000):
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
                hackathon_projects.extend(project_info)

                if len(hackathon_projects) >= batch_limit:
                    await insert_into_project(hackathon_projects)
                    logger.info(
                        f"Inserted {len(hackathon_projects)} projects into the database"
                    )
                    hackathon_projects.clear()

    except Exception as e:
        logger.error(f"An error occurred while fetching the URL {project_gallery_url}: {e}")
        return


async def scrape_project_info(project_url: str) -> Optional[Dict[str, str]]:
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
    }
    async with httpx.AsyncClient() as client:
        try:
            response = await client.get(project_url, headers=headers, follow_redirects=True, timeout=15)
            response.raise_for_status()
        except httpx.RequestError as e:
            logger.error(f"Error fetching URL {project_url}: {e}")
            return None

    soup = BeautifulSoup(response.text, 'html.parser')

    def get_text_safely(element):
        return element.get_text(strip=True) if element else ""

    project_name = get_text_safely(soup.find('h1', id='app-title'))
    short_description = get_text_safely(soup.select_one('#software-header p.large'))

    details_container = soup.find('div', id='app-details-left')
    problem_description = ""
    solution = ""
    technical_details = ""

    if details_container:
        def extract_section(header_pattern: str) -> str:
            start_node = details_container.find(
                lambda tag: tag.name in ['h2', 'p'] and re.search(header_pattern, tag.get_text(), re.IGNORECASE)
            )
            if not start_node:
                return ""

            content_parts = []
            for sibling in start_node.find_next_siblings():
                if sibling.name == 'h2' or (sibling.name == 'p' and sibling.get_text(strip=True).startswith('##')):
                    break
                content_parts.append(sibling.get_text(separator='\n', strip=True))
            return '\n'.join(content_parts).strip()

        problem_description = extract_section(r'Inspiration')
        solution = extract_section(r'What it does')
        technical_details = extract_section(r'How we built it')

    return {
        "project_url": project_url,
        "project_name": project_name,
        "short_description": short_description,
        "problem_description": problem_description,
        "solution": solution,
        "technical_details": technical_details,
    }


async def scrape_projects_from_gallery(url: str, page: int) -> List[str]:
    target_url = f"{url}?page={page}"
    project_urls = []

    try:
        async with aiohttp.ClientSession() as session:
            async with session.get(target_url) as response:
                response.raise_for_status()
                html_content = await response.text()

    except aiohttp.ClientError as e:
        print(f"An error occurred while fetching the URL {target_url}: {e}")
        return []

    soup = BeautifulSoup(html_content, 'html.parser')
    link_elements = soup.select('a.block-wrapper-link.link-to-software')

    for link in link_elements:
        href = link.get('href')
        if href:
            project_urls.append(href)

    return project_urls
