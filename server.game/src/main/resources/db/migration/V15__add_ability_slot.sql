-- Which of the species' ability slots the monster carries: 0 first, 1 second, 2 hidden (honoured
-- only together with has_hidden_ability). The client shows the ability from this byte of the monster
-- record; the server used to send 0 while battles picked the slot from the personality's low bit, so
-- a monster could show one ability and battle with another. Existing monsters keep the ability they
-- battled with (project owner, 2026-09-13): the seed's low bit. MOD keeps the sign of a negative
-- seed, hence the CASE rather than MOD alone.
ALTER TABLE pokemon ADD COLUMN ability_slot SMALLINT NOT NULL DEFAULT 0;
UPDATE pokemon SET ability_slot = CASE WHEN MOD(seed, 2) = 0 THEN 0 ELSE 1 END;
