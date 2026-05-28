-- Normalize OAuth provider values to the enum code stored by OAuthProvider#getCode().
-- The OAuth lookup path uses lowercase provider codes such as "github"; older rows
-- may contain enum names such as "GITHUB", causing duplicate inserts on login.

SET search_path TO nexusvoice;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM users
        WHERE oauth_provider IS NOT NULL
          AND oauth_id IS NOT NULL
          AND deleted = 0
        GROUP BY lower(oauth_provider), oauth_id
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot normalize OAuth providers: duplicate active OAuth bindings exist after lowercasing';
    END IF;
END $$;

DROP INDEX IF EXISTS idx_users_oauth_unique;

UPDATE users
SET oauth_provider = lower(oauth_provider)
WHERE oauth_provider IS NOT NULL
  AND oauth_provider <> lower(oauth_provider);

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_oauth_unique
    ON users(oauth_provider, oauth_id)
    WHERE oauth_provider IS NOT NULL
      AND oauth_id IS NOT NULL
      AND deleted = 0;
