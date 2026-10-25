-- V15__accounting_start_month.sql

ALTER TABLE tenant ADD COLUMN accounting_start_month DATE;
ALTER TABLE tenant ADD CONSTRAINT ck_tenant_start_first_day
    CHECK (accounting_start_month IS NULL OR EXTRACT(DAY FROM accounting_start_month) = 1);

-- Filiais que já têm lançamentos começam no mês do lançamento mais antigo
UPDATE tenant t
SET accounting_start_month = (
    SELECT date_trunc('month', MIN(ft.transaction_date))::date
    FROM financial_transactions ft WHERE ft.tenant_id = t.id)
WHERE EXISTS (SELECT 1 FROM financial_transactions ft WHERE ft.tenant_id = t.id);

INSERT INTO permissions (id, code, description, category) VALUES
    (gen_random_uuid(), 'PERIOD_SETUP', 'Definir o mês de início da escrituração de uma filial', 'FINANCEIRO');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'TESOUREIRO_SEDE' AND p.code = 'PERIOD_SETUP';

-- SUPER_ADMIN: mesma regra da V13
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);