CREATE TABLE sync_cursors (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sync_type VARCHAR(40) NOT NULL,
    last_cursor_value VARCHAR(500),
    last_page INTEGER,
    last_occurred_at TIMESTAMP(6),
    last_synced_at TIMESTAMP(6),
    last_successful_sync_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_sync_cursors PRIMARY KEY (id),
    CONSTRAINT uk_sync_cursors_sync_type UNIQUE (sync_type)
);

CREATE INDEX idx_sync_cursors_updated_at ON sync_cursors (updated_at);
