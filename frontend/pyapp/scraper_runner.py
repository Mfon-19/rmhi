from __future__ import annotations

import asyncio
import os
import time
from typing import Dict

# Set defaults before importing scraper modules.
os.environ.setdefault("SCRAPE_BATCH_LIMIT", os.getenv("SCRAPER_BATCH_LIMIT", "200"))
os.environ.setdefault("MAX_DISCOVERED_HACKATHONS", os.getenv("SCRAPER_MAX_DISCOVERED", "50"))

from rmhi import jobs  # noqa: E402
from rmhi.db import advisory_lock  # noqa: E402


def _env_int(name: str, default: int) -> int:
    raw = os.getenv(name, "").strip()
    if not raw:
        return default
    try:
        return int(raw)
    except ValueError:
        return default


async def run_once() -> Dict:
    max_seconds = _env_int("SCRAPER_MAX_SECONDS", 240)
    mode = os.getenv("SCRAPER_MODE", "daily").strip().lower()

    lock_key = 994_200_001
    async with advisory_lock(lock_key) as acquired:
        if not acquired:
            return {"status": "skipped", "reason": "lock"}

        start = time.time()
        try:
            if mode == "transform":
                await asyncio.wait_for(jobs.run_transform(), timeout=max_seconds)
            elif mode == "transform_check":
                await asyncio.wait_for(jobs.run_transform_check(), timeout=max_seconds)
            elif mode == "backfill":
                await asyncio.wait_for(jobs.run_backfill(), timeout=max_seconds)
            else:
                await asyncio.wait_for(jobs.run_daily(), timeout=max_seconds)
        except asyncio.TimeoutError:
            elapsed = int(time.time() - start)
            return {"status": "timeout", "elapsed": elapsed}

    elapsed = int(time.time() - start)
    return {"status": "ok", "elapsed": elapsed}
