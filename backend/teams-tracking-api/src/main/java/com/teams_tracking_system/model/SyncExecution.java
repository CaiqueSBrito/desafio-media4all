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
@Table(name = "sync_executions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SyncExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_type", nullable = false, length = 40)
    private SyncType syncType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SyncExecutionStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "duration_millis")
    private Long durationMillis;

    @Column(name = "records_read", nullable = false)
    private int recordsRead;

    @Column(name = "records_created", nullable = false)
    private int recordsCreated;

    @Column(name = "records_updated", nullable = false)
    private int recordsUpdated;

    @Column(name = "records_ignored", nullable = false)
    private int recordsIgnored;

    @Column(name = "records_failed", nullable = false)
    private int recordsFailed;

    @Column(name = "retry_attempts", nullable = false)
    private int retryAttempts;

    @Column(name = "rate_limit_errors", nullable = false)
    private int rateLimitErrors;

    @Column(name = "service_unavailable_errors", nullable = false)
    private int serviceUnavailableErrors;

    @Column(name = "cursor_value_before", length = 500)
    private String cursorValueBefore;

    @Column(name = "cursor_value_after", length = 500)
    private String cursorValueAfter;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SyncExecution(
            SyncType syncType,
            SyncExecutionStatus status,
            Instant startedAt,
            String cursorValueBefore,
            Instant createdAt,
            Instant updatedAt) {
        this.syncType = syncType;
        this.status = status;
        this.startedAt = startedAt;
        this.cursorValueBefore = cursorValueBefore;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void updateCounters(
            int recordsRead,
            int recordsCreated,
            int recordsUpdated,
            int recordsIgnored,
            int recordsFailed,
            int retryAttempts,
            int rateLimitErrors,
            int serviceUnavailableErrors,
            Instant updatedAt) {
        this.recordsRead = recordsRead;
        this.recordsCreated = recordsCreated;
        this.recordsUpdated = recordsUpdated;
        this.recordsIgnored = recordsIgnored;
        this.recordsFailed = recordsFailed;
        this.retryAttempts = retryAttempts;
        this.rateLimitErrors = rateLimitErrors;
        this.serviceUnavailableErrors = serviceUnavailableErrors;
        this.updatedAt = updatedAt;
    }

    public void finishSuccessfully(
            Instant finishedAt,
            Long durationMillis,
            String cursorValueAfter,
            Instant updatedAt) {
        this.status = SyncExecutionStatus.SUCCESS;
        this.finishedAt = finishedAt;
        this.durationMillis = durationMillis;
        this.cursorValueAfter = cursorValueAfter;
        this.updatedAt = updatedAt;
    }

    public void finishWithWarning(
            Instant finishedAt,
            Long durationMillis,
            String cursorValueAfter,
            String errorMessage,
            Instant updatedAt) {
        this.status = SyncExecutionStatus.WARNING;
        this.finishedAt = finishedAt;
        this.durationMillis = durationMillis;
        this.cursorValueAfter = cursorValueAfter;
        this.errorMessage = errorMessage;
        this.updatedAt = updatedAt;
    }

    public void fail(
            Instant finishedAt,
            Long durationMillis,
            String errorMessage,
            Instant updatedAt) {
        this.status = SyncExecutionStatus.FAILED;
        this.finishedAt = finishedAt;
        this.durationMillis = durationMillis;
        this.errorMessage = errorMessage;
        this.updatedAt = updatedAt;
    }
}
