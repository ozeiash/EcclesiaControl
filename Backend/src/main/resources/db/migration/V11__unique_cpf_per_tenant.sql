-- V11__unique_cpf_per_tenant.sql
CREATE UNIQUE INDEX uk_members_tenant_cpf_hash ON members(tenant_id, cpf_hash)
    WHERE cpf_hash IS NOT NULL AND membership_status <> 'DUPLICATE';