CREATE TABLE sessions
(
    id UUID PRIMARY KEY
);

CREATE TABLE simulations
(
    id           UUID PRIMARY KEY,
    session_id   UUID         NOT NULL REFERENCES sessions (id),
    errors_count INTEGER      NOT NULL DEFAULT 0,
    started_at   TIMESTAMP    NOT NULL,
    finished_at  TIMESTAMP,
    status       VARCHAR(20)  NOT NULL
);

CREATE INDEX idx_simulations_session_id ON simulations (session_id);