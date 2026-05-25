CREATE TABLE geofences (
    id VARCHAR(100) NOT NULL,
    external_id VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    type VARCHAR(40) NOT NULL,
    coordinates_json TEXT NOT NULL,
    alert_on_enter BOOLEAN NOT NULL,
    alert_on_exit BOOLEAN NOT NULL,
    assigned_teams VARCHAR(1000),
    synced_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_geofences PRIMARY KEY (id),
    CONSTRAINT uk_geofences_external_id UNIQUE (external_id)
);

CREATE INDEX idx_geofences_type ON geofences (type);
CREATE INDEX idx_geofences_synced_at ON geofences (synced_at);
