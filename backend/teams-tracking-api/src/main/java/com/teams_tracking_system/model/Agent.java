package com.teams_tracking_system.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "agents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Agent {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private AgentRole role;

    @Column(name = "team")
    private String team;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "active")
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AgentStatus status;

    @Column(name = "battery")
    private Integer battery;

    @Column(name = "last_seen")
    private Instant lastSeen;

    @Column(name = "current_latitude")
    private BigDecimal currentLatitude;

    @Column(name = "current_longitude")
    private BigDecimal currentLongitude;

    @Column(name = "current_address")
    private String currentAddress;

    @Column(name = "current_accuracy")
    private BigDecimal currentAccuracy;

    @Column(name = "current_speed")
    private BigDecimal currentSpeed;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Agent(
            String id,
            String externalId,
            String name,
            AgentRole role,
            String team,
            String phone,
            String email,
            boolean active,
            AgentStatus status,
            Integer battery,
            Instant lastSeen,
            BigDecimal currentLatitude,
            BigDecimal currentLongitude,
            String currentAddress,
            BigDecimal currentAccuracy,
            BigDecimal currentSpeed,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.externalId = externalId;
        this.name = name;
        this.role = role;
        this.team = team;
        this.phone = phone;
        this.email = email;
        this.active = active;
        this.status = status;
        this.battery = battery;
        this.lastSeen = lastSeen;
        this.currentLatitude = currentLatitude;
        this.currentLongitude = currentLongitude;
        this.currentAddress = currentAddress;
        this.currentAccuracy = currentAccuracy;
        this.currentSpeed = currentSpeed;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void updateFromExternalData(
            String externalId,
            String name,
            AgentRole role,
            String team,
            String phone,
            String email,
            boolean active,
            AgentStatus status,
            Integer battery,
            Instant lastSeen,
            Instant updatedAt) {
        this.externalId = externalId;
        this.name = name;
        this.role = role;
        this.team = team;
        this.phone = phone;
        this.email = email;
        this.active = active;
        this.status = status;
        this.battery = battery;
        this.lastSeen = lastSeen;
        this.updatedAt = updatedAt;
    }

    public void updateFromRequest(
            String name,
            AgentRole role,
            String team,
            String phone,
            String email,
            Boolean active,
            AgentStatus status,
            Integer battery,
            Instant lastSeen,
            Instant updatedAt) {
        if (name != null) {
            this.name = name;
        }
        if (role != null) {
            this.role = role;
        }
        if (team != null) {
            this.team = team;
        }
        if (phone != null) {
            this.phone = phone;
        }
        if (email != null) {
            this.email = email;
        }
        if (active != null) {
            this.active = active;
        }
        if (status != null) {
            this.status = status;
        }
        if (battery != null) {
            this.battery = battery;
        }
        if (lastSeen != null) {
            this.lastSeen = lastSeen;
        }
        this.updatedAt = updatedAt;
    }

    public void deactivate(Instant updatedAt) {
        this.active = false;
        this.status = AgentStatus.OFFLINE;
        this.updatedAt = updatedAt;
    }

    public void updateCurrentLocation(
            BigDecimal currentLatitude,
            BigDecimal currentLongitude,
            String currentAddress,
            BigDecimal currentAccuracy,
            BigDecimal currentSpeed,
            Integer battery,
            AgentStatus status,
            Instant lastSeen,
            Instant updatedAt) {
        this.currentLatitude = currentLatitude;
        this.currentLongitude = currentLongitude;
        this.currentAddress = currentAddress;
        this.currentAccuracy = currentAccuracy;
        this.currentSpeed = currentSpeed;
        this.battery = battery;
        this.status = status;
        this.lastSeen = lastSeen;
        this.updatedAt = updatedAt;
    }
}
