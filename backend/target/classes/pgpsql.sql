CREATE OR REPLACE FUNCTION get_or_create_category(p_alias TEXT)
    RETURNS INT
    LANGUAGE plpgsql AS
$$
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
    ON CONFLICT (lower(name)) DO NOTHING
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
$$;

CREATE OR REPLACE PROCEDURE add_tags_to_idea(
    p_idea_id BIGINT,
    p_tags    TEXT[]
)
    LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO idea_categories (idea_id, category_id)
    SELECT p_idea_id, get_or_create_category(tag)
    FROM   unnest(p_tags) AS t(tag)
    ON CONFLICT DO NOTHING;
END;
$$;


CREATE OR REPLACE FUNCTION get_or_create_technology(p_alias TEXT)
    RETURNS INT
    LANGUAGE plpgsql AS
$$
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
$$;

CREATE OR REPLACE PROCEDURE add_technologies_to_idea(
    p_idea_id INT,
    p_tags    TEXT[]
)
    LANGUAGE plpgsql AS
$$
BEGIN
    INSERT INTO scraped_idea_technologies(idea_id, technology_id)
    SELECT p_idea_id, get_or_create_technology(tag)
    FROM unnest(p_tags) AS t(tag)
    ON CONFLICT DO NOTHING;
END;
$$;