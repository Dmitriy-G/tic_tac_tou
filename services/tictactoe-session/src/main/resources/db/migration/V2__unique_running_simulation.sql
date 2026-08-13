ALTER TABLE simulations ADD COLUMN running_session_id UUID;

UPDATE simulations SET running_session_id = session_id WHERE status = 'IN_PROGRESS';

CREATE UNIQUE INDEX uq_simulation_running ON simulations (running_session_id);
