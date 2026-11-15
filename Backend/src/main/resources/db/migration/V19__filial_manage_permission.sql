-- CRUD de filiais: cadastrar e editar filiais.
-- A permissão só deve existir em papéis GLOBAL (sede). Não vincular a papéis LOCAL.

INSERT INTO permissions (id, code, description, category) VALUES
    (gen_random_uuid(), 'FILIAL_MANAGE', 'Cadastrar e editar filiais (somente papéis da sede)', 'GOVERNANCA');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
 WHERE r.name = 'PASTOR_SEDE' AND p.code = 'FILIAL_MANAGE'
   AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- SUPER_ADMIN: mesma regra da V13
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
 WHERE r.name = 'SUPER_ADMIN'
   AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
