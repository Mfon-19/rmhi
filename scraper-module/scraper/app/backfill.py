# This is meant to be run once to prefferably get all the hackathons on Devpost

import requests
import json
import time
from datetime import datetime, timedelta

devpost_api_url = "https://devpost.com/api/hackathons"
start_page = 1
end_page = 1500
batch_limit = 500


def append_to_jsonl(filename, data_to_append):
    """
    Appends new hackathon data to the on disk JSON Lines file
    """
    try:
        with open(filename, "a", encoding="utf-8") as f:
            for item in data_to_append:
                f.write(json.dumps(item, ensure_ascii=False) + "\n")
    except (FileNotFoundError, json.JSONDecodeError):
        print("error")

def record_last_page(page):
    with open("last_page.txt", "w") as f:
        f.write(f"Last page: {page}")


def calculate_submission_deadline(submission_period_dates: str) -> str:
    sides = submission_period_dates.split("-")
    return sides[-1].strip()


def scrape_devpost() -> None:
    """
    Main function to scrape hackathon listings from Devpost.
    It iterates through pages, extracts hackathon links, gets details for each,
    and saves the data to a JSON file.
    """
    hackathon_data = []
    hackathon_count = 0
    count = 0
    last_page = 0
    for i in range(start_page, end_page):
        last_page = i
        print(f"[SCRAPE_DEVPOST] Scraping page {i}")
        try:
            request = requests.get(devpost_api_url + f"?page={i}")
            result = request.json()
            hackathons = result["hackathons"]

            for hackathon in hackathons:
                url = hackathon["url"]
                name = hackathon["title"]
                submission_deadline = calculate_submission_deadline(
                    hackathon["submission_period_dates"]
                )
                project_gallery_url = (
                    hackathon["submission_gallery_url"] or url + "/project-gallery"
                )

                hackathon_data.append(
                    {
                        "url": url,
                        "name": name,
                        "submission_deadline": submission_deadline,
                        "judging_ended": False,
                        "project_gallery_url": project_gallery_url,
                    }
                )

            print(f"[SCRAPE_DEVPOST] Found {len(hackathons)} hackathons on this page")
            count += len(hackathons)

            hackathon_count += len(hackathon_data)
            if hackathon_count >= batch_limit:
                append_to_jsonl("hackathons.jsonl", hackathon_data)
                hackathon_data.clear()
                hackathon_count = 0

            print(f"[SCRAPE_DEVPOST] {count} hackathons scraped")
            time.sleep(1)
        except (json.JSONDecodeError, requests.RequestException):
            append_to_jsonl("hackathons.jsonl", hackathon_data)
            hackathon_data.clear()
            break
    
    record_last_page(last_page)

if __name__ == "__main__":
    scrape_devpost()
