BEGIN;
SET LOCAL search_path TO public, pg_catalog;
CREATE UNIQUE INDEX IF NOT EXISTS uq_ssh_practice_reviewed_assessment
    ON ssh_practice_runs (assessment_id) WHERE state = 'REVIEWED';
COMMIT;
