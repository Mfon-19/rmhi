import aiohttp
from bs4 import BeautifulSoup
from typing import List


async def scrape_projects_from_gallery(url: str, page: int) -> List[str]:
    """
    Scrapes project URLs from a given project gallery URL and page number.
    """
    target_url = f"{url}?page={page}"
    project_urls: List[str] = []

    try:
        timeout = aiohttp.ClientTimeout(total=30)
        async with aiohttp.ClientSession(timeout=timeout) as session:
            async with session.get(target_url) as response:
                response.raise_for_status()
                html_content = await response.text()

    except aiohttp.ClientError as e:
        print(f"An error occurred while fetching the URL {target_url}: {e}")
        return []

    soup = BeautifulSoup(html_content, "html.parser")
    link_elements = soup.select("a.block-wrapper-link.link-to-software")

    for link in link_elements:
        href = link.get("href")
        if href:
            project_urls.append(href)

    return project_urls
