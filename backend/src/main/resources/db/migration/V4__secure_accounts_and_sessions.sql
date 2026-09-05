ALTER TABLE sys_user
    ALTER COLUMN password TYPE VARCHAR(255),
    ADD COLUMN account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN locked_until TIMESTAMP,
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN password_changed_at TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE sys_user
    ADD CONSTRAINT ck_sys_user_role CHECK (role IN ('ADMIN', 'STUDENT')),
    ADD CONSTRAINT ck_sys_user_status CHECK (account_status IN ('ACTIVE', 'DISABLED')),
    ADD CONSTRAINT ck_sys_user_failed_attempts CHECK (failed_login_attempts >= 0);

UPDATE sys_user
SET must_change_password = TRUE
WHERE role = 'STUDENT';

CREATE TABLE auth_session (
    session_id      UUID PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    token_hash      CHAR(64) NOT NULL UNIQUE,
    audience        VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP NOT NULL,
    revoked_at      TIMESTAMP,
    revoke_reason   VARCHAR(50),
    client_ip_hash  CHAR(64) NOT NULL,
    user_agent_hash CHAR(64) NOT NULL,
    CONSTRAINT fk_auth_session_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT ck_auth_session_audience CHECK (audience IN ('LOCAL_ADMIN', 'PUBLIC_CANDIDATE')),
    CONSTRAINT ck_auth_session_expiry CHECK (expires_at > created_at),
    CONSTRAINT ck_auth_session_revocation CHECK (
        (revoked_at IS NULL AND revoke_reason IS NULL)
        OR (revoked_at IS NOT NULL AND revoke_reason IS NOT NULL)
    )
);

CREATE INDEX idx_auth_session_user_active
    ON auth_session(user_id, expires_at)
    WHERE revoked_at IS NULL;
