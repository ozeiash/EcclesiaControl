-- ================================================================
-- Migration 3: Consolidar e-mail único global, roles como catálogo
-- compartilhado, e remover mecanismo de RBAC legado (user_roles)
-- ================================================================

-- 1. E-mail único globalmente
ALTER TABLE users DROP CONSTRAINT users_email_tenant_id_key;
ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);
DROP INDEX IF EXISTS idx_users_tenant; -- índice órfão: users.tenant_id deixa de ser usado para autorização

-- 2. Roles viram catálogo compartilhado (não pertencem mais a uma filial)
ALTER TABLE roles DROP CONSTRAINT roles_tenant_id_fkey;
ALTER TABLE roles DROP CONSTRAINT roles_name_tenant_id_key;
ALTER TABLE roles DROP COLUMN tenant_id;
ALTER TABLE roles ADD CONSTRAINT uk_roles_name UNIQUE (name);

-- 3. Remover coluna não utilizada, sobra de tentativa anterior
ALTER TABLE user_filial_role DROP COLUMN active_filial;

-- 4. Remover RBAC legado — substituído inteiramente por user_filial_role
DROP TABLE IF EXISTS user_roles;