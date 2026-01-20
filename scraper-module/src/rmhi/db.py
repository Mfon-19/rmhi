import logging
import os
from contextlib import asynccontextmanager
from datetime import datetime
from typing import Optional, List, Dict, Tuple, Set, AsyncIterator

from psycopg_pool import AsyncConnectionPool

from .settings import get_settings

logger = logging.getLogger(__name__)
SEARCH_PATH = "ingest"

cfg = get_settings()
DSN = os.getenv("DB_DSN", cfg.DB_DSN)
POOL: Optional[AsyncConnectionPool] = None


def get_pool() -> AsyncConnectionPool:
    global POOL
    if POOL is None:
        POOL = AsyncConnectionPool(
            conninfo=DSN,
            min_size=1,
            max_size=8,
            max_idle=300,
            timeout=30,
            kwargs={
                "application_name": os.getenv("APP_NAME", "rmhi-scraper"),
                "options": "-c statement_timeout=15000 -c lock_timeout=2000 -c idle_in_transaction_session_timeout=10000",
                "keepalives": 1,
                "keepalives_idle": 30,
                "keepalives_interval": 10,
                "keepalives_count": 5,
            },
        )
    return POOL


@asynccontextmanager
async def db_conn(
    readonly: bool = False, search_path: Optional[str] = None
) -> AsyncIterator:
    pool = get_pool()
    async with pool.connection() as conn:
        if search_path:
            await conn.execute(f"SET search_path TO {search_path}")
        if readonly:
            await conn.execute("SET TRANSACTION READ ONLY")
        try:
            yield conn
            await conn.commit()
        except Exception:
            await conn.rollback()
            raise


@asynccontextmanager
async def advisory_lock(lock_key: int) -> AsyncIterator[bool]:
    pool = get_pool()
    async with pool.connection() as conn:
        async with conn.cursor() as cur:
            await cur.execute("SELECT pg_try_advisory_lock(%s);", (lock_key,))
            row = await cur.fetchone()
            acquired = bool(row and row[0])
        if not acquired:
            yield False
            return
        try:
            yield True
        finally:
            try:
                async with conn.cursor() as cur:
                    await cur.execute("SELECT pg_advisory_unlock(%s);", (lock_key,))
                await conn.commit()
            except Exception as exc:
                logger.warning(f"Failed to release advisory lock: {exc}")


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
                hackathon.get("project_gallery_url"),
            )
        )

    logger.info(f"Attempting to insert {len(data_to_insert)} hackathons")
    sql_statement = (
        "INSERT INTO ingest.hackathon (url, name, project_gallery_url) "
        "VALUES(%s, %s, %s) "
        "ON CONFLICT DO NOTHING;"
    )

    try:
        async with db_conn(search_path=SEARCH_PATH) as conn:
            async with conn.transaction():
                async with conn.cursor() as cur:
                    await cur.executemany(sql_statement, data_to_insert)
            logger.info(f"Successfully inserted {len(hackathons)} hackathons")
    except Exception as e:
        logger.error(f"Failed to insert hackathons: {e}, batch size: {len(hackathons)}")
        raise


async def get_existing_hackathon_urls(urls: List[str]) -> Set[str]:
    if not urls:
        return set()

    unique_urls = list({url for url in urls if url})
    if not unique_urls:
        return set()

    try:
        async with db_conn(search_path=SEARCH_PATH, readonly=True) as conn:
            async with conn.cursor() as cur:
                await cur.execute(
                    """
                        SELECT url FROM ingest.hackathon
                        WHERE url = ANY(%s);
                    """,
                    (unique_urls,),
                )
                rows = await cur.fetchall()
                return {row[0] for row in rows}
    except Exception as e:
        logger.error(f"Failed to query existing hackathon urls: {e}")
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


