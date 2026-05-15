CREATE TABLE profile (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    singleton          BOOLEAN NOT NULL DEFAULT TRUE UNIQUE,
    name               TEXT   NOT NULL DEFAULT '',
    phone              TEXT   NOT NULL DEFAULT '',
    email              TEXT   NOT NULL DEFAULT '',
    linkedin_handle    TEXT   NOT NULL DEFAULT '',
    github_handle      TEXT   NOT NULL DEFAULT '',
    portfolio_url      TEXT,
    education          JSONB  NOT NULL DEFAULT '[]'::jsonb,
    experience         JSONB  NOT NULL DEFAULT '[]'::jsonb,
    skills_languages   TEXT   NOT NULL DEFAULT '',
    skills_frameworks  TEXT   NOT NULL DEFAULT '',
    skills_databases   TEXT   NOT NULL DEFAULT '',
    skills_devops      TEXT   NOT NULL DEFAULT '',
    skills_interests   TEXT   NOT NULL DEFAULT '',
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO profile (singleton) VALUES (TRUE) ON CONFLICT DO NOTHING;
