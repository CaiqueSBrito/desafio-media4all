CREATE TABLE sync_failures (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sync_execution_id BIGINT NOT NULL,
    sync_type VARCHAR(40) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    payload_json TEXT,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_sync_failures PRIMARY KEY (id),
    CONSTRAINT fk_sync_failures_execution FOREIGN KEY (sync_execution_id) REFERENCES sync_executions (id)
);

CREATE INDEX idx_sync_failures_execution_id ON sync_failures (sync_execution_id);
CREATE INDEX idx_sync_failures_sync_type ON sync_failures (sync_type);
CREATE INDEX idx_sync_failures_created_at ON sync_failures (created_at);
