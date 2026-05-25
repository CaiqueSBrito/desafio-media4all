CREATE TABLE check_ins (
    id VARCHAR(100) NOT NULL,
    agent_id VARCHAR(100) NOT NULL,
    type VARCHAR(40) NOT NULL,
    source VARCHAR(40) NOT NULL,
    latitude DECIMAL(9, 7),
    longitude DECIMAL(10, 7),
    address VARCHAR(500),
    accuracy DECIMAL(10, 2),
    speed DECIMAL(10, 2),
    notes VARCHAR(1000),
    distance_from_previous DECIMAL(12, 2),
    external_event_id VARCHAR(100),
    occurred_at TIMESTAMP(6) NOT NULL,
    synced_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_check_ins PRIMARY KEY (id),
    CONSTRAINT fk_check_ins_agent FOREIGN KEY (agent_id) REFERENCES agents (id),
    CONSTRAINT uk_check_ins_external_event_id UNIQUE (external_event_id)
);

CREATE INDEX idx_check_ins_agent_id ON check_ins (agent_id);
CREATE INDEX idx_check_ins_type ON check_ins (type);
CREATE INDEX idx_check_ins_source ON check_ins (source);
CREATE INDEX idx_check_ins_occurred_at ON check_ins (occurred_at);
CREATE INDEX idx_check_ins_synced_at ON check_ins (synced_at);
CREATE INDEX idx_check_ins_agent_occurred_at ON check_ins (agent_id, occurred_at);
