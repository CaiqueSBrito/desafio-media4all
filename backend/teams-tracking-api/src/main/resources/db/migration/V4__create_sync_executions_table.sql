CREATE TABLE sync_executions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sync_type VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    started_at TIMESTAMP(6) NOT NULL,
    finished_at TIMESTAMP(6),
    duration_millis BIGINT,
    records_read INTEGER NOT NULL DEFAULT 0,
    records_created INTEGER NOT NULL DEFAULT 0,
    records_updated INTEGER NOT NULL DEFAULT 0,
    records_ignored INTEGER NOT NULL DEFAULT 0,
    records_failed INTEGER NOT NULL DEFAULT 0,
    retry_attempts INTEGER NOT NULL DEFAULT 0,
    rate_limit_errors INTEGER NOT NULL DEFAULT 0,
    service_unavailable_errors INTEGER NOT NULL DEFAULT 0,
    cursor_value_before VARCHAR(500),
    cursor_value_after VARCHAR(500),
    error_message VARCHAR(1000),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_sync_executions PRIMARY KEY (id)
);

CREATE INDEX idx_sync_executions_sync_type ON sync_executions (sync_type);
CREATE INDEX idx_sync_executions_status ON sync_executions (status);
CREATE INDEX idx_sync_executions_started_at ON sync_executions (started_at);
CREATE INDEX idx_sync_executions_type_started_at ON sync_executions (sync_type, started_at);
