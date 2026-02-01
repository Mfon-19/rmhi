from __future__ import annotations

from typing import Dict, Iterable, List

from psycopg.rows import dict_row

from pyapp.models.schemas import IdeaCreate


def _normalize_name_list(values: Iterable) -> List[str]:
    names: List[str] = []
    for item in values:
        if item is None:
            continue
        if isinstance(item, str):
            name = item.strip()
        elif isinstance(item, dict):
            name = str(item.get("name", "")).strip()
        else:
            name = str(getattr(item, "name", "")).strip()
        if name:
            names.append(name)
    return names


def username_exists(conn, username: str) -> bool:
    with conn.cursor() as cur:
        cur.execute("SELECT 1 FROM users WHERE username = %s LIMIT 1", (username,))
        return cur.fetchone() is not None


def register_user(conn, uid: str, email: str, username: str, provider: str) -> None:
    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO users (uid, email, username, provider)
            VALUES (%s, %s, %s, %s)
            """,
            (uid, email, username, provider),
        )
    conn.commit()


def create_idea(conn, idea: IdeaCreate) -> int:
    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO ideas (
                project_name,
                likes,
                created_by,
                short_description,
                solution,
                problem_description,
                technical_details,
                rating
            )
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
            RETURNING id
            """,
            (
                idea.projectName,
                idea.likes,
                idea.createdBy,
                idea.shortDescription,
                idea.solution,
                idea.problemDescription,
                idea.technicalDetails,
                idea.rating,
            ),
        )
        idea_id = cur.fetchone()[0]

        categories = _normalize_name_list(idea.categories)
        if categories:
            cur.execute("CALL add_tags_to_idea(%s, %s)", (idea_id, categories))

        technologies = _normalize_name_list(idea.technologies)
        if technologies:
            cur.execute("CALL add_technologies_to_idea(%s, %s)", (idea_id, technologies))

    conn.commit()
    return int(idea_id)


def get_ideas(conn) -> List[Dict]:
    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute(
            """
            SELECT
                i.id,
                i.project_name,
                i.likes,
                i.created_by,
                i.short_description,
                i.solution,
                i.problem_description,
                i.technical_details,
                i.rating,
                COALESCE(
                    array_agg(DISTINCT c.name) FILTER (WHERE c.name IS NOT NULL),
                    '{}'::text[]
                ) AS categories,
                COALESCE(
                    array_agg(DISTINCT t.name) FILTER (WHERE t.name IS NOT NULL),
                    '{}'::text[]
                ) AS technologies
            FROM ideas i
            LEFT JOIN idea_categories ic ON ic.idea_id = i.id
            LEFT JOIN categories c ON c.id = ic.category_id
            LEFT JOIN idea_technologies it ON it.idea_id = i.id
            LEFT JOIN technologies t ON t.id = it.technology_id
            GROUP BY i.id
            ORDER BY i.id DESC
            """
        )
        ideas = cur.fetchall()

    idea_ids = [row["id"] for row in ideas]
    comments_by_idea: Dict[int, List[Dict]] = {idea_id: [] for idea_id in idea_ids}
    if idea_ids:
        with conn.cursor(row_factory=dict_row) as cur:
            cur.execute(
                """
                SELECT id, content, idea_id, user_id
                FROM comments
                WHERE idea_id = ANY(%s)
                """,
                (idea_ids,),
            )
            for row in cur.fetchall():
                comments_by_idea.setdefault(row["idea_id"], []).append(row)

    results: List[Dict] = []
    for row in ideas:
        results.append(
            {
                "id": row["id"],
                "projectName": row["project_name"],
                "likes": row["likes"],
                "createdBy": row["created_by"],
                "shortDescription": row["short_description"],
                "solution": row["solution"],
                "problemDescription": row["problem_description"],
                "technicalDetails": row["technical_details"],
                "rating": row["rating"],
                "categories": row["categories"] or [],
                "technologies": row["technologies"] or [],
                "comments": comments_by_idea.get(row["id"], []),
            }
        )
    return results


def get_transformed_ideas(conn, offset: int, limit: int) -> List[Dict]:
    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute(
            """
            SELECT id,
                   project_name,
                   short_description,
                   created_by,
                   problem_description,
                   solution,
                   technical_details,
                   likes,
                   technologies,
                   categories,
                   rating
            FROM ingest.transformed_project
            ORDER BY created_at DESC
            LIMIT %s OFFSET %s
            """,
            (limit, offset),
        )
        rows = cur.fetchall()

    results: List[Dict] = []
    for row in rows:
        results.append(
            {
                "id": row["id"],
                "projectName": row["project_name"],
                "shortDescription": row["short_description"],
                "createdBy": row["created_by"] or "anonymous",
                "problemDescription": row["problem_description"],
                "solution": row["solution"],
                "technicalDetails": row["technical_details"],
                "likes": row["likes"] or 0,
                "technologies": row["technologies"] or [],
                "categories": row["categories"] or [],
                "rating": float(row["rating"]) if row["rating"] is not None else 0.0,
            }
        )
    return results
