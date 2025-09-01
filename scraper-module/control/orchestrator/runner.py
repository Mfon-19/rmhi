import os
import asyncio

from control.orchestrator.orchestrator import run_daily, run_backfill


def main() -> None:
    mode = os.getenv("MODE", "daily").strip().lower()
    if mode == "backfill":
        asyncio.run(run_backfill())
    else:
        asyncio.run(run_daily())


if __name__ == "__main__":
    main()
