-- V7__member_duplicate_status_and_audit_decoupling.sql

ALTER TABLE members DROP CONSTRAINT members_membership_status_check;
ALTER TABLE members ADD CONSTRAINT members_membership_status_check
    CHECK (membership_status IN ('ATIVO', 'INATIVO', 'AFASTADO', 'SOB_DISCIPLINA', 'FALECIDO', 'DUPLICATE'));

ALTER TABLE members ADD COLUMN duplicate_of_id UUID REFERENCES members(id) ON DELETE SET NULL;

ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS audit_log_user_id_fkey;