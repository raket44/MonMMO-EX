-- Friendship/happiness, the cartridge 0-255 scale. 70 is the common wild base;
-- hatched monsters start at 120, and happiness evolutions trigger at 220.
ALTER TABLE pokemon ADD COLUMN friendship INT NOT NULL DEFAULT 70;
