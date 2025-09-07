import logging
from typing import List, Dict, Any, Tuple

from libs.common.db import db_conn

logger = logging.getLogger(__name__)
SEARCH_PATH = "ingest"


async def get_untransformed_projects(batch_size: int = 100) -> List[Dict[str, Any]]:
    """
    Fetches a batch of projects from the ingest.project table that have not yet been transformed.
    """
    projects = []
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


async def insert_transformed_projects_and_update_flags(
    transformed_projects: List[Dict[str, Any]]
):
    """
    Inserts transformed projects into the new table and updates the 'transformed' flag
    on the original projects in a single transaction.
    """
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
