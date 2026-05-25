package com.teams_tracking_system.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "check_ins")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CheckIn {

    @Id
    @Column(nullable = false, length = 100)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CheckInType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SyncSource source;

    @Column(precision = 9, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(length = 500)
    private String address;

    @Column(precision = 10, scale = 2)
    private BigDecimal accuracy;

    @Column(precision = 10, scale = 2)
    private BigDecimal speed;

    @Column(length = 1000)
    private String notes;

    @Column(name = "distance_from_previous", precision = 12, scale = 2)
    private BigDecimal distanceFromPrevious;

    @Column(name = "external_event_id", length = 100)
    private String externalEventId;

    @Column(name = "manual_idempotency_key", length = 100)
    private String manualIdempotencyKey;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CheckIn(
            String id,
            Agent agent,
            CheckInType type,
            SyncSource source,
            BigDecimal latitude,
            BigDecimal longitude,
            String address,
            BigDecimal accuracy,
            BigDecimal speed,
            String notes,
            BigDecimal distanceFromPrevious,
            String externalEventId,
            String manualIdempotencyKey,
            Instant occurredAt,
            Instant syncedAt,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.agent = agent;
        this.type = type;
        this.source = source;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.accuracy = accuracy;
        this.speed = speed;
        this.notes = notes;
        this.distanceFromPrevious = distanceFromPrevious;
        this.externalEventId = externalEventId;
        this.manualIdempotencyKey = manualIdempotencyKey;
        this.occurredAt = occurredAt;
        this.syncedAt = syncedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void updateFromExternalEvent(
            CheckInType type,
            SyncSource source,
            BigDecimal latitude,
            BigDecimal longitude,
            String address,
            BigDecimal accuracy,
            BigDecimal speed,
            String notes,
            BigDecimal distanceFromPrevious,
            Instant occurredAt,
            Instant syncedAt,
            Instant updatedAt) {
        this.type = type;
        this.source = source;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.accuracy = accuracy;
        this.speed = speed;
        this.notes = notes;
        this.distanceFromPrevious = distanceFromPrevious;
        this.occurredAt = occurredAt;
        this.syncedAt = syncedAt;
        this.updatedAt = updatedAt;
    }
}
