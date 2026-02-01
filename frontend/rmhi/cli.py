import argparse
import asyncio
import logging
import os

from .jobs import run_backfill, run_daily, run_transform, run_transform_check


def configure_logging() -> None:
    level = os.getenv("LOG_LEVEL", "INFO").upper()
    logging.basicConfig(
        level=level,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    )


def main() -> None:
    configure_logging()
    default_mode = os.getenv("MODE", "daily").strip().lower()
    parser = argparse.ArgumentParser(description="RMHI scraper runner")
    parser.add_argument(
        "--mode",
        choices=["daily", "backfill", "transform", "transform_check"],
        default=default_mode,
        help="Select the job mode (defaults to $MODE or daily).",
    )
    args = parser.parse_args()

    if args.mode == "backfill":
        asyncio.run(run_backfill())
    elif args.mode == "transform":
        asyncio.run(run_transform())
    elif args.mode == "transform_check":
        asyncio.run(run_transform_check())
    else:
        asyncio.run(run_daily())


if __name__ == "__main__":
    main()
