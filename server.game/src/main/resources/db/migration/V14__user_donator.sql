-- Donator status per login account: the client shows "Donator Status active. <time> left." in
-- the menu header while the join response's first int (an epoch second) lies in the future.
CREATE TABLE user_donator (
  user_id     INT    PRIMARY KEY,
  until_epoch BIGINT NOT NULL
);
