# RMHI Scraper Module

This module contains the ingest (scraper) and transform services used to
discover hackathon projects and generate derivative ideas.

Structure:
- `src/rmhi/common`: shared DB/config helpers.
- `src/rmhi/ingest`: daily/backfill scraping logic and CLI.
- `src/rmhi/transform`: Gemini-based transformation logic and CLI.

Local usage:
```
export PYTHONPATH=./src
python -m rmhi.ingest.cli --mode daily
python -m rmhi.ingest.cli --mode backfill
python -m rmhi.transform.cli
```

Requirements:
- Ingest: `requirements/ingest.txt`
- Transform: `requirements/transform.txt`

Key env vars:
- `DB_DSN`
- `GOOGLE_API_KEY`
- `AWS_REGION`, `AWS_ENDPOINT_URL`, `SQS_SCRAPE_URL`, `SQS_TRANSFORM_URL`
