-- V12__contributions_rename_and_reserved_accounts.sql

ALTER TABLE tithe_contributions RENAME TO contributions;
ALTER INDEX idx_tithe_member RENAME TO idx_contributions_member;

-- UPDATE e não delete/insert: os vínculos em role_permissions seguem o id e continuam válidos
UPDATE permissions
SET code = 'CONTRIBUTION_DETAIL_READ',
    description = 'Ver o vínculo entre contribuição (dízimo/oferta) e membro (dado sigiloso)'
WHERE code = 'TITHE_DETAIL_READ';

INSERT INTO chart_of_accounts (id, code, name, type, category) VALUES
                                                                   (gen_random_uuid(), 'DESP-003', 'Repasse à Sede/Convenção', 'DESPESA', 'REPASSE'),
                                                                   (gen_random_uuid(), 'REC-004', 'Repasse Recebido', 'RECEITA', 'REPASSE');