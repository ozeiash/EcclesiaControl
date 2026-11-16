-- Portal do membro: vínculo opcional User -> Member e permissão PORTAL_ACCESS.

ALTER TABLE users ADD COLUMN member_id UUID REFERENCES members (id);

-- Um membro tem no máximo uma conta de acesso.
CREATE UNIQUE INDEX uq_users_member_id ON users (member_id) WHERE member_id IS NOT NULL;

INSERT INTO permissions (id, code, description, category) VALUES
    (gen_random_uuid(), 'PORTAL_ACCESS', 'Acessar o portal do membro (ver e editar os próprios dados)', 'PORTAL');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
 WHERE r.name = 'MEMBRO' AND p.code = 'PORTAL_ACCESS'
   AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- SUPER_ADMIN: mesma regra da V13
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
 WHERE r.name = 'SUPER_ADMIN'
   AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
