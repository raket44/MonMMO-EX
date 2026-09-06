-- Character names and team names/tags are unique regardless of case, as on retail. The server
-- checks before inserting; these indexes close the race between two simultaneous creations.
-- jOOQ's code generator simulates the schema on H2, which cannot take a functional index, so the
-- statements are hidden from it (they add no columns for it to see).
-- [jooq ignore start]
CREATE UNIQUE INDEX uq_characters_name_ci ON characters (lower(name));
CREATE UNIQUE INDEX uq_guilds_name_ci ON guilds (lower(name));
CREATE UNIQUE INDEX uq_guilds_tag_ci ON guilds (lower(tag));
-- [jooq ignore stop]
