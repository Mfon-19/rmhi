import logging

import os

from .db_ops import get_discovered_hackathons, update_states_to_ended
from .gallery import scrape_projects_from_gallery

logger = logging.getLogger(__name__)


async def flip_state_to_ended():
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
        for i, url in enumerate(project_gallery_urls):
            projects = await scrape_projects_from_gallery(url, 1)
            if len(projects) != 0:
                hackathon_ended.append(url)

        if not hackathon_ended:
            return

        await update_states_to_ended(hackathon_ended)
    except Exception as e:
        logger.error(f"Failed to flip hackathon states to ended: {e}")
