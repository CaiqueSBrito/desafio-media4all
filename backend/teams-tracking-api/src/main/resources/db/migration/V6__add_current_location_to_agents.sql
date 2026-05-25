ALTER TABLE agents ADD COLUMN current_latitude DECIMAL(9, 7);
ALTER TABLE agents ADD COLUMN current_longitude DECIMAL(10, 7);
ALTER TABLE agents ADD COLUMN current_address VARCHAR(500);
ALTER TABLE agents ADD COLUMN current_accuracy DECIMAL(10, 2);
ALTER TABLE agents ADD COLUMN current_speed DECIMAL(10, 2);

CREATE INDEX idx_agents_active_status ON agents (active, status);
