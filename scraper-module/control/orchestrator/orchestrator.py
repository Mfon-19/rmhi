import scraper.app.scrape as daily_scraper
import scraper.app.backfill as backfill_scraper


async def run_daily():
    await daily_scraper.main()
    # call the transformation service
async def run_backfill():
    await backfill_scraper.main()
    # call the transformation service

