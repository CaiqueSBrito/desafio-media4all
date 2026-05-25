package com.teams_tracking_system.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "sync_cursors")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SyncCursor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_type", nullable = false, unique = true, length = 40)
    private SyncType syncType;

    @Column(name = "last_cursor_value", length = 500)
    private String lastCursorValue;

    @Column(name = "last_page")
    private Integer lastPage;

    @Column(name = "last_occurred_at")
    private Instant lastOccurredAt;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "last_successful_sync_at")
    private Instant lastSuccessfulSyncAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SyncCursor(
            SyncType syncType,
            String lastCursorValue,
            Integer lastPage,
            Instant lastOccurredAt,
            Instant lastSyncedAt,
            Instant lastSuccessfulSyncAt,
            Instant createdAt,
            Instant updatedAt) {
        this.syncType = syncType;
        this.lastCursorValue = lastCursorValue;
        this.lastPage = lastPage;
        this.lastOccurredAt = lastOccurredAt;
        this.lastSyncedAt = lastSyncedAt;
        this.lastSuccessfulSyncAt = lastSuccessfulSyncAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void updateAfterSuccessfulSync(
            String lastCursorValue,
            Integer lastPage,
            Instant lastOccurredAt,
            Instant lastSyncedAt,
            Instant lastSuccessfulSyncAt,
            Instant updatedAt) {
        this.lastCursorValue = lastCursorValue;
        this.lastPage = lastPage;
        this.lastOccurredAt = lastOccurredAt;
        this.lastSyncedAt = lastSyncedAt;
        this.lastSuccessfulSyncAt = lastSuccessfulSyncAt;
        this.updatedAt = updatedAt;
    }
}
