-- Quem gerencia usuários precisa poder atribuir o papel MEMBRO sem esbarrar no teto de permissões.
-- PORTAL_ACCESS só abre o /me, e o /me exige conta vinculada a um membro; para qualquer outra conta responde 403.
INSERT INTO role_permissions (role_id, permission_id)
SELECT DISTINCT rp.role_id, portal.id
FROM role_permissions rp
         JOIN permissions um     ON um.id = rp.permission_id AND um.code = 'USER_MANAGE'
         JOIN permissions portal ON portal.code = 'PORTAL_ACCESS'
WHERE NOT EXISTS (SELECT 1 FROM role_permissions x
                  WHERE x.role_id = rp.role_id AND x.permission_id = portal.id);