package com.teams_tracking_system.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "geofences")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Geofence {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private GeofenceType type;

    @Column(name = "coordinates_json")
    private String coordinatesJson;

    @Column(name = "alert_on_enter")
    private boolean alertOnEnter;

    @Column(name = "alert_on_exit")
    private boolean alertOnExit;

    @Column(name = "assigned_teams")
    private String assignedTeams;

    @Column(name = "synced_at")
    private Instant syncedAt;

    public Geofence(
            String id,
            String externalId,
            String name,
            GeofenceType type,
            String coordinatesJson,
            boolean alertOnEnter,
            boolean alertOnExit,
            String assignedTeams,
            Instant syncedAt) {
        this.id = id;
        this.externalId = externalId;
        this.name = name;
        this.type = type;
        this.coordinatesJson = coordinatesJson;
        this.alertOnEnter = alertOnEnter;
        this.alertOnExit = alertOnExit;
        this.assignedTeams = assignedTeams;
        this.syncedAt = syncedAt;
    }

    public void updateFromExternalData(
            String externalId,
            String name,
            GeofenceType type,
            String coordinatesJson,
            boolean alertOnEnter,
            boolean alertOnExit,
            String assignedTeams,
            Instant syncedAt) {
        this.externalId = externalId;
        this.name = name;
        this.type = type;
        this.coordinatesJson = coordinatesJson;
        this.alertOnEnter = alertOnEnter;
        this.alertOnExit = alertOnExit;
        this.assignedTeams = assignedTeams;
        this.syncedAt = syncedAt;
    }
}
