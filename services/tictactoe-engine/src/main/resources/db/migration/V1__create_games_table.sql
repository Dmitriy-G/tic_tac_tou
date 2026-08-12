CREATE TABLE games
(
    id    UUID PRIMARY KEY,
    board VARCHAR(9)  NOT NULL,
    state VARCHAR(20) NOT NULL
);