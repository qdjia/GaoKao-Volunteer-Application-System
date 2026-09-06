CREATE TABLE demo_account_secret (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    secret BYTEA NOT NULL CHECK (octet_length(secret) = 32)
);
CREATE TRIGGER trg_demo_secret_immutable BEFORE UPDATE OR DELETE ON demo_account_secret
    FOR EACH ROW EXECUTE FUNCTION reject_immutable_change();

ALTER TABLE sys_user ADD COLUMN demo_slot INTEGER UNIQUE;
ALTER TABLE sys_user ADD CONSTRAINT ck_permanent_demo_account CHECK (
    demo_slot IS NULL OR (
        demo_slot BETWEEN 1 AND 10 AND role = 'STUDENT' AND candidate_id IS NOT NULL
        AND username = '9' || lpad(demo_slot::text, 9, '0')
        AND NOT must_change_password AND account_status = 'ACTIVE'
    )
);

CREATE FUNCTION protect_permanent_demo_account() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP <> 'INSERT' AND OLD.demo_slot IS NOT NULL THEN
        IF TG_OP = 'DELETE' THEN
            RAISE EXCEPTION 'permanent demo accounts cannot be deleted' USING ERRCODE = '55000';
        END IF;
        IF ROW(OLD.demo_slot, OLD.username, OLD.password, OLD.role, OLD.candidate_id)
            IS DISTINCT FROM ROW(NEW.demo_slot, NEW.username, NEW.password, NEW.role, NEW.candidate_id) THEN
            RAISE EXCEPTION 'permanent demo credentials are immutable' USING ERRCODE = '55000';
        END IF;
    END IF;
    IF TG_OP <> 'DELETE' AND NEW.demo_slot IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM candidate WHERE id = NEW.candidate_id AND data_origin = 'DEMO'
    ) THEN
        RAISE EXCEPTION 'permanent demo accounts require explicit demo origin' USING ERRCODE = '55000';
    END IF;
    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_permanent_demo_account BEFORE INSERT OR UPDATE OR DELETE ON sys_user
    FOR EACH ROW EXECUTE FUNCTION protect_permanent_demo_account();
