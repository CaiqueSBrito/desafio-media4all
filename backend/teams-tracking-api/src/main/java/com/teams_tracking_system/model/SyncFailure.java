package com.teams_tracking_system.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "sync_failures")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SyncFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sync_execution_id", nullable = false)
    private SyncExecution syncExecution;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_type", nullable = false, length = 40)
    private SyncType syncType;

    @Column(name = "entity_type", nullable = false, length = 80)
    private String entityType;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public SyncFailure(
            SyncExecution syncExecution,
            SyncType syncType,
            String entityType,
            String reason,
            String payloadJson,
            Instant createdAt) {
        this.syncExecution = syncExecution;
        this.syncType = syncType;
        this.entityType = entityType;
        this.reason = reason;
        this.payloadJson = payloadJson;
        this.createdAt = createdAt;
    }
}
