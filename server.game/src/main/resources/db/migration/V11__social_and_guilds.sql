-- Friends, block lists and teams (guilds) used to live only in server memory and vanished on
-- every restart, with the founding fee already spent.
CREATE TABLE friends (
  user_id INT         NOT NULL,
  name    VARCHAR(32) NOT NULL,
  PRIMARY KEY (user_id, name)
);

CREATE TABLE blocked_players (
  user_id INT         NOT NULL,
  name    VARCHAR(32) NOT NULL,
  PRIMARY KEY (user_id, name)
);

CREATE TABLE guilds (
  id         BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name       VARCHAR(32) NOT NULL,
  tag        VARCHAR(8)  NOT NULL,
  created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE guild_members (
  -- A member is keyed by character id; a character belongs to at most one guild.
  char_id  BIGINT      PRIMARY KEY,
  guild_id BIGINT      NOT NULL REFERENCES guilds (id) ON DELETE CASCADE,
  name     VARCHAR(32) NOT NULL,
  rank     SMALLINT    NOT NULL,
  leader   BOOLEAN     NOT NULL DEFAULT FALSE,
  position INT         NOT NULL DEFAULT 0
);

CREATE TABLE guild_rank_permissions (
  guild_id    BIGINT   NOT NULL REFERENCES guilds (id) ON DELETE CASCADE,
  rank        SMALLINT NOT NULL,
  -- Bit i set = GuildPermission ordinal i granted.
  permissions INT      NOT NULL,
  PRIMARY KEY (guild_id, rank)
);

CREATE TABLE guild_log (
  id       BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  guild_id BIGINT      NOT NULL REFERENCES guilds (id) ON DELETE CASCADE,
  type     SMALLINT    NOT NULL,
  actor    VARCHAR(32) NOT NULL,
  target   VARCHAR(32) NOT NULL,
  ts       INT         NOT NULL
);
