CREATE TABLE IF NOT EXISTS users (
    uid           VARCHAR(28) PRIMARY KEY,
    email         VARCHAR(255) UNIQUE NOT NULL,
    username      VARCHAR(32) UNIQUE NOT NULL,
    provider      VARCHAR(32) NOT NULL DEFAULT 'password'
);

-- DO $$
--     BEGIN
--         IF NOT EXISTS (
--             SELECT 1
--             FROM pg_type t
--                      JOIN pg_namespace n ON n.oid = t.typnamespace
--             WHERE t.typname = 'category'
--               AND n.nspname = 'public'   -- adjust if you’re using another schema
--         ) THEN
--             CREATE TYPE public.category AS ENUM ('MobileApp', 'AI', 'FinTech', 'Web3', 'Health', 'Education', 'Gaming', 'SaaS', 'E-commerce', 'Social', 'Productivity', 'Entertainment');
--         END IF;
-- END; $$;
--CREATE TYPE public.category AS ENUM ('MobileApp', 'AI', 'FinTech', 'Web3', 'Health', 'Education', 'Gaming', 'SaaS', 'E-commerce', 'Social', 'Productivity', 'Entertainment');

CREATE TABLE IF NOT EXISTS ideas (
    id SERIAL PRIMARY KEY ,
    title VARCHAR(255),
    description TEXT,
    likes INT,
    categories category[],
    user_id VARCHAR(28) REFERENCES users(uid)
);

CREATE TABLE IF NOT EXISTS comments (
    id SERIAL PRIMARY KEY ,
    content TEXT,
    idea_id SERIAL REFERENCES ideas(id),
    user_id VARCHAR(28) REFERENCES users (uid)
)