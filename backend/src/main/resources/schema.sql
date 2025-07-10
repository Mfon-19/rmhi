CREATE SCHEMA IF NOT EXISTS public;
CREATE TABLE IF NOT EXISTS users (
    uid           VARCHAR(28) PRIMARY KEY,
    email         VARCHAR(255) UNIQUE NOT NULL,
    username      VARCHAR(32) UNIQUE NOT NULL,
    provider      VARCHAR(32) NOT NULL DEFAULT 'password'
);

CREATE TABLE IF NOT EXISTS ideas (
                                     id           SERIAL PRIMARY KEY NOT NULL ,
                                     project_name VARCHAR(255)    NOT NULL,
                                     likes        INTEGER         NOT NULL DEFAULT 0,
                                     created_by   VARCHAR(255)    NOT NULL,
                                     short_description  VARCHAR(255),
                                     solution TEXT,
                                     problem_description TEXT,
                                     technical_details TEXT,
                                     rating INTEGER
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



CREATE TABLE IF NOT EXISTS technologies (
                                            id   SERIAL PRIMARY KEY,
                                            name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS technology_aliases (
                                                  alias         CITEXT PRIMARY KEY,
                                                  technology_id INT  NOT NULL REFERENCES technologies(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS idea_technologies (
                                                         idea_id       INT NOT NULL REFERENCES ideas(id) ON DELETE CASCADE,
                                                         technology_id INT NOT NULL REFERENCES technologies(id)  ON DELETE CASCADE,
                                                         PRIMARY KEY (idea_id, technology_id)
);

CREATE INDEX IF NOT EXISTS idx_sit_tech_id ON idea_technologies(technology_id);


CREATE OR REPLACE FUNCTION get_or_create_category(p_alias TEXT)
    RETURNS INT
AS $$
DECLARE
    v_cat_id INT;
BEGIN
    SELECT category_id INTO v_cat_id
    FROM   category_aliases
    WHERE  alias = lower(p_alias);
    IF FOUND THEN
        RETURN v_cat_id;
    END IF;

    SELECT id INTO v_cat_id
    FROM   categories
    WHERE  lower(name) = lower(p_alias);
    IF FOUND THEN
        INSERT INTO category_aliases(alias, category_id)
        VALUES (lower(p_alias), v_cat_id);
        RETURN v_cat_id;
    END IF;

    INSERT INTO categories(name)
    VALUES (initcap(p_alias))
    ON CONFLICT (name) DO NOTHING
    RETURNING id INTO v_cat_id;

    IF v_cat_id IS NULL THEN
        SELECT id INTO v_cat_id
        FROM categories
        WHERE lower(name) = lower(p_alias);
    END IF;

    INSERT INTO category_aliases(alias, category_id)
    VALUES (lower(p_alias), v_cat_id);
    RETURN v_cat_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE PROCEDURE add_tags_to_idea(
    p_idea_id BIGINT,
    p_tags    TEXT[]
)
AS $$
BEGIN
    INSERT INTO idea_categories (idea_id, category_id)
    SELECT p_idea_id, get_or_create_category(tag)
    FROM   unnest(p_tags) AS t(tag)
    ON CONFLICT DO NOTHING;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_or_create_technology(p_alias TEXT)
    RETURNS INT
AS $$
DECLARE
    v_tid INT;
BEGIN
    SELECT technology_id INTO v_tid
    FROM technology_aliases
    WHERE alias = lower(p_alias);
    IF FOUND THEN
        RETURN v_tid;
    END IF;

    SELECT id INTO v_tid
    FROM technologies
    WHERE lower(name) = lower(p_alias);
    IF FOUND THEN
        INSERT INTO technology_aliases(alias, technology_id)
        VALUES (lower(p_alias), v_tid);
        RETURN v_tid;
    END IF;

    INSERT INTO technologies(name)
    VALUES (initcap(p_alias))
    ON CONFLICT (name) DO NOTHING
    RETURNING id INTO v_tid;

    IF v_tid IS NULL THEN
        SELECT id INTO v_tid
        FROM technologies
        WHERE lower(name) = lower(p_alias);
    END IF;

    INSERT INTO technology_aliases(alias, technology_id)
    VALUES (lower(p_alias), v_tid);

    RETURN v_tid;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE PROCEDURE add_technologies_to_idea(
    p_idea_id BIGINT,
    p_tags    varchar[]
)
AS $$
BEGIN
    INSERT INTO idea_technologies(idea_id, technology_id)
    SELECT p_idea_id, get_or_create_technology(tag)
    FROM unnest(p_tags) AS t(tag)
    ON CONFLICT DO NOTHING;
END;
$$ LANGUAGE plpgsql;
