ALTER TABLE project
    ADD COLUMN kind     TEXT NOT NULL DEFAULT 'PROJECT'
        CHECK (kind IN ('PROJECT','EXPERIENCE')),
    ADD COLUMN title    TEXT,
    ADD COLUMN company  TEXT,
    ADD COLUMN location TEXT,
    ADD COLUMN dates    TEXT;

CREATE INDEX project_kind_idx ON project(kind);

-- Profile no longer owns experience entries; they live as kind=EXPERIENCE projects.
ALTER TABLE profile DROP COLUMN experience;
