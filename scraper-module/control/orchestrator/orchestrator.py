from rmhi.ingest.backfill import main as backfill_main
from rmhi.ingest.scrape import main as daily_main


async def run_daily():
    await daily_main()
    # call the transformation service
async def run_backfill():
    await backfill_main()
    # call the transformation service
