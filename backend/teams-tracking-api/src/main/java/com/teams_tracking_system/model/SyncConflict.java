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
@Table(name = "sync_conflicts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SyncConflict {

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

    @Column(name = "conflict_key", nullable = false, length = 200)
    private String conflictKey;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "local_snapshot_json", columnDefinition = "TEXT")
    private String localSnapshotJson;

    @Column(name = "external_payload_json", columnDefinition = "TEXT")
    private String externalPayloadJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public SyncConflict(
            SyncExecution syncExecution,
            SyncType syncType,
            String entityType,
            String conflictKey,
            String reason,
            String localSnapshotJson,
            String externalPayloadJson,
            Instant createdAt) {
        this.syncExecution = syncExecution;
        this.syncType = syncType;
        this.entityType = entityType;
        this.conflictKey = conflictKey;
        this.reason = reason;
        this.localSnapshotJson = localSnapshotJson;
        this.externalPayloadJson = externalPayloadJson;
        this.createdAt = createdAt;
    }
}
