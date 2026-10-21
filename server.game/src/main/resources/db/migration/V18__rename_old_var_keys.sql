-- Character vars written under the old project prefix move to the Pirated Version prefix,
-- so battle points, rail lines, Repeat Ball streaks and raid win days carry over.
UPDATE character_vars
SET var_key = 'pirated.' || substring(var_key FROM 8)
WHERE var_key LIKE 'monmmo.%';
