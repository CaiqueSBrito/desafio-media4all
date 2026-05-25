CREATE TABLE sync_conflicts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sync_execution_id BIGINT NOT NULL,
    sync_type VARCHAR(40) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    conflict_key VARCHAR(200) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    local_snapshot_json TEXT,
    external_payload_json TEXT,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_sync_conflicts PRIMARY KEY (id),
    CONSTRAINT fk_sync_conflicts_execution FOREIGN KEY (sync_execution_id) REFERENCES sync_executions (id)
);

CREATE INDEX idx_sync_conflicts_execution_id ON sync_conflicts (sync_execution_id);
CREATE INDEX idx_sync_conflicts_sync_type ON sync_conflicts (sync_type);
CREATE INDEX idx_sync_conflicts_created_at ON sync_conflicts (created_at);
CREATE INDEX idx_sync_conflicts_type_created_at ON sync_conflicts (sync_type, created_at);
