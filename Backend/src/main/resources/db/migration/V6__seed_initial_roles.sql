-- Roles (catálogo compartilhado)
INSERT INTO roles (id, name, description, scope) VALUES
                                                     (gen_random_uuid(), 'SUPER_ADMIN', 'Administrador geral do sistema', 'GLOBAL'),
                                                     (gen_random_uuid(), 'PASTOR_SEDE', 'Pastor da Sede — acesso a todas as filiais', 'GLOBAL'),
                                                     (gen_random_uuid(), 'PASTOR_FILIAL', 'Pastor responsável por uma filial', 'LOCAL'),
                                                     (gen_random_uuid(), 'SECRETARIO', 'Secretaria da filial', 'LOCAL'),
                                                     (gen_random_uuid(), 'TESOUREIRO', 'Tesouraria da filial', 'LOCAL'),
                                                     (gen_random_uuid(), 'LIDER_MINISTERIO', 'Líder de ministério/departamento', 'LOCAL'),
                                                     (gen_random_uuid(), 'LIDER_CELULA', 'Líder de célula/pequeno grupo', 'LOCAL'),
                                                     (gen_random_uuid(), 'VOLUNTARIO', 'Voluntário da filial', 'LOCAL'),
                                                     (gen_random_uuid(), 'MEMBRO', 'Membro comum, acesso ao portal', 'LOCAL');

-- Permissions (catálogo mínimo — cresce conforme os módulos forem nascendo)
INSERT INTO permissions (id, code, description, category) VALUES
                                                              (gen_random_uuid(), 'MEMBER_READ', 'Visualizar membros', 'PESSOAS'),
                                                              (gen_random_uuid(), 'MEMBER_WRITE', 'Criar/editar membros', 'PESSOAS'),
                                                              (gen_random_uuid(), 'AUDIT_READ', 'Visualizar log de auditoria', 'GOVERNANCA'),
                                                              (gen_random_uuid(), 'ROLE_MANAGE', 'Gerenciar papéis e permissões', 'GOVERNANCA');

-- Vínculo inicial: SUPER_ADMIN tem todas as permissions cadastradas
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.name = 'SUPER_ADMIN';