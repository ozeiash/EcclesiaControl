-- Adicionar coluna scope à tabela roles
ALTER TABLE roles ADD COLUMN scope VARCHAR(50) DEFAULT 'LOCAL'
    CHECK(scope IN ('LOCAL', 'GLOBAL'));

-- Criar tabela user_filial_role
CREATE TABLE user_filial_role (
                                  id UUID PRIMARY KEY,
                                  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                  filial_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
                                  role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
                                  active_filial UUID,
                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  UNIQUE(user_id, filial_id, role_id)
);

-- Índices para performance
CREATE INDEX idx_user_filial_role_user ON user_filial_role(user_id);
CREATE INDEX idx_user_filial_role_filial ON user_filial_role(filial_id);
CREATE INDEX idx_user_filial_role_role ON user_filial_role(role_id);
CREATE INDEX idx_user_filial_role_active_filial ON user_filial_role(active_filial);