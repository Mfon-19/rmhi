from datetime import datetime
import logging
from typing import List, Dict, Tuple

from libs.common.db import db_conn

logger = logging.getLogger(__name__)
SEARCH_PATH = "ingest"


async def insert_into_hackathon(hackathons: List[Dict]) -> None:
    if not hackathons:
        return

    required_fields = ["url", "name", "project_gallery_url"]
    data_to_insert = []

    for hackathon in hackathons:
        for field in required_fields:
            if field not in hackathon or not hackathon.get(field):
                raise ValueError(f"Missing or empty required field: {field}")
        data_to_insert.append(
            (
                hackathon.get("url"),
                hackathon.get("name"),
                hackathon.get("project_gallery_url")
             )
        )

    logger.info(f"Attempting to insert {len(data_to_insert)} hackathons")
    sql_statement = "INSERT INTO ingest.hackathon (url, name, project_gallery_url) VALUES(%s, %s, %s);"

    try:
        async with db_conn(search_path=SEARCH_PATH) as conn:
            async with conn.transaction():
                async with conn.cursor() as cur:
                    await cur.executemany(sql_statement, data_to_insert)
            logger.info(f"Successfully inserted {len(hackathons)} hackathons")
    except Exception as e:
        logger.error(f"Failed to insert hackathons: {e}, batch size: {len(hackathons)}")
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
        return

    try:
        async with db_conn(search_path=SEARCH_PATH) as conn:
            await conn.execute(
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
    except Exception as e:
        logger.error(f"Failed to insert scrape run: {e}")
        raise


async def get_discovered_hackathons() -> List[str]:
    project_gallery_urls = []
    try:
        async with db_conn(search_path=SEARCH_PATH, readonly=True) as conn:
            async with conn.cursor() as cur:
                await cur.execute(
                    """
                        SELECT project_gallery_url FROM ingest.hackathon WHERE state = 'discovered';
                    """
                )
                async for row in cur:
                    project_gallery_urls.append(row[0])
    except Exception as e:
        logger.error(f"Failed to get discovered hackathons: {e}")
        raise

    return project_gallery_urls


async def update_states_to_ended(project_gallery_urls: List[str]) -> None:
    if not project_gallery_urls:
        return

    sql_statement = "UPDATE ingest.hackathon SET state = 'ended' WHERE project_gallery_url = %s;"
    try:
        async with db_conn() as conn:
            async with conn.transaction():
                async with conn.cursor() as cur:
                    await cur.executemany(sql_statement, project_gallery_urls)
    except Exception as e:
        logger.error(f"Failed to flip hackathon states to ended: {e}")
        raise
