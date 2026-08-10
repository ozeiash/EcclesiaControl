-- Tenant (Igreja, Filial, etc.)
CREATE TABLE tenant (
                        id UUID PRIMARY KEY,
                        name VARCHAR(255) NOT NULL UNIQUE,
                        legal_name VARCHAR(255),
                        type VARCHAR(50) NOT NULL CHECK(type IN ('SEDE', 'FILIAL', 'CONGREGACAO', 'DEPARTAMENTO')),
                        parent_tenant_id UUID REFERENCES tenant(id) ON DELETE SET NULL,
                        cnpj VARCHAR(20),
                        email VARCHAR(255),
                        phone VARCHAR(20),
                        address VARCHAR(500),
                        city VARCHAR(100),
                        state VARCHAR(2),
                        postal_code VARCHAR(10),
                        is_active BOOLEAN DEFAULT TRUE NOT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Users
CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
                       email VARCHAR(255) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       full_name VARCHAR(255),
                       phone VARCHAR(20),
                       status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
                       last_login TIMESTAMP,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       UNIQUE(email, tenant_id)
);

-- Roles
CREATE TABLE roles (
                       id UUID PRIMARY KEY,
                       tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
                       name VARCHAR(100) NOT NULL,
                       description VARCHAR(500) NOT NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       UNIQUE(name, tenant_id)
);

-- Permissions
CREATE TABLE permissions (
                             id UUID PRIMARY KEY,
                             code VARCHAR(100) NOT NULL UNIQUE,
                             description VARCHAR(500) NOT NULL,
                             category VARCHAR(100)
);

-- User-Role Mapping
CREATE TABLE user_roles (
                            user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                            role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
                            PRIMARY KEY (user_id, role_id)
);

-- Role-Permission Mapping
CREATE TABLE role_permissions (
                                  role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
                                  permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
                                  PRIMARY KEY (role_id, permission_id)
);

-- Audit Log
CREATE TABLE audit_log (
                           id UUID PRIMARY KEY,
                           tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
                           user_id UUID REFERENCES users(id) ON DELETE SET NULL,
                           action VARCHAR(100) NOT NULL,
                           entity_type VARCHAR(100) NOT NULL,
                           entity_id UUID NOT NULL,
                           old_value JSONB,
                           new_value JSONB,
                           ip_address VARCHAR(45),
                           user_agent TEXT,
                           timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices
CREATE INDEX idx_tenant_parent ON tenant(parent_tenant_id);
CREATE INDEX idx_users_tenant ON users(tenant_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_roles_tenant ON roles(tenant_id);
CREATE INDEX idx_audit_tenant ON audit_log(tenant_id);
CREATE INDEX idx_audit_timestamp ON audit_log(timestamp);