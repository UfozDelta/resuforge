CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE project (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         TEXT NOT NULL,
    description  TEXT NOT NULL,
    source_path  TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE bullet (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    text        TEXT NOT NULL,
    tags        TEXT[] NOT NULL DEFAULT '{}',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX bullet_project_id_idx ON bullet(project_id);

CREATE TABLE application (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company              TEXT,
    role                 TEXT,
    jd_text              TEXT NOT NULL,
    jd_url               TEXT,
    role_emphasis        TEXT NOT NULL,
    bullet_ranking       JSONB NOT NULL DEFAULT '[]'::jsonb,
    selected_bullet_ids  UUID[] NOT NULL DEFAULT '{}',
    cover_letter         TEXT,
    ats_matched          TEXT[] NOT NULL DEFAULT '{}',
    ats_missing          TEXT[] NOT NULL DEFAULT '{}',
    tex_blob             BYTEA,
    pdf_blob             BYTEA,
    tectonic_log         TEXT,
    outcome              TEXT NOT NULL DEFAULT 'applied',
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX application_outcome_idx ON application(outcome);
CREATE INDEX application_created_at_idx ON application(created_at DESC);
