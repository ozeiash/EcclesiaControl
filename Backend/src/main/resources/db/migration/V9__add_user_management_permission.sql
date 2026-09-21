-- V9__add_user_management_permission.sql

INSERT INTO permissions (id, code, description, category) VALUES
    (gen_random_uuid(), 'USER_MANAGE', 'Criar usuários e atribuir papéis dentro do próprio escopo', 'GOVERNANCA');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name IN ('PASTOR_SEDE', 'PASTOR_FILIAL', 'SECRETARIO_SEDE', 'SECRETARIO_FILIAL')
  AND p.code = 'USER_MANAGE';