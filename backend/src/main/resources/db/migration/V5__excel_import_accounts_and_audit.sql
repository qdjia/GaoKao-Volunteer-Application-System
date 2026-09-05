ALTER TABLE sys_user ADD COLUMN candidate_id BIGINT UNIQUE REFERENCES candidate(id);
ALTER TABLE sys_user ADD CONSTRAINT ck_sys_user_candidate_role
    CHECK (candidate_id IS NULL OR (role = 'STUDENT' AND student_id IS NULL));

ALTER TABLE institution_group_major DROP CONSTRAINT uq_group_major_order;
ALTER TABLE institution_group_major ADD CONSTRAINT uq_group_major_order
    UNIQUE (institution_group_id, display_order) DEFERRABLE INITIALLY DEFERRED;

CREATE TABLE excel_import_job (
    id UUID PRIMARY KEY,
    operator_user_id BIGINT NOT NULL REFERENCES sys_user(id),
    admission_batch_id BIGINT NOT NULL REFERENCES admission_batch(id),
    template_type VARCHAR(20) NOT NULL,
    template_version VARCHAR(10) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('SUCCEEDED', 'REJECTED')),
    row_count INTEGER NOT NULL,
    created_count INTEGER NOT NULL DEFAULT 0,
    updated_count INTEGER NOT NULL DEFAULT 0,
    errors_json JSONB NOT NULL DEFAULT '[]',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX ix_excel_import_job_created ON excel_import_job(created_at DESC);
