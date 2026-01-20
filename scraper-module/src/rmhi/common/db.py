# create a connection pool for the scraper and transformer to pull from
import os
from contextlib import asynccontextmanager
from typing import Optional
from psycopg_pool import ConnectionPool, AsyncConnectionPool
from .settings import get_settings

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
                "application_name": os.getenv("APP_NAME", "unknown"),
                "options": "-c statement_timeout=15000 -c lock_timeout=2000 -c idle_in_transaction_session_timeout=10000",
                "keepalives": 1,
                "keepalives_idle": 30,
                "keepalives_interval": 10,
                "keepalives_count": 5,
            },
        )
    return POOL


@asynccontextmanager
async def db_conn(readonly: bool = False, search_path: Optional[str] = None):
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
