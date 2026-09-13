-- The monster's form number on its species (the client's form byte): Unown B is 201 form 1, Rotom
-- Heat is 479 form 1. Existing monsters are the base form. Monsters stored under an Expansion id for
-- a form the retail client already owns are normalized to species + form when they load.
ALTER TABLE pokemon ADD COLUMN form SMALLINT NOT NULL DEFAULT 0;
