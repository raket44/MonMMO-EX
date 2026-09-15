-- The trainer card's "Time played" (client eu6.zz, seconds) and the moment a character last left
-- the game, shown as "Last Online" to friends and team members. They were never stored, so the
-- card said 0 hours and every friend showed the epoch (12/31/69).
ALTER TABLE characters ADD COLUMN play_time_seconds INT NOT NULL DEFAULT 0;
ALTER TABLE characters ADD COLUMN last_logout TIMESTAMP;

-- When a friend was added and when a member joined a team. Rows older than this migration get
-- the migration's own time: the real dates were never recorded.
ALTER TABLE friends ADD COLUMN added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE guild_members ADD COLUMN joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
