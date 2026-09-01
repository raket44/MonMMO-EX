-- Client item id of the item the monster is holding, 0 for none.
ALTER TABLE pokemon ADD COLUMN held_item INT NOT NULL DEFAULT 0;
