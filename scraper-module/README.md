# RMHI Scraper Module

This module runs the daily scrape, backfill, and transform jobs.

Structure:
- `src/rmhi/cli.py`: single entrypoint for all modes.
- `src/rmhi/jobs.py`: job runners.
- `src/rmhi/scrape.py`: scraping logic.
- `src/rmhi/transform.py`: Gemini-based transformations.
- `src/rmhi/db.py`: database helpers.
- `src/rmhi/settings.py`: environment config.

Local usage:
```
export PYTHONPATH=./src
python -m rmhi.cli --mode daily
python -m rmhi.cli --mode backfill
python -m rmhi.cli --mode transform
python -m rmhi.cli --mode transform_check
```

Requirements:
- `requirements.txt`

Key env vars:
- `DB_DSN`
- `GOOGLE_API_KEY`
- `MODE` (optional override for the CLI)
