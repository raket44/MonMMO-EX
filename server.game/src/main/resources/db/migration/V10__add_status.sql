-- Non-volatile status (sleep/poison/burn/freeze/paralysis/toxic) in the cartridge bit layout:
-- sleep turns in bits 0-2, poison 8, burn 16, freeze 32, paralysis 64, toxic 128. It survives the
-- battle it was inflicted in until a heal clears it.
ALTER TABLE pokemon ADD COLUMN status INT NOT NULL DEFAULT 0;
