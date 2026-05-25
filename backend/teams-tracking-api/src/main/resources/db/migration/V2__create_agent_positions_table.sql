CREATE TABLE agent_positions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    agent_id VARCHAR(100) NOT NULL,
    latitude DECIMAL(9, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    current_address VARCHAR(500),
    accuracy DECIMAL(10, 2),
    speed DECIMAL(10, 2),
    battery INTEGER,
    status VARCHAR(40) NOT NULL,
    last_seen TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_agent_positions PRIMARY KEY (id),
    CONSTRAINT fk_agent_positions_agent FOREIGN KEY (agent_id) REFERENCES agents (id)
);

CREATE INDEX idx_agent_positions_agent_id ON agent_positions (agent_id);
CREATE INDEX idx_agent_positions_last_seen ON agent_positions (last_seen);
CREATE INDEX idx_agent_positions_agent_last_seen ON agent_positions (agent_id, last_seen);
