-- Permissions granted to an account rather than a character: staff accounts whose characters
-- are created with these bits instead of the default. Keyed by the login server's user id.
CREATE TABLE user_permissions (
  user_id     INT PRIMARY KEY,
  permissions INT NOT NULL
);
