import argparse
import asyncio
import os

from .backfill import main as backfill_main
from .scrape import main as daily_main


def main() -> None:
    default_mode = os.getenv("MODE", "daily").strip().lower()
    parser = argparse.ArgumentParser(description="RMHI ingest runner")
    parser.add_argument(
        "--mode",
        choices=["daily", "backfill"],
        default=default_mode,
        help="Select the ingest mode (defaults to $MODE or daily).",
    )
    args = parser.parse_args()

    if args.mode == "backfill":
        asyncio.run(backfill_main())
    else:
        asyncio.run(daily_main())


if __name__ == "__main__":
    main()
