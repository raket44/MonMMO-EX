-- Cosmetic variant (the client skin set's per-slot "extra" byte, N50.wn0): selects an addon's
-- alternate form, e.g. Noble Steed -> Noble Steed (Alt). 0 = the base form.

ALTER TABLE character_skins
  ADD COLUMN skin_variant SMALLINT NOT NULL DEFAULT 0;
