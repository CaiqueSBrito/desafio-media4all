ALTER TABLE agent_positions
    ADD CONSTRAINT uk_agent_positions_snapshot UNIQUE (agent_id, last_seen, latitude, longitude);
