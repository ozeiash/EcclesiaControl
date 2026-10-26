ALTER TABLE users ADD COLUMN member_id UUID NULL REFERENCES members(id) ON DELETE SET NULL;
CREATE UNIQUE INDEX ux_users_member_id ON users(member_id) WHERE member_id IS NOT NULL;
