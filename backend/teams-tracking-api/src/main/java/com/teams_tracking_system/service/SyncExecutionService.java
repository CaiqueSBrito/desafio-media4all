package com.teams_tracking_system.service;

import com.teams_tracking_system.model.SyncExecution;
import com.teams_tracking_system.model.SyncExecutionStatus;
import com.teams_tracking_system.model.SyncType;
import com.teams_tracking_system.repositories.SyncExecutionRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncExecutionService {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

    private final SyncExecutionRepository syncExecutionRepository;

    public SyncExecutionService(SyncExecutionRepository syncExecutionRepository) {
        this.syncExecutionRepository = syncExecutionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long start(SyncType syncType, String cursorValueBefore) {
        Instant now = Instant.now();
        SyncExecution syncExecution = new SyncExecution(
                syncType,
                SyncExecutionStatus.RUNNING,
                now,
                cursorValueBefore,
                now,
                now);

        return syncExecutionRepository.save(syncExecution).getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finishSuccessfully(
            Long executionId,
            int recordsRead,
            int recordsCreated,
            int recordsUpdated,
            int recordsIgnored,
            int recordsFailed,
            int retryAttempts,
            int rateLimitErrors,
            int serviceUnavailableErrors,
            String cursorValueAfter) {
        SyncExecution syncExecution = findById(executionId);
        Instant now = Instant.now();

        syncExecution.updateCounters(
                recordsRead,
                recordsCreated,
                recordsUpdated,
                recordsIgnored,
                recordsFailed,
                retryAttempts,
                rateLimitErrors,
                serviceUnavailableErrors,
                now);
        syncExecution.finishSuccessfully(
                now,
                durationMillis(syncExecution.getStartedAt(), now),
                cursorValueAfter,
                now);

        syncExecutionRepository.save(syncExecution);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finishWithWarning(
            Long executionId,
            int recordsRead,
            int recordsCreated,
            int recordsUpdated,
            int recordsIgnored,
            int recordsFailed,
            int retryAttempts,
            int rateLimitErrors,
            int serviceUnavailableErrors,
            String cursorValueAfter,
            String warningMessage) {
        SyncExecution syncExecution = findById(executionId);
        Instant now = Instant.now();

        syncExecution.updateCounters(
                recordsRead,
                recordsCreated,
                recordsUpdated,
                recordsIgnored,
                recordsFailed,
                retryAttempts,
                rateLimitErrors,
                serviceUnavailableErrors,
                now);
        syncExecution.finishWithWarning(
                now,
                durationMillis(syncExecution.getStartedAt(), now),
                cursorValueAfter,
                truncate(warningMessage),
                now);

        syncExecutionRepository.save(syncExecution);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long executionId, RuntimeException exception) {
        fail(executionId, exception, 0, 0, 0);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(
            Long executionId,
            RuntimeException exception,
            int retryAttempts,
            int rateLimitErrors,
            int serviceUnavailableErrors) {
        SyncExecution syncExecution = findById(executionId);
        Instant now = Instant.now();
        syncExecution.updateCounters(
                syncExecution.getRecordsRead(),
                syncExecution.getRecordsCreated(),
                syncExecution.getRecordsUpdated(),
                syncExecution.getRecordsIgnored(),
                syncExecution.getRecordsFailed(),
                retryAttempts,
                rateLimitErrors,
                serviceUnavailableErrors,
                now);
        syncExecution.fail(
                now,
                durationMillis(syncExecution.getStartedAt(), now),
                truncate(exception.getMessage()),
                now);

        syncExecutionRepository.save(syncExecution);
    }

    private SyncExecution findById(Long executionId) {
        return syncExecutionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalStateException("Sync execution not found: " + executionId));
    }

    private Long durationMillis(Instant startedAt, Instant finishedAt) {
        return Duration.between(startedAt, finishedAt).toMillis();
    }

    private String truncate(String message) {
        if (message == null || message.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}
