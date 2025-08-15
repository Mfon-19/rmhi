from datetime import datetime
import logging
from libs.common.db import db_conn

logger = logging.getLogger(__name__)


async def insert_into_hackathon(hackathons) -> None:
    if not hackathons:
        return

    required_fields = ["url", "name", "project_gallery_url"]
    logger.info(f"Attempting to insert {len(hackathons)} hackathons")

    with db_conn() as conn:
        try:
            for hackathon in hackathons:
                for field in required_fields:
                    if field not in hackathon or not hackathon[field]:
                        raise ValueError(f"Missing or empty required field: {field}")

                conn.execute(
                    """
                        INSERT INTO ingest.hackathon (url, name, project_gallery_url) VALUES(%s, %s, %s);
                    """,
                    (
                        hackathon["url"],
                        hackathon["name"],
                        hackathon["project_gallery_url"],
                    ),
                    prepare=True,
                )
            logger.info(f"Successfully inserted {len(hackathons)} hackathons")
        except Exception as e:
            logger.error(
                f"Failed to insert hackathons: {e}, batch size: {len(hackathons)}"
            )
            raise


async def insert_into_hackathon_scrape_run(
    started_at: datetime,
    finished_at: datetime,
    mode: str,
    pages_scanned: int,
    new_count: int,
    stop_reason: str,
    status: str,
    error: str,
) -> None:
    if any(
        param is None
        for param in [
            started_at,
            finished_at,
            mode,
            pages_scanned,
            new_count,
            stop_reason,
            status,
            error,
        ]
    ):
        logger.warning("Skipping scrape run insert due to None parameters")
        return

    logger.info(
        f"Recording scrape run: {mode}, {pages_scanned} pages, {new_count} new items"
    )

    with db_conn() as conn:
        try:
            conn.execute(
                """
                    INSERT INTO ingest.hackathon_scrape_run 
                    (started_at, finished_at, mode, pages_scanned, new_count, stop_reason, status, error) 
                    VALUES(%s, %s, %s, %s, %s, %s, %s, %s);
                """,
                (
                    started_at,
                    finished_at,
                    mode,
                    pages_scanned,
                    new_count,
                    stop_reason,
                    status,
                    error,
                ),
                prepare=False,
            )
            logger.info("Successfully recorded scrape run")
        except Exception as e:
            logger.error(f"Failed to insert scrape run: {e}")
            raise
