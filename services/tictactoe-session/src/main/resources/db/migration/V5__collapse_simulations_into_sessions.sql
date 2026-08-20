-- Session is the game. Status, outcome and error move onto sessions.
ALTER TABLE sessions ADD COLUMN status       VARCHAR(20) NOT NULL DEFAULT 'CREATED';
ALTER TABLE sessions ADD COLUMN board        VARCHAR(9)  NOT NULL DEFAULT '.........';
ALTER TABLE sessions ADD COLUMN winner       VARCHAR(1);
ALTER TABLE sessions ADD COLUMN errors_count INTEGER     NOT NULL DEFAULT 0;
ALTER TABLE sessions ADD COLUMN error_code   VARCHAR(64);
ALTER TABLE sessions ADD COLUMN error_message VARCHAR(1000);
ALTER TABLE sessions ADD COLUMN started_at   TIMESTAMP;
ALTER TABLE sessions ADD COLUMN finished_at  TIMESTAMP;

ALTER TABLE sessions ALTER COLUMN status DROP DEFAULT;
ALTER TABLE sessions ALTER COLUMN board  DROP DEFAULT;

-- Move history becomes durable. "session and move data" in the spec.
CREATE TABLE session_moves
(
    session_id  UUID        NOT NULL REFERENCES sessions (id) ON DELETE CASCADE,
    move_number SMALLINT    NOT NULL,
    symbol      VARCHAR(1)  NOT NULL,
    position    SMALLINT    NOT NULL,
    step_status VARCHAR(32) NOT NULL,
    created_at  TIMESTAMP   NOT NULL,
    PRIMARY KEY (session_id, move_number)
);

DROP TABLE simulations;