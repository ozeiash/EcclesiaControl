-- V14__accounting_periods.sql

CREATE TABLE accounting_periods (
                                    id UUID PRIMARY KEY,
                                    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
                                    reference_month DATE NOT NULL,
                                    status VARCHAR(10) NOT NULL CHECK (status IN ('ABERTO', 'FECHADO')),
                                    total_income NUMERIC(14,2),
                                    total_expense NUMERIC(14,2),
                                    transaction_count INTEGER,
                                    closed_at TIMESTAMP,
                                    closed_by UUID,
                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    CONSTRAINT uk_period_tenant_month UNIQUE (tenant_id, reference_month),
                                    CONSTRAINT ck_period_first_day CHECK (EXTRACT(DAY FROM reference_month) = 1)
);
CREATE INDEX idx_periods_tenant_status ON accounting_periods(tenant_id, status);

-- Append-only: sem endpoint de update/delete
CREATE TABLE accounting_period_events (
                                          id UUID PRIMARY KEY,
                                          tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
                                          reference_month DATE NOT NULL,
                                          event_type VARCHAR(10) NOT NULL CHECK (event_type IN ('CLOSED', 'REOPENED')),
                                          reason VARCHAR(500),
                                          cascaded BOOLEAN NOT NULL DEFAULT FALSE,
                                          total_income NUMERIC(14,2),
                                          total_expense NUMERIC(14,2),
                                          transaction_count INTEGER,
                                          actor_id UUID,
                                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_period_events_tenant_month ON accounting_period_events(tenant_id, reference_month);

INSERT INTO permissions (id, code, description, category) VALUES
                                                              (gen_random_uuid(), 'PERIOD_CLOSE', 'Fechar o período contábil mensal da filial ativa', 'FINANCEIRO'),
                                                              (gen_random_uuid(), 'PERIOD_REOPEN', 'Reabrir períodos fechados (exige motivo; reabre os posteriores)', 'FINANCEIRO');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name IN ('TESOUREIRO_SEDE', 'TESOUREIRO_FILIAL') AND p.code = 'PERIOD_CLOSE';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'TESOUREIRO_SEDE' AND p.code = 'PERIOD_REOPEN';

-- SUPER_ADMIN: mesma regra da V13
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);