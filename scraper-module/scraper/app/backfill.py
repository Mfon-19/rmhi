# This is meant to be run once to preferably get all the hackathons on Devpost

from datetime import datetime
import requests
import json
import time
import asyncio
import logging
from asyncio import Task
from json import JSONDecodeError
from scraper.app.db_ops import insert_into_hackathon, insert_into_hackathon_scrape_run

should_stop_scraping = False
logger = logging.getLogger("__name__")

devpost_api_url = "https://devpost.com/api/hackathons"
start_page = 1
end_page = 1500
batch_limit = 500


def append_to_jsonl(filename, data_to_append):
    try:
        with open(filename, "a", encoding="utf-8") as f:
            for item in data_to_append:
                f.write(json.dumps(item, ensure_ascii=False) + "\n")
    except (FileNotFoundError, JSONDecodeError) as e:
        logger.error(f"[APPEND_TO_JSONL] Failed to append to JSONL: {e}")


def handle_task_exception(task: Task, hackathon_data = None):
    global should_stop_scraping
    try:
        task.result()
    except Exception as e:
        logger.error(f"[DB_ERROR] Failed to insert hackathons: {e}")

        if hackathon_data:
            append_to_jsonl("failed_inserts.jsonl", hackathon_data)

        if any(
            keyword in str(e).lower()
            for keyword in ["connection", "pool", "timeout", "database"]
        ):
            logger.critical("[CRITICAL] Database connection issue.")
            should_stop_scraping = True


async def scrape_devpost() -> None:
    global should_stop_scraping
    hackathon_data = []
    hackathon_count = 0
    count = 0
    scrape_started = datetime.now()

    for i in range(start_page, end_page):
        if should_stop_scraping:
            logger.info("[SCRAPE_DEVPOST] Stopping due to critical database errors")
            break

        logger.info(f"[SCRAPE_DEVPOST] Scraping page {i}")
        try:
            request = requests.get(devpost_api_url + f"?page={i}")
            result = request.json()
            hackathons = result["hackathons"]

            if len(hackathons) == 0:
                break

            for hackathon in hackathons:
                url = hackathon["url"]
                name = hackathon["title"]
                project_gallery_url = (
                    hackathon["submission_gallery_url"] or url + "/project-gallery"
                )

                hackathon_data.append(
                    {
                        "url": url,
                        "name": name,
                        "project_gallery_url": project_gallery_url,
                    }
                )

            logger.info(
                f"[SCRAPE_DEVPOST] Found {len(hackathons)} hackathons on this page"
            )
            count += len(hackathons)

            hackathon_count += len(hackathon_data)
            if hackathon_count >= batch_limit:
                asyncio.create_task(insert_into_hackathon(hackathon_data.copy()))
                hackathon_data.clear()
                hackathon_count = 0

            logger.info(f"[SCRAPE_DEVPOST] {count} hackathons scraped \n")
            time.sleep(1)
        except (json.JSONDecodeError, requests.RequestException):
            hackathon_data_copy = hackathon_data.copy()
            task = asyncio.create_task(insert_into_hackathon(hackathon_data_copy))
            task.add_done_callback(
                handle_task_exception(hackathon_data=hackathon_data_copy)
            )
            hackathon_data.clear()
            hackathon_count = 0

    if hackathon_data:
        asyncio.create_task(insert_into_hackathon(hackathon_data.copy()))
        hackathon_data.clear()

    scrape_end = datetime.now()
    await insert_into_hackathon_scrape_run(scrape_started, scrape_end, "backfill", count, 0, "end_of_pages", "success", "")

    # TODO: trigger the script to convert state from discovered -> ended
    # TODO: if there are projects in the project gallery


def main():
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
        handlers=[
            logging.FileHandler("backfill.log"),
            logging.StreamHandler(),
        ],
    )
    logger.info("Started backfill")
    asyncio.run(scrape_devpost())
    logger.info("Finished backfill")


if __name__ == "__main__":
    main()
