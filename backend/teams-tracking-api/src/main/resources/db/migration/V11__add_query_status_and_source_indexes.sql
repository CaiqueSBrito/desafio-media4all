CREATE INDEX idx_agent_positions_status_last_seen ON agent_positions (status, last_seen);
CREATE INDEX idx_agent_positions_agent_status_last_seen ON agent_positions (agent_id, status, last_seen);

CREATE INDEX idx_check_ins_agent_source_occurred_at ON check_ins (agent_id, source, occurred_at);
CREATE INDEX idx_check_ins_source_occurred_at ON check_ins (source, occurred_at);
CREATE INDEX idx_check_ins_type_occurred_at ON check_ins (type, occurred_at);

CREATE INDEX idx_sync_executions_status_started_at ON sync_executions (status, started_at);
CREATE INDEX idx_sync_executions_type_status_started_at ON sync_executions (sync_type, status, started_at);

CREATE INDEX idx_sync_failures_type_created_at ON sync_failures (sync_type, created_at);
CREATE INDEX idx_sync_failures_execution_created_at ON sync_failures (sync_execution_id, created_at);

CREATE INDEX idx_geofences_type_synced_at ON geofences (type, synced_at);
