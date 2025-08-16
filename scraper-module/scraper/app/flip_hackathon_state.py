from scraper.app.db_ops import get_discovered_hackathons, update_states_to_ended
from scraper.app.scrape import scrape_projects_from_gallery
import logging

logger = logging.getLogger(__name__)

async def flip_state_to_ended():
    try:
        project_gallery_urls = await get_discovered_hackathons()
        if not project_gallery_urls:
            return
        logger.info(f"Found {len(project_gallery_urls)} hackathons to check")
        hackathon_ended = []
        for i, url in enumerate(project_gallery_urls):
            logger.info(f"Checking projects from {url}")
            print(f"Checking projects from {url}. Count: {i}")
            projects = await scrape_projects_from_gallery(url, 1)
            if projects is not None:
                hackathon_ended.append(url)

        if not hackathon_ended:
            return

        await update_states_to_ended(hackathon_ended)
    except Exception as e:
        logger.error(f"Failed to flip hackathon states to ended: {e}")

if __name__ == "__main__":
    import asyncio
    asyncio.run(flip_state_to_ended())