ALTER TABLE admission_candidate_snapshot
    ALTER COLUMN source_submission_id DROP NOT NULL,
    ALTER COLUMN effective_submission_version DROP NOT NULL;

ALTER TABLE admission_candidate_snapshot
    DROP CONSTRAINT ck_candidate_snapshot_version;

ALTER TABLE admission_candidate_snapshot
    ADD CONSTRAINT ck_candidate_snapshot_version CHECK (
        (source_submission_id IS NULL AND effective_submission_version IS NULL)
        OR (source_submission_id IS NOT NULL AND effective_submission_version > 0)
    );
