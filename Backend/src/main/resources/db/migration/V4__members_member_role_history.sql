-- ================================================================
-- Migration 4: Tabela members (completa) + histórico de cargo
-- ================================================================

CREATE TABLE members (
                         id UUID PRIMARY KEY,
                         tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,

                         full_name VARCHAR(255) NOT NULL,
                         social_name VARCHAR(255),
                         birth_date DATE,
                         gender VARCHAR(20),
                         cpf_encrypted VARCHAR(500),
                         cpf_hash VARCHAR(64),
                         rg_encrypted VARCHAR(500),

                         email VARCHAR(255),
                         phone_primary VARCHAR(20),
                         phone_secondary VARCHAR(20),
                         postal_code VARCHAR(10),
                         street VARCHAR(255),
                         number VARCHAR(20),
                         complement VARCHAR(100),
                         neighborhood VARCHAR(100),
                         city VARCHAR(100),
                         state VARCHAR(2),

                         membership_status VARCHAR(30) NOT NULL DEFAULT 'ATIVO'
                             CHECK (membership_status IN ('ATIVO', 'INATIVO', 'AFASTADO', 'SOB_DISCIPLINA', 'FALECIDO')),
                         admission_date DATE,
                         admission_way VARCHAR(30),
                         exit_date DATE,
                         exit_way VARCHAR(30),
                         is_baptized BOOLEAN NOT NULL DEFAULT FALSE,
                         baptism_date DATE,
                         is_confirmed_or_professed BOOLEAN,

                         marital_status VARCHAR(20),
                         wedding_date DATE,
                         spouse_name VARCHAR(255),
                         father_name VARCHAR(255),
                         mother_name VARCHAR(255),

                         ministry_role VARCHAR(100),

                         photo_url VARCHAR(500),
                         observations TEXT,
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_members_tenant ON members(tenant_id);
CREATE INDEX idx_members_cpf_hash ON members(cpf_hash);
CREATE INDEX idx_members_status ON members(membership_status);

CREATE TABLE member_role_history (
                                     id UUID PRIMARY KEY,
                                     tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
                                     member_id UUID NOT NULL REFERENCES members(id) ON DELETE CASCADE,
                                     role_name VARCHAR(100) NOT NULL,
                                     started_at DATE NOT NULL,
                                     ended_at DATE,
                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_member_role_history_tenant ON member_role_history(tenant_id);
CREATE INDEX idx_member_role_history_member ON member_role_history(member_id);