-- V8__expand_role_catalog.sql

-- Renomeia papéis existentes para o padrão SEDE/FILIAL (preserva o id — vínculos
-- em user_filial_role continuam válidos, ninguém perde acesso)
UPDATE roles SET name = 'SECRETARIO_FILIAL' WHERE name = 'SECRETARIO';
UPDATE roles SET name = 'TESOUREIRO_FILIAL' WHERE name = 'TESOUREIRO';

-- Novos papéis
INSERT INTO roles (id, name, description, scope) VALUES
                                                     (gen_random_uuid(), 'SECRETARIO_SEDE', 'Secretaria da Sede — aprova transferências entre filiais e relatórios globais', 'GLOBAL'),
                                                     (gen_random_uuid(), 'TESOUREIRO_SEDE', 'Tesouraria da Sede — consolidado de caixa de todas as filiais', 'GLOBAL'),
                                                     (gen_random_uuid(), 'TI_ADMIN', 'Suporte técnico — configura integrações, sem acesso a dado de membro ou financeiro', 'GLOBAL'),
                                                     (gen_random_uuid(), 'CONTADOR', 'Contabilidade — leitura de relatórios financeiros consolidados, sem dado pessoal de membro', 'GLOBAL');

-- Vínculo de permissões (com o catálogo atual — MEMBER_READ/MEMBER_WRITE/AUDIT_READ/ROLE_MANAGE)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name IN ('PASTOR_SEDE', 'PASTOR_FILIAL', 'SECRETARIO_SEDE', 'SECRETARIO_FILIAL')
  AND p.code IN ('MEMBER_READ', 'MEMBER_WRITE');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name IN ('TESOUREIRO_SEDE', 'TESOUREIRO_FILIAL', 'LIDER_MINISTERIO', 'LIDER_CELULA')
  AND p.code = 'MEMBER_READ';

-- TI_ADMIN, CONTADOR, VOLUNTARIO, MEMBRO: nenhuma permission ainda —
-- intencional. CONTADOR/financeiro e MEMBRO/portal-do-membro entram
-- quando os respectivos módulos forem construídos (Financeiro, Portal
-- do Membro — Sprint 10). AUDIT_READ permanece restrito a SUPER_ADMIN.