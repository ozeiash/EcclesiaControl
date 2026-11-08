-- Remove CPF/RG que nunca deveriam ter sido gravados em texto puro no audit_log
UPDATE audit_log
SET old_value = old_value - 'cpf' - 'rg',
    new_value = new_value - 'cpf' - 'rg'
WHERE entity_type = 'Member';