CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    -- For synthetic IDs (i.e. ones not appearing in REST API), we use UUID
    id       uuid    NOT NULL DEFAULT uuid_generate_v4(),
    email    VARCHAR NOT NULL UNIQUE,
    password TEXT    NOT NULL,
    username VARCHAR NOT NULL UNIQUE,
    bio      TEXT,
--     TODO: image
    PRIMARY KEY(id)
);

CREATE TABLE followers (
    follower uuid REFERENCES users(id),
    followed uuid REFERENCES users(id),
    PRIMARY KEY(follower, followed)
);

CREATE TABLE articles (
    id          uuid        NOT NULL DEFAULT uuid_generate_v4(),
    author_id   uuid        NOT NULL REFERENCES users(id),
    slug        TEXT        NOT NULL UNIQUE,
    title       TEXT        NOT NULL,
    description TEXT        NOT NULL,
    body        TEXT        NOT NULL,
    created_at  TIMESTAMP   NOT NULL,
    updated_at  TIMESTAMP   NOT NULL,
    PRIMARY KEY(id)
);

CREATE TABLE tags (
    id          uuid        NOT NULL DEFAULT uuid_generate_v4(),
    tag         VARCHAR     NOT NULL,
    PRIMARY KEY(id)
);

CREATE TABLE articles_tags (
    article_id uuid NOT NULL REFERENCES articles(id),
    tag_id     uuid NOT NULL REFERENCES tags(id),
    PRIMARY KEY(article_id, tag_id)
);

CREATE TABLE comments (
    -- comments.id is the only non-synthetic ID, i.e. it appears in the REST API and API dictates it to be a numeric id,
    -- hence SERIAL
    id          SERIAL      NOT NULL,
    author_id   uuid        NOT NULL REFERENCES users(id),
    body        TEXT        NOT NULL,
    created_at  TIMESTAMP   NOT NULL,
    updated_at  TIMESTAMP   NOT NULL,
    PRIMARY KEY(id)
);
