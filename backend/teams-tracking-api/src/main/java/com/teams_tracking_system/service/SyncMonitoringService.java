package com.teams_tracking_system.service;

import com.teams_tracking_system.dtos.schemas.SyncCursorMonitoringResponse;
import com.teams_tracking_system.dtos.schemas.SyncExecutionMonitoringResponse;
import com.teams_tracking_system.dtos.schemas.SyncFailureMonitoringResponse;
import com.teams_tracking_system.dtos.schemas.SyncMonitoringResponse;
import com.teams_tracking_system.dtos.schemas.SyncMonitoringTotalsResponse;
import com.teams_tracking_system.dtos.schemas.SyncSchedulerStatusResponse;
import com.teams_tracking_system.model.SyncCursor;
import com.teams_tracking_system.model.SyncExecution;
import com.teams_tracking_system.model.SyncFailure;
import com.teams_tracking_system.model.SyncType;
import com.teams_tracking_system.repositories.SyncCursorRepository;
import com.teams_tracking_system.repositories.SyncExecutionRepository;
import com.teams_tracking_system.repositories.SyncFailureRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncMonitoringService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final List<SyncType> SCHEDULER_SYNC_TYPES = List.of(
            SyncType.AGENTS,
            SyncType.LOCATIONS,
            SyncType.CHECK_INS,
            SyncType.GEOFENCES);

    private final SyncExecutionRepository syncExecutionRepository;
    private final SyncFailureRepository syncFailureRepository;
    private final SyncCursorRepository syncCursorRepository;

    public SyncMonitoringService(
            SyncExecutionRepository syncExecutionRepository,
            SyncFailureRepository syncFailureRepository,
            SyncCursorRepository syncCursorRepository) {
        this.syncExecutionRepository = syncExecutionRepository;
        this.syncFailureRepository = syncFailureRepository;
        this.syncCursorRepository = syncCursorRepository;
    }

    @Transactional(readOnly = true)
    public SyncMonitoringResponse getOverview(Integer executionLimit, Integer failureLimit) {
        List<SyncExecution> allExecutions = syncExecutionRepository.findAll();
        List<SyncCursor> allCursors = syncCursorRepository.findAllByOrderBySyncTypeAsc();
        Map<SyncType, SyncCursor> cursorsByType = allCursors.stream()
                .collect(Collectors.toMap(SyncCursor::getSyncType, Function.identity()));

        List<SyncExecutionMonitoringResponse> latestExecutions = syncExecutionRepository
                .findAllByOrderByStartedAtDesc(PageRequest.of(0, sanitizeLimit(executionLimit)))
                .stream()
                .map(this::toExecutionResponse)
                .toList();

        List<SyncFailureMonitoringResponse> recentFailures = syncFailureRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, sanitizeLimit(failureLimit)))
                .stream()
                .map(this::toFailureResponse)
                .toList();

        return new SyncMonitoringResponse(
                Instant.now(),
                toTotalsResponse(allExecutions, syncFailureRepository.count()),
                toSchedulerStatuses(cursorsByType),
                latestExecutions,
                recentFailures,
                allCursors.stream().map(this::toCursorResponse).toList());
    }

    public SyncMonitoringResponse getOverview() {
        return getOverview(DEFAULT_LIMIT, DEFAULT_LIMIT);
    }

    private List<SyncSchedulerStatusResponse> toSchedulerStatuses(Map<SyncType, SyncCursor> cursorsByType) {
        return SCHEDULER_SYNC_TYPES.stream()
                .map(syncType -> toSchedulerStatus(syncType, cursorsByType.get(syncType)))
                .toList();
    }

    private SyncSchedulerStatusResponse toSchedulerStatus(SyncType syncType, SyncCursor cursor) {
        SyncExecution latestExecution = syncExecutionRepository
                .findFirstBySyncTypeOrderByStartedAtDesc(syncType)
                .orElse(null);

        if (latestExecution == null) {
            return new SyncSchedulerStatusResponse(
                    syncType,
                    null,
                    null,
                    null,
                    cursor != null ? cursor.getLastCursorValue() : null,
                    cursor != null ? cursor.getLastSuccessfulSyncAt() : null,
                    0,
                    0,
                    0,
                    null);
        }

        return new SyncSchedulerStatusResponse(
                syncType,
                latestExecution.getStatus(),
                latestExecution.getStartedAt(),
                latestExecution.getFinishedAt(),
                cursor != null ? cursor.getLastCursorValue() : latestExecution.getCursorValueAfter(),
                cursor != null ? cursor.getLastSuccessfulSyncAt() : null,
                latestExecution.getRetryAttempts(),
                latestExecution.getRateLimitErrors(),
                latestExecution.getServiceUnavailableErrors(),
                latestExecution.getErrorMessage());
    }

    private SyncMonitoringTotalsResponse toTotalsResponse(
            List<SyncExecution> executions,
            long failures) {
        return new SyncMonitoringTotalsResponse(
                executions.size(),
                failures,
                sum(executions, SyncExecution::getRecordsRead),
                sum(executions, SyncExecution::getRecordsCreated),
                sum(executions, SyncExecution::getRecordsUpdated),
                sum(executions, SyncExecution::getRecordsIgnored),
                sum(executions, SyncExecution::getRecordsFailed),
                sum(executions, SyncExecution::getRetryAttempts),
                sum(executions, SyncExecution::getRateLimitErrors),
                sum(executions, SyncExecution::getServiceUnavailableErrors));
    }

    private long sum(List<SyncExecution> executions, java.util.function.ToIntFunction<SyncExecution> mapper) {
        return executions.stream().mapToInt(mapper).sum();
    }

    private SyncExecutionMonitoringResponse toExecutionResponse(SyncExecution execution) {
        return new SyncExecutionMonitoringResponse(
                execution.getId(),
                execution.getSyncType(),
                execution.getStatus(),
                execution.getStartedAt(),
                execution.getFinishedAt(),
                execution.getDurationMillis(),
                execution.getRecordsRead(),
                execution.getRecordsCreated(),
                execution.getRecordsUpdated(),
                execution.getRecordsIgnored(),
                execution.getRecordsFailed(),
                execution.getRetryAttempts(),
                execution.getRateLimitErrors(),
                execution.getServiceUnavailableErrors(),
                execution.getCursorValueBefore(),
                execution.getCursorValueAfter(),
                execution.getErrorMessage());
    }

    private SyncFailureMonitoringResponse toFailureResponse(SyncFailure failure) {
        return new SyncFailureMonitoringResponse(
                failure.getId(),
                failure.getSyncExecution().getId(),
                failure.getSyncType(),
                failure.getEntityType(),
                failure.getReason(),
                failure.getCreatedAt());
    }

    private SyncCursorMonitoringResponse toCursorResponse(SyncCursor cursor) {
        return new SyncCursorMonitoringResponse(
                cursor.getSyncType(),
                cursor.getLastCursorValue(),
                cursor.getLastPage(),
                cursor.getLastOccurredAt(),
                cursor.getLastSyncedAt(),
                cursor.getLastSuccessfulSyncAt(),
                cursor.getUpdatedAt());
    }

    private int sanitizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }
}