async def get_ended_hackathons() -> List[str]:
    project_gallery_urls = []
    try:
        async with db_conn(search_path=SEARCH_PATH, readonly=True) as conn:
            async with conn.cursor() as cur:
                await cur.execute(
                    """
                        SELECT project_gallery_url FROM ingest.hackathon WHERE state = 'ended';
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

    sql_statement = (
        "UPDATE ingest.hackathon SET state = 'ended' WHERE project_gallery_url = %s;"
    )
    try:
        async with db_conn() as conn:
            async with conn.transaction():
                async with conn.cursor() as cur:
                    await cur.executemany(
                        sql_statement, [(url,) for url in project_gallery_urls]
                    )
    except Exception as e:
        logger.error(f"Failed to flip hackathon states to failed: {e}")
        raise


async def insert_into_project(projects: List[Dict]) -> None:
    if not projects:
        return

    data_to_insert = []
    required_fields = [
        "project_url",
        "project_name",
    ]
    for project in projects:
        for field in required_fields:
            if field not in project or not project.get(field):
                raise ValueError(f"Missing or empty required field: {field}")
        data_to_insert.append(
            (
                project.get("project_url"),
                project.get("project_name"),
                project.get("short_description"),
                project.get("problem_description"),
                project.get("solution"),
                project.get("technical_details"),
                False,
            )
        )

    sql_statement = "INSERT INTO ingest.project (project_url, project_name, short_description, problem_description, solution, technical_details, transformed) VALUES(%s, %s, %s, %s, %s, %s, %s);"
    try:
        async with db_conn(search_path=SEARCH_PATH) as conn:
            async with conn.transaction():
                async with conn.cursor() as cur:
                    await cur.executemany(sql_statement, data_to_insert)
    except Exception as e:
        logger.error(f"Failed to insert projects: {e}, batch size: {len(projects)}")
        raise


async def get_untransformed_projects(batch_size: int = 100) -> List[Dict]:
    projects: List[Dict] = []
    sql_statement = """
        SELECT id, project_name, short_description, problem_description, solution, technical_details
        FROM ingest.project
        WHERE transformed = FALSE
        LIMIT %s;
    """
    try:
        async with db_conn(search_path=SEARCH_PATH, readonly=True) as conn:
            async with conn.cursor() as cur:
                await cur.execute(sql_statement, (batch_size,))
                rows = await cur.fetchall()
                for row in rows:
                    projects.append(
                        {
                            "id": row[0],
                            "project_name": row[1],
                            "short_description": row[2],
                            "problem_description": row[3],
                            "solution": row[4],
                            "technical_details": row[5],
                        }
                    )
    except Exception as e:
        logger.error(f"Failed to get untransformed projects: {e}")
        raise
    return projects


async def has_untransformed_projects() -> bool:
    sql_statement = """
        SELECT 1
        FROM ingest.project
        WHERE transformed = FALSE
        LIMIT 1;
    """
    try:
        async with db_conn(search_path=SEARCH_PATH, readonly=True) as conn:
            async with conn.cursor() as cur:
                await cur.execute(sql_statement)
                row = await cur.fetchone()
                return row is not None
    except Exception as e:
        logger.error(f"Failed to check for untransformed projects: {e}")
        raise


async def insert_transformed_projects_and_update_flags(
    transformed_projects: List[Dict],
) -> None:
    if not transformed_projects:
        return

    insert_statement = """
        INSERT INTO ingest.transformed_project (
            original_project_id, project_name, short_description, created_by,
            problem_description, solution, technical_details, comments,
            likes, technologies, categories, rating
        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s);
    """

    update_statement = "UPDATE ingest.project SET transformed = TRUE WHERE id = %s;"

    insert_data: List[Tuple] = []
    update_ids: List[Tuple] = []

    for project in transformed_projects:
        insert_data.append(
            (
                project.get("original_project_id"),
                project.get("project_name"),
                project.get("short_description"),
                project.get("created_by"),
                project.get("problem_description"),
                project.get("solution"),
                project.get("technical_details"),
                project.get("comments"),
                project.get("likes"),
                project.get("technologies"),
                project.get("categories"),
                project.get("rating"),
            )
        )
        update_ids.append((project.get("original_project_id"),))

    try:
        async with db_conn(search_path=SEARCH_PATH) as conn:
            async with conn.transaction():
                async with conn.cursor() as cur:
                    await cur.executemany(insert_statement, insert_data)
                    await cur.executemany(update_statement, update_ids)
            logger.info(
                f"Successfully inserted {len(transformed_projects)} transformed projects and updated flags."
            )
    except Exception as e:
        logger.error(f"Failed to insert transformed projects: {e}")
        raise
