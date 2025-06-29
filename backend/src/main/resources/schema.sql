CREATE TABLE IF NOT EXISTS users (
    uid           VARCHAR(28) PRIMARY KEY,
    email         VARCHAR(255) UNIQUE NOT NULL,
    username      VARCHAR(32) UNIQUE NOT NULL,
    provider      VARCHAR(32) NOT NULL DEFAULT 'password'
); 