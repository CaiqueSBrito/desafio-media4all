package com.teams_tracking_system.service;

import com.teams_tracking_system.dtos.schemas.GeofenceSyncResultResponse;
import com.teams_tracking_system.integrations.ExternalGpsApiClient;
import com.teams_tracking_system.integrations.ExternalGpsRetryStats;
import com.teams_tracking_system.integrations.dtos.ExternalGeofenceResponse;
import com.teams_tracking_system.model.Geofence;
import com.teams_tracking_system.model.GeofenceType;
import com.teams_tracking_system.model.SyncType;
import com.teams_tracking_system.repositories.GeofenceRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GeofenceSyncService {

    private final ExternalGpsApiClient externalGpsApiClient;
    private final GeofenceRepository geofenceRepository;
    private final SyncExecutionService syncExecutionService;
    private final SyncFailureService syncFailureService;
    private final SyncCursorService syncCursorService;
    private final SyncConflictService syncConflictService;

    public GeofenceSyncService(
            ExternalGpsApiClient externalGpsApiClient,
            GeofenceRepository geofenceRepository,
            SyncExecutionService syncExecutionService,
            SyncFailureService syncFailureService,
            SyncCursorService syncCursorService,
            SyncConflictService syncConflictService) {
        this.externalGpsApiClient = externalGpsApiClient;
        this.geofenceRepository = geofenceRepository;
        this.syncExecutionService = syncExecutionService;
        this.syncFailureService = syncFailureService;
        this.syncCursorService = syncCursorService;
        this.syncConflictService = syncConflictService;
    }

    @Transactional
    public GeofenceSyncResultResponse syncGeofences() {
        String previousCursor = syncCursorService.getLastCursorValue(SyncType.GEOFENCES);
        Long executionId = syncExecutionService.start(SyncType.GEOFENCES, previousCursor);

        try {
            GeofenceSyncOutcome outcome = executeSyncGeofences(executionId, previousCursor);
            syncCursorService.upsertAfterSuccessfulSync(
                    SyncType.GEOFENCES,
                    outcome.cursorValueAfter(),
                    null,
                    outcome.lastOccurredAt(),
                    Instant.now());

            ExternalGpsRetryStats retryStats = resolveRetryStats();
            finishExecution(executionId, outcome.result(), retryStats, outcome.cursorValueAfter());
            return outcome.result();
        } catch (RuntimeException exception) {
            ExternalGpsRetryStats retryStats = resolveRetryStats();
            syncExecutionService.fail(
                    executionId,
                    exception,
                    retryStats.getRetryAttempts(),
                    retryStats.getRateLimitErrors(),
                    retryStats.getServiceUnavailableErrors());
            throw exception;
        }
    }

    private GeofenceSyncOutcome executeSyncGeofences(Long executionId, String previousCursor) {
        var response = externalGpsApiClient.findGeofences();
        if (response == null || response.data() == null) {
            return new GeofenceSyncOutcome(new GeofenceSyncResultResponse(0, 0, 0, 0), previousCursor, null);
        }

        int created = 0;
        int updated = 0;
        int skipped = 0;
        Instant checkpoint = null;
        Set<String> seenIds = new HashSet<>();
        Set<String> seenExternalIds = new HashSet<>();

        for (ExternalGeofenceResponse externalGeofence : response.data()) {
            String invalidReason = validate(externalGeofence);
            if (invalidReason != null) {
                recordInvalidPayload(executionId, invalidReason, externalGeofence);
                skipped++;
                continue;
            }

            String duplicateReason = detectDuplicateInPayload(externalGeofence, seenIds, seenExternalIds);
            if (duplicateReason != null) {
                recordInvalidPayload(executionId, duplicateReason, externalGeofence);
                skipped++;
                continue;
            }

            var existingGeofence = geofenceRepository.findByExternalId(externalGeofence.externalId());
            if (existingGeofence.isPresent()) {
                updateGeofence(existingGeofence.get(), externalGeofence);
                updated++;
                checkpoint = max(checkpoint, externalGeofence.syncedAt());
            } else if (geofenceRepository.findById(externalGeofence.id()).isPresent()) {
                recordInvalidPayload(
                        executionId,
                        "geofence.id already exists with another externalId: " + externalGeofence.id(),
                        externalGeofence);
                recordGeofenceIdConflict(executionId, externalGeofence);
                skipped++;
            } else {
                createGeofence(externalGeofence);
                created++;
                checkpoint = max(checkpoint, externalGeofence.syncedAt());
            }
        }

        String cursorValueAfter = checkpoint != null ? checkpoint.toString() : previousCursor;
        return new GeofenceSyncOutcome(
                new GeofenceSyncResultResponse(response.data().size(), created, updated, skipped),
                cursorValueAfter,
                checkpoint);
    }

    private void finishExecution(
            Long executionId,
            GeofenceSyncResultResponse result,
            ExternalGpsRetryStats retryStats,
            String cursorValueAfter) {
        if (result.skipped() > 0) {
            syncExecutionService.finishWithWarning(
                    executionId,
                    result.read(),
                    result.created(),
                    result.updated(),
                    0,
                    result.skipped(),
                    retryStats.getRetryAttempts(),
                    retryStats.getRateLimitErrors(),
                    retryStats.getServiceUnavailableErrors(),
                    cursorValueAfter,
                    "Geofence sync finished with " + result.skipped() + " invalid payload(s).");
            return;
        }

        syncExecutionService.finishSuccessfully(
                executionId,
                result.read(),
                result.created(),
                result.updated(),
                0,
                0,
                retryStats.getRetryAttempts(),
                retryStats.getRateLimitErrors(),
                retryStats.getServiceUnavailableErrors(),
                cursorValueAfter);
    }

    private void createGeofence(ExternalGeofenceResponse externalGeofence) {
        Geofence geofence = new Geofence(
                externalGeofence.id(),
                externalGeofence.externalId(),
                externalGeofence.name(),
                GeofenceType.valueOf(externalGeofence.type().name()),
                externalGeofence.coordinatesJson(),
                externalGeofence.alertOnEnter(),
                externalGeofence.alertOnExit(),
                externalGeofence.assignedTeams(),
                externalGeofence.syncedAt());

        geofenceRepository.save(geofence);
    }

    private void updateGeofence(Geofence geofence, ExternalGeofenceResponse externalGeofence) {
        geofence.updateFromExternalData(
                externalGeofence.externalId(),
                externalGeofence.name(),
                GeofenceType.valueOf(externalGeofence.type().name()),
                externalGeofence.coordinatesJson(),
                externalGeofence.alertOnEnter(),
                externalGeofence.alertOnExit(),
                externalGeofence.assignedTeams(),
                externalGeofence.syncedAt());

        geofenceRepository.save(geofence);
    }

    private String validate(ExternalGeofenceResponse externalGeofence) {
        if (externalGeofence == null) {
            return "geofence payload is null";
        }
        if (externalGeofence.id() == null || externalGeofence.id().isBlank()) {
            return "geofence.id is required";
        }
        if (externalGeofence.externalId() == null || externalGeofence.externalId().isBlank()) {
            return "geofence.externalId is required";
        }
        if (externalGeofence.name() == null || externalGeofence.name().isBlank()) {
            return "geofence.name is required";
        }
        if (externalGeofence.type() == null) {
            return "geofence.type is required";
        }
        if (externalGeofence.coordinatesJson() == null || externalGeofence.coordinatesJson().isBlank()) {
            return "geofence.coordinatesJson is required";
        }
        if (externalGeofence.alertOnEnter() == null) {
            return "geofence.alertOnEnter is required";
        }
        if (externalGeofence.alertOnExit() == null) {
            return "geofence.alertOnExit is required";
        }
        if (externalGeofence.syncedAt() == null) {
            return "geofence.syncedAt is required";
        }
        return null;
    }

    private String detectDuplicateInPayload(
            ExternalGeofenceResponse externalGeofence,
            Set<String> seenIds,
            Set<String> seenExternalIds) {
        if (seenIds.contains(externalGeofence.id())) {
            return "duplicate geofence.id in external payload: " + externalGeofence.id();
        }
        if (seenExternalIds.contains(externalGeofence.externalId())) {
            return "duplicate geofence.externalId in external payload: " + externalGeofence.externalId();
        }

        seenIds.add(externalGeofence.id());
        seenExternalIds.add(externalGeofence.externalId());
        return null;
    }

    private void recordInvalidPayload(
            Long executionId,
            String reason,
            ExternalGeofenceResponse externalGeofence) {
        syncFailureService.recordInvalidPayload(
                executionId,
                SyncType.GEOFENCES,
                "geofence",
                reason,
                externalGeofence);
    }

    private void recordGeofenceIdConflict(Long executionId, ExternalGeofenceResponse externalGeofence) {
        syncConflictService.recordConflict(
                executionId,
                SyncType.GEOFENCES,
                "geofence",
                externalGeofence.id(),
                "External geofence id conflicts with an existing local geofence using another externalId.",
                Map.of("id", externalGeofence.id()),
                externalGeofence);
    }

    private ExternalGpsRetryStats resolveRetryStats() {
        ExternalGpsRetryStats retryStats = externalGpsApiClient.getLastRetryStats();
        return retryStats != null ? retryStats : new ExternalGpsRetryStats();
    }

    private Instant max(Instant current, Instant candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null || candidate.isAfter(current)) {
            return candidate;
        }
        return current;
    }

    private record GeofenceSyncOutcome(
            GeofenceSyncResultResponse result,
            String cursorValueAfter,
            Instant lastOccurredAt) {
    }
}
