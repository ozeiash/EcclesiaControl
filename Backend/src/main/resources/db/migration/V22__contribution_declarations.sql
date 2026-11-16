-- Contribuição declarada pelo membro (aguarda confirmação do tesoureiro) e chave PIX da filial.
-- A declaração NÃO é lançamento: só vira contribuição (e entra no financeiro) quando o tesoureiro confirma.

ALTER TABLE tenant ADD COLUMN pix_key VARCHAR(140);

CREATE TABLE contribution_declarations (
    id               UUID PRIMARY KEY,
    tenant_id        UUID           NOT NULL REFERENCES tenant (id),
    member_id        UUID           NOT NULL REFERENCES members (id),
    account_id       UUID           NOT NULL REFERENCES chart_of_accounts (id),
    amount           NUMERIC(14, 2) NOT NULL CHECK (amount > 0),
    declared_date    DATE           NOT NULL,
    payment_method   VARCHAR(20)    NOT NULL,
    pix_reference    VARCHAR(64),
    note             VARCHAR(255),
    status           VARCHAR(20)    NOT NULL DEFAULT 'PENDENTE'
                         CHECK (status IN ('PENDENTE', 'CONFIRMADA', 'REJEITADA', 'CANCELADA')),
    decided_by       UUID,
    decided_at       TIMESTAMP,
    rejection_reason VARCHAR(255),
    contribution_id  UUID REFERENCES contributions (id),
    created_at       TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_declarations_tenant_status ON contribution_declarations (tenant_id, status, created_at DESC);
CREATE INDEX idx_declarations_member        ON contribution_declarations (member_id, created_at DESC);

-- Mesma referência PIX não pode ser declarada duas vezes na filial (rede de segurança; o serviço checa antes).
CREATE UNIQUE INDEX uq_declarations_pix_reference
    ON contribution_declarations (tenant_id, pix_reference)
    WHERE pix_reference IS NOT NULL AND status IN ('PENDENTE', 'CONFIRMADA');
