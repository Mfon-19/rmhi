CREATE TABLE IF NOT EXISTS users (
    uid           VARCHAR(28) PRIMARY KEY,
    email         VARCHAR(255) UNIQUE NOT NULL,
    username      VARCHAR(32) UNIQUE NOT NULL,
    provider      VARCHAR(32) NOT NULL DEFAULT 'password'
);
--CREATE TYPE public.category AS ENUM ('MobileApp', 'AI', 'FinTech', 'Web3', 'Health', 'Education', 'Gaming', 'SaaS', 'E-commerce', 'Social', 'Productivity', 'Entertainment');

CREATE TABLE IF NOT EXISTS ideas (
    id SERIAL PRIMARY KEY ,
    title VARCHAR(255),
    description TEXT,
    likes INT,
    user_id VARCHAR(28) REFERENCES users(uid),
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS comments (
    id SERIAL PRIMARY KEY ,
    content TEXT,
    idea_id SERIAL REFERENCES ideas(id),
    user_id VARCHAR(28) REFERENCES users (uid)
);


CREATE TABLE IF NOT EXISTS categories (
                            id   SERIAL PRIMARY KEY,
                            name TEXT NOT NULL UNIQUE
);

CREATE EXTENSION IF NOT EXISTS citext;
CREATE TABLE IF NOT EXISTS category_aliases (
                                  alias       CITEXT PRIMARY KEY,
                                  category_id INT NOT NULL REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS idea_categories (
                                 idea_id     BIGINT NOT NULL REFERENCES ideas(id)      ON DELETE CASCADE,
                                 category_id INT    NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
                                 PRIMARY KEY (idea_id, category_id)
);

CREATE INDEX ON idea_categories(category_id);

CREATE TABLE scraped_ideas (
                               id           SERIAL PRIMARY KEY NOT NULL ,
                               project_name VARCHAR(255)    NOT NULL,
                               likes        INTEGER         NOT NULL DEFAULT 0,
                               submitted_to VARCHAR(255),
                               winner       BOOLEAN         NOT NULL DEFAULT FALSE,
                               created_by   VARCHAR(255)    NOT NULL,
                               description  TEXT
);

CREATE TABLE IF NOT EXISTS technologies (
                                            id   SERIAL PRIMARY KEY,
                                            name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS technology_aliases (
                                                  alias         CITEXT PRIMARY KEY,
                                                  technology_id INT  NOT NULL REFERENCES technologies(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS scraped_idea_technologies (
                                                         idea_id       INT NOT NULL REFERENCES scraped_ideas(id) ON DELETE CASCADE,
                                                         technology_id INT NOT NULL REFERENCES technologies(id)  ON DELETE CASCADE,
                                                         PRIMARY KEY (idea_id, technology_id)
);

CREATE INDEX IF NOT EXISTS idx_sit_tech_id ON scraped_idea_technologies(technology_id);
