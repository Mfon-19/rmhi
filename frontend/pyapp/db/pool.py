from __future__ import annotations

from contextlib import contextmanager
from typing import Iterator, Optional

from psycopg_pool import ConnectionPool

_pool: Optional[ConnectionPool] = None


def init_pool(dsn: str) -> None:
    global _pool
    if _pool is None:
        _pool = ConnectionPool(conninfo=dsn, min_size=1, max_size=5)


def close_pool() -> None:
    global _pool
    if _pool is not None:
        _pool.close()
        _pool = None


def get_pool() -> ConnectionPool:
    if _pool is None:
        raise RuntimeError("DB pool not initialized")
    return _pool


@contextmanager
def get_conn() -> Iterator:
    pool = get_pool()
    with pool.connection() as conn:
        yield conn
