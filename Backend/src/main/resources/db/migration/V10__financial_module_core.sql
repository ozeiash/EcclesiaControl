-- V10__financial_module_core.sql

CREATE TABLE chart_of_accounts (
                                   id UUID PRIMARY KEY,
                                   code VARCHAR(20) NOT NULL UNIQUE,
                                   name VARCHAR(150) NOT NULL,
                                   type VARCHAR(20) NOT NULL CHECK (type IN ('RECEITA', 'DESPESA')),
                                   category VARCHAR(50) NOT NULL,
                                   is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE financial_transactions (
                                        id UUID PRIMARY KEY,
                                        tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
                                        account_id UUID NOT NULL REFERENCES chart_of_accounts(id),
                                        type VARCHAR(20) NOT NULL CHECK (type IN ('RECEITA', 'DESPESA')),
                                        amount NUMERIC(14,2) NOT NULL CHECK (amount > 0),
                                        transaction_date DATE NOT NULL,
                                        description VARCHAR(255),
                                        attachment_url VARCHAR(500),
                                        created_by UUID REFERENCES users(id) ON DELETE SET NULL,
                                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fin_transactions_tenant ON financial_transactions(tenant_id);
CREATE INDEX idx_fin_transactions_date ON financial_transactions(transaction_date);
CREATE INDEX idx_fin_transactions_account ON financial_transactions(account_id);

CREATE TABLE tithe_contributions (
                                     id UUID PRIMARY KEY,
                                     tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
                                     transaction_id UUID NOT NULL UNIQUE REFERENCES financial_transactions(id) ON DELETE CASCADE,
                                     member_id UUID REFERENCES members(id) ON DELETE SET NULL,
                                     is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tithe_member ON tithe_contributions(member_id);

CREATE TABLE accounts_payable (
                                  id UUID PRIMARY KEY,
                                  tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
                                  account_id UUID NOT NULL REFERENCES chart_of_accounts(id),
                                  payee_name VARCHAR(255) NOT NULL,
                                  amount NUMERIC(14,2) NOT NULL CHECK (amount > 0),
                                  due_date DATE NOT NULL,
                                  expense_kind VARCHAR(20) NOT NULL DEFAULT 'FIXA' CHECK (expense_kind IN ('FIXA', 'EXTRAORDINARIA')),
                                  status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE'
                                      CHECK (status IN ('PENDENTE', 'PARCIAL', 'PAGO', 'VENCIDO', 'CANCELADO')),
                                  created_by UUID REFERENCES users(id) ON DELETE SET NULL,
                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payable_tenant ON accounts_payable(tenant_id);
CREATE INDEX idx_payable_status ON accounts_payable(status);

CREATE TABLE accounts_payable_settlements (
                                              id UUID PRIMARY KEY,
                                              tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
                                              accounts_payable_id UUID NOT NULL REFERENCES accounts_payable(id) ON DELETE CASCADE,
                                              financial_transaction_id UUID NOT NULL UNIQUE REFERENCES financial_transactions(id) ON DELETE CASCADE,
                                              amount NUMERIC(14,2) NOT NULL CHECK (amount > 0),
                                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payable_settlements_payable ON accounts_payable_settlements(accounts_payable_id);

INSERT INTO chart_of_accounts (id, code, name, type, category) VALUES
                                                                   (gen_random_uuid(), 'REC-001', 'Dízimos', 'RECEITA', 'DIZIMO'),
                                                                   (gen_random_uuid(), 'REC-002', 'Ofertas', 'RECEITA', 'OFERTA'),
                                                                   (gen_random_uuid(), 'REC-003', 'Ofertas Missionárias', 'RECEITA', 'MISSOES'),
                                                                   (gen_random_uuid(), 'DESP-001', 'Despesas Administrativas', 'DESPESA', 'DESPESA_ADMINISTRATIVA'),
                                                                   (gen_random_uuid(), 'DESP-002', 'Despesas Extraordinárias', 'DESPESA', 'DESPESA_EXTRAORDINARIA');

INSERT INTO permissions (id, code, description, category) VALUES
                                                              (gen_random_uuid(), 'FINANCE_READ', 'Ver lançamentos e relatórios financeiros agregados', 'FINANCEIRO'),
                                                              (gen_random_uuid(), 'FINANCE_WRITE', 'Lançar receitas/despesas, dar baixa em contas a pagar', 'FINANCEIRO'),
                                                              (gen_random_uuid(), 'TITHE_DETAIL_READ', 'Ver o vínculo entre lançamento e membro (dado sigiloso)', 'FINANCEIRO');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name IN ('TESOUREIRO_SEDE', 'TESOUREIRO_FILIAL')
  AND p.code IN ('FINANCE_READ', 'FINANCE_WRITE', 'TITHE_DETAIL_READ');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name IN ('PASTOR_SEDE', 'PASTOR_FILIAL', 'CONTADOR')
  AND p.code = 'FINANCE_READ';