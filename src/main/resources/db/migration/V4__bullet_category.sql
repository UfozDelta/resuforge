ALTER TABLE bullet
    ADD COLUMN category TEXT NOT NULL DEFAULT 'general';

CREATE INDEX bullet_project_category_idx ON bullet (project_id, category);
