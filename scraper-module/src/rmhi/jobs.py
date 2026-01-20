import logging

from .scrape import run_daily_scrape, run_backfill_scrape
from .transform import run_transformations, run_transformations_if_pending

logger = logging.getLogger(__name__)


async def run_daily() -> None:
    logger.info("Running daily scrape job.")
    await run_daily_scrape()


async def run_backfill() -> None:
    logger.info("Running backfill scrape job.")
    await run_backfill_scrape()


async def run_transform() -> None:
    logger.info("Running transform job.")
    await run_transformations()


async def run_transform_check() -> None:
    logger.info("Running transform check job.")
    await run_transformations_if_pending()
