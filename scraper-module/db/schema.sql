-- SCHEMA
CREATE SCHEMA IF NOT EXISTS ingest;

-- ENUMS
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'hackathon_state') THEN
    CREATE TYPE ingest.hackathon_state AS ENUM
      ('discovered','ended','scraping','retrying','completed','failed');
  END IF;
END;
    $$ LANGUAGE plpgsql;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'hackathon_scrape_stop_reason') THEN
        CREATE TYPE ingest.hackathon_scrape_stop_reason AS ENUM
          ('seen_hit','end_of_pages','limit_500','error');
    END IF;
END;
    $$ LANGUAGE plpgsql;

-- TABLES
CREATE TABLE IF NOT EXISTS ingest.hackathon (
  id                          BIGSERIAL PRIMARY KEY,
  url                         TEXT UNIQUE NOT NULL,
  project_gallery_url         TEXT UNIQUE NOT NULL,
  name                        TEXT,
  state                       ingest.hackathon_state NOT NULL DEFAULT 'discovered',
  attempt_count               INT NOT NULL DEFAULT 0,
  next_action_at              TIMESTAMPTZ,
  last_error                  TEXT,
  created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Listing crawl runs (audit)
CREATE TABLE IF NOT EXISTS ingest.hackathon_scrape_run (
  id            BIGSERIAL PRIMARY KEY,
  started_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  finished_at   TIMESTAMPTZ,
  mode          TEXT NOT NULL CHECK (mode IN ('backfill','daily')),
  pages_scanned INT,
  new_count     INT,
  stop_reason   ingest.hackathon_scrape_stop_reason NOT NULL DEFAULT 'seen_hit',
  status        TEXT NOT NULL CHECK (status IN ('success','error')),
  error         TEXT
);

-- Per-hackathon project scrape attempts
CREATE TABLE IF NOT EXISTS ingest.project_scrape_run (
  id            BIGSERIAL PRIMARY KEY,
  hackathon_id  BIGINT NOT NULL REFERENCES ingest.hackathon(id) ON DELETE CASCADE,
  started_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  finished_at   TIMESTAMPTZ,
  status        TEXT NOT NULL CHECK (status IN ('success','error')),
  project_count INT,
  error         TEXT
);

-- Project URLs discovered per hackathon (supports retries/idempotency)
CREATE TABLE IF NOT EXISTS ingest.hackathon_project_url (
  hackathon_id  BIGINT NOT NULL REFERENCES ingest.hackathon(id) ON DELETE CASCADE,
  project_url   TEXT   NOT NULL,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (hackathon_id, project_url)
);

-- Projects scraped
CREATE TABLE IF NOT EXISTS ingest.project (
    id                  BIGSERIAL PRIMARY KEY,
    project_url         TEXT,
    project_name        VARCHAR(255)    NOT NULL,
    short_description   VARCHAR(255),
    solution            TEXT,
    problem_description TEXT,
    technical_details   TEXT,
    transformed         BOOLEAN
);

-- INDEXES tuned for dispatcher and lookups

CREATE INDEX IF NOT EXISTS idx_ing_psr_hackathon
  ON ingest.project_scrape_run(hackathon_id);

CREATE INDEX IF NOT EXISTS idx_ing_hpu_hackathon
  ON ingest.hackathon_project_url(hackathon_id);


-- SCHEMA
CREATE SCHEMA IF NOT EXISTS transform;

-- TABLES
CREATE TABLE IF NOT EXISTS transform.transformation_run (
  id             BIGSERIAL PRIMARY KEY,
  started_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  finished_at    TIMESTAMPTZ,
  status         TEXT NOT NULL CHECK (status IN ('success','error')),
  error          TEXT,
  transformed_count INT DEFAULT 0
);
