# create a connection pool for the scraper and transformer to pull from
import os
from contextlib import contextmanager
from typing import Optional
from psycopg_pool import ConnectionPool
from .settings import get_settings

cfg = get_settings()
DSN = os.getenv("DB_DSN", cfg.DB_DSN)

POOL = ConnectionPool(
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


@contextmanager
def db_conn(readonly: bool = False, search_path: Optional[str] = None):
    with POOL.connection() as conn:
        if search_path:
            conn.execute(f"SET search_path TO {search_path}")
        if readonly:
            conn.execute("SET TRANSACTION READONLY")
        try:
            yield conn
            conn.commit()
        except Exception:
            conn.rollback()
            raise
