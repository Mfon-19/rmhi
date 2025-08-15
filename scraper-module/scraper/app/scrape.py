import requests

# This is for the daily scraper
class DailyScraper:
    def __init__(self) -> None:
        self.devpost_api_url = "https://devpost.com/api/hackathons"
        self.start_page = 1
        self.end_page = 1500

        pass
    
    def scrape() -> None:
      """
      This scans Devpost and scrapes hackathons not yet discovered
      """

      # TODO: First load the most recent 100 hackathons in our database. Then start scraping. If
      # we encounter 5 hackathons we have scraped already, stop scraping. Migrate the ones
      # we scraped to the database
      hackathon_data = []
      hackathon_count = 0
      count = 0
      last_page = 0
      for i in range(self.start_page, self.end_page):
          # last_page = i
          # print(f"[SCRAPE_DEVPOST] Scraping page {i}")
          # try:
          #     request = requests.get(devpost_api_url + f"?page={i}")
          #     result = request.json()
          #     hackathons = result["hackathons"]

          #     for hackathon in hackathons:
          #         url = hackathon["url"]
          #         name = hackathon["title"]
          #         submission_deadline = calculate_submission_deadline(
          #             hackathon["submission_period_dates"]
          #         )
          #         project_gallery_url = (
          #             hackathon["submission_gallery_url"] or url + "/project-gallery"
          #         )

          #         hackathon_data.append(
          #             {
          #                 "url": url,
          #                 "name": name,
          #                 "submission_deadline": submission_deadline,
          #                 "judging_ended": False,
          #                 "project_gallery_url": project_gallery_url,
          #             }
          #         )

          #     print(f"[SCRAPE_DEVPOST] Found {len(hackathons)} hackathons on this page")
          #     count += len(hackathons)

          #     hackathon_count += len(hackathon_data)
          #     if hackathon_count >= batch_limit:
          #         append_to_jsonl("hackathons.jsonl", hackathon_data)
          #         hackathon_data.clear()
          #         hackathon_count = 0

          #     print(f"[SCRAPE_DEVPOST] {count} hackathons scraped")
          #     time.sleep(1)
          # except (json.JSONDecodeError, requests.RequestException):
          #     append_to_jsonl("hackathons.jsonl", hackathon_data)
          #     hackathon_data.clear()
          #     break
