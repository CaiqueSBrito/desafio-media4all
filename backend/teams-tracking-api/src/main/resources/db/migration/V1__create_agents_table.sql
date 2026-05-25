CREATE TABLE agents (
    id VARCHAR(100) NOT NULL,
    external_id VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    role VARCHAR(40) NOT NULL,
    team VARCHAR(100),
    phone VARCHAR(30),
    email VARCHAR(150),
    active BOOLEAN NOT NULL,
    status VARCHAR(40) NOT NULL,
    battery INTEGER,
    last_seen TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_agents PRIMARY KEY (id),
    CONSTRAINT uk_agents_external_id UNIQUE (external_id)
);

CREATE INDEX idx_agents_status ON agents (status);
CREATE INDEX idx_agents_last_seen ON agents (last_seen);
