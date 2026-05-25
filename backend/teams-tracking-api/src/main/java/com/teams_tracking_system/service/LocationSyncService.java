package com.teams_tracking_system.service;

import com.teams_tracking_system.dtos.schemas.LocationSyncResultResponse;
import com.teams_tracking_system.integrations.ExternalGpsApiClient;
import com.teams_tracking_system.integrations.ExternalGpsRetryStats;
import com.teams_tracking_system.integrations.dtos.ExternalLocationResponse;
import com.teams_tracking_system.model.Agent;
import com.teams_tracking_system.model.AgentPosition;
import com.teams_tracking_system.model.AgentStatus;
import com.teams_tracking_system.model.SyncType;
import com.teams_tracking_system.repositories.AgentPositionRepository;
import com.teams_tracking_system.repositories.AgentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class LocationSyncService {

    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int COORDINATE_SCALE = 7;

    private final ExternalGpsApiClient externalGpsApiClient;
    private final AgentRepository agentRepository;
    private final AgentPositionRepository agentPositionRepository;
    private final AgentPositionPersistenceService agentPositionPersistenceService;
    private final SyncExecutionService syncExecutionService;
    private final SyncFailureService syncFailureService;
    private final SyncCursorService syncCursorService;
    private final SyncConflictService syncConflictService;

    @Value("${sync.locations.max-accuracy-meters:50}")
    private BigDecimal maxAccuracyMeters = new BigDecimal("50");

    public LocationSyncService(
            ExternalGpsApiClient externalGpsApiClient,
            AgentRepository agentRepository,
            AgentPositionRepository agentPositionRepository,
            AgentPositionPersistenceService agentPositionPersistenceService,
            SyncExecutionService syncExecutionService,
            SyncFailureService syncFailureService,
            SyncCursorService syncCursorService,
            SyncConflictService syncConflictService) {
        this.externalGpsApiClient = externalGpsApiClient;
        this.agentRepository = agentRepository;
        this.agentPositionRepository = agentPositionRepository;
        this.agentPositionPersistenceService = agentPositionPersistenceService;
        this.syncExecutionService = syncExecutionService;
        this.syncFailureService = syncFailureService;
        this.syncCursorService = syncCursorService;
        this.syncConflictService = syncConflictService;
    }

    @Transactional
    public LocationSyncResultResponse syncLocations(Boolean onlineOnly) {
        String previousCursor = syncCursorService.getLastCursorValue(SyncType.LOCATIONS);
        Long executionId = syncExecutionService.start(SyncType.LOCATIONS, previousCursor);

        try {
            LocationSyncOutcome outcome = executeSyncLocations(onlineOnly, executionId, previousCursor);
            syncCursorService.upsertAfterSuccessfulSync(
                    SyncType.LOCATIONS,
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

    private LocationSyncOutcome executeSyncLocations(
            Boolean onlineOnly,
            Long executionId,
            String previousCursor) {
        var response = externalGpsApiClient.findLocations(onlineOnly);
        if (response == null || response.data() == null) {
            return new LocationSyncOutcome(
                    new LocationSyncResultResponse(0, 0, 0, 0, 0),
                    previousCursor,
                    null);
        }

        int positionsCreated = 0;
        int agentsUpdated = 0;
        int ignored = 0;
        int skipped = 0;
        Instant lastOccurredAt = null;
        Set<PositionSnapshotKey> seenPositionKeys = new HashSet<>();

        for (ExternalLocationResponse externalLocation : response.data()) {
            String invalidReason = validate(externalLocation);
            if (invalidReason != null) {
                recordInvalidPayload(executionId, invalidReason, externalLocation);
                skipped++;
                continue;
            }

            Agent agent = findAgent(externalLocation);
            if (agent == null) {
                recordInvalidPayload(
                        executionId,
                        "location agent not found: " + externalLocation.agentId(),
                        externalLocation);
                skipped++;
                continue;
            }

            if (isDuplicatePosition(agent.getId(), externalLocation, seenPositionKeys)) {
                ignored++;
            } else {
                if (persistPosition(agent, externalLocation)) {
                    positionsCreated++;
                } else {
                    ignored++;
                }
            }

            if (isDelayedEvent(agent, externalLocation)) {
                recordLocationConflict(
                        executionId,
                        externalLocation,
                        agent,
                        "Delayed GPS event kept in history but ignored as current location.");
            } else if (hasSameTimestampPositionConflict(agent, externalLocation)) {
                recordLocationConflict(
                        executionId,
                        externalLocation,
                        agent,
                        "GPS event has same timestamp as current location but different coordinates.");
            }

            if (shouldUpdateCurrentLocation(agent, externalLocation.lastSeen())) {
                updateAgentCurrentLocation(agent, externalLocation);
                agentsUpdated++;
            }

            lastOccurredAt = max(lastOccurredAt, externalLocation.lastSeen());
        }

        String cursorValueAfter = lastOccurredAt != null ? lastOccurredAt.toString() : previousCursor;
        return new LocationSyncOutcome(
                new LocationSyncResultResponse(
                        response.data().size(),
                        positionsCreated,
                        agentsUpdated,
                        ignored,
                        skipped),
                cursorValueAfter,
                lastOccurredAt);
    }

    private void finishExecution(
            Long executionId,
            LocationSyncResultResponse result,
            ExternalGpsRetryStats retryStats,
            String cursorValueAfter) {
        if (result.skipped() > 0) {
            syncExecutionService.finishWithWarning(
                    executionId,
                    result.read(),
                    result.positionsCreated(),
                    result.agentsUpdated(),
                    result.ignored(),
                    result.skipped(),
                    retryStats.getRetryAttempts(),
                    retryStats.getRateLimitErrors(),
                    retryStats.getServiceUnavailableErrors(),
                    cursorValueAfter,
                    "Location sync finished with " + result.skipped() + " invalid payload(s).");
            return;
        }

        syncExecutionService.finishSuccessfully(
                executionId,
                result.read(),
                result.positionsCreated(),
                result.agentsUpdated(),
                result.ignored(),
                0,
                retryStats.getRetryAttempts(),
                retryStats.getRateLimitErrors(),
                retryStats.getServiceUnavailableErrors(),
                cursorValueAfter);
    }

    private Agent findAgent(ExternalLocationResponse externalLocation) {
        return agentRepository.findById(externalLocation.agentId())
                .or(() -> StringUtils.hasText(externalLocation.externalId())
                        ? agentRepository.findByExternalId(externalLocation.externalId())
                        : java.util.Optional.empty())
                .orElse(null);
    }

    private boolean isDuplicatePosition(
            String agentId,
            ExternalLocationResponse externalLocation,
            Set<PositionSnapshotKey> seenPositionKeys) {
        PositionSnapshotKey positionSnapshotKey = PositionSnapshotKey.from(agentId, externalLocation);
        if (!seenPositionKeys.add(positionSnapshotKey)) {
            return true;
        }

        return agentPositionRepository.existsByAgent_IdAndLastSeenAndLatitudeAndLongitude(
                agentId,
                externalLocation.lastSeen(),
                normalizeCoordinate(externalLocation.latitude()),
                normalizeCoordinate(externalLocation.longitude()));
    }

    private boolean persistPosition(Agent agent, ExternalLocationResponse externalLocation) {
        AgentPosition agentPosition = new AgentPosition(
                agent,
                normalizeCoordinate(externalLocation.latitude()),
                normalizeCoordinate(externalLocation.longitude()),
                externalLocation.currentAddress(),
                externalLocation.accuracy(),
                externalLocation.speed(),
                externalLocation.battery(),
                AgentStatus.valueOf(externalLocation.status().name()),
                externalLocation.lastSeen());

        return agentPositionPersistenceService.saveSnapshot(agentPosition);
    }

    private void updateAgentCurrentLocation(Agent agent, ExternalLocationResponse externalLocation) {
        agent.updateCurrentLocation(
                normalizeCoordinate(externalLocation.latitude()),
                normalizeCoordinate(externalLocation.longitude()),
                externalLocation.currentAddress(),
                externalLocation.accuracy(),
                externalLocation.speed(),
                externalLocation.battery(),
                AgentStatus.valueOf(externalLocation.status().name()),
                externalLocation.lastSeen(),
                Instant.now());

        agentRepository.save(agent);
    }

    private boolean shouldUpdateCurrentLocation(Agent agent, Instant locationLastSeen) {
        if (agent.getLastSeen() == null) {
            return true;
        }
        if (locationLastSeen.isAfter(agent.getLastSeen())) {
            return true;
        }
        return locationLastSeen.equals(agent.getLastSeen())
                && (agent.getCurrentLatitude() == null || agent.getCurrentLongitude() == null);
    }

    private String validate(ExternalLocationResponse externalLocation) {
        if (externalLocation == null) {
            return "location payload is null";
        }
        if (!StringUtils.hasText(externalLocation.agentId())) {
            return "location.agentId is required";
        }
        if (externalLocation.latitude() == null) {
            return "location.latitude is required";
        }
        if (externalLocation.longitude() == null) {
            return "location.longitude is required";
        }
        if (isOutOfRange(externalLocation.latitude(), MIN_LATITUDE, MAX_LATITUDE)) {
            return "location.latitude must be between -90 and 90";
        }
        if (isOutOfRange(externalLocation.longitude(), MIN_LONGITUDE, MAX_LONGITUDE)) {
            return "location.longitude must be between -180 and 180";
        }
        if (externalLocation.accuracy() != null && externalLocation.accuracy().compareTo(ZERO) < 0) {
            return "location.accuracy must be greater than or equal to 0";
        }
        if (externalLocation.accuracy() != null
                && maxAccuracyMeters != null
                && externalLocation.accuracy().compareTo(maxAccuracyMeters) > 0) {
            return "location.accuracy exceeds max accepted accuracy of " + maxAccuracyMeters + " meters";
        }
        if (externalLocation.speed() != null && externalLocation.speed().compareTo(ZERO) < 0) {
            return "location.speed must be greater than or equal to 0";
        }
        if (externalLocation.status() == null) {
            return "location.status is required";
        }
        if (externalLocation.lastSeen() == null) {
            return "location.lastSeen is required";
        }
        return null;
    }

    private boolean isOutOfRange(BigDecimal value, BigDecimal min, BigDecimal max) {
        return value.compareTo(min) < 0 || value.compareTo(max) > 0;
    }

    private BigDecimal normalizeCoordinate(BigDecimal value) {
        return value.setScale(COORDINATE_SCALE, RoundingMode.HALF_UP);
    }

    private void recordInvalidPayload(
            Long executionId,
            String reason,
            ExternalLocationResponse externalLocation) {
        syncFailureService.recordInvalidPayload(
                executionId,
                SyncType.LOCATIONS,
                "location",
                reason,
                externalLocation);
    }

    private boolean isDelayedEvent(Agent agent, ExternalLocationResponse externalLocation) {
        return agent.getLastSeen() != null && externalLocation.lastSeen().isBefore(agent.getLastSeen());
    }

    private boolean hasSameTimestampPositionConflict(Agent agent, ExternalLocationResponse externalLocation) {
        return agent.getLastSeen() != null
                && externalLocation.lastSeen().equals(agent.getLastSeen())
                && agent.getCurrentLatitude() != null
                && agent.getCurrentLongitude() != null
                && (!sameValue(agent.getCurrentLatitude(), externalLocation.latitude())
                        || !sameValue(agent.getCurrentLongitude(), externalLocation.longitude()));
    }

    private boolean sameValue(BigDecimal first, BigDecimal second) {
        if (first == null || second == null) {
            return Objects.equals(first, second);
        }
        return first.compareTo(second) == 0;
    }

    private void recordLocationConflict(
            Long executionId,
            ExternalLocationResponse externalLocation,
            Agent agent,
            String reason) {
        Map<String, Object> localSnapshot = new HashMap<>();
        localSnapshot.put("agentId", agent.getId());
        localSnapshot.put("lastSeen", agent.getLastSeen());
        localSnapshot.put("currentLatitude", agent.getCurrentLatitude());
        localSnapshot.put("currentLongitude", agent.getCurrentLongitude());

        syncConflictService.recordConflict(
                executionId,
                SyncType.LOCATIONS,
                "location",
                externalLocation.agentId() + ":" + externalLocation.lastSeen(),
                reason,
                localSnapshot,
                externalLocation);
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

    private record LocationSyncOutcome(
            LocationSyncResultResponse result,
            String cursorValueAfter,
            Instant lastOccurredAt) {
    }

    private record PositionSnapshotKey(
            String agentId,
            Instant lastSeen,
            BigDecimal latitude,
            BigDecimal longitude) {

        private static PositionSnapshotKey from(String agentId, ExternalLocationResponse externalLocation) {
            return new PositionSnapshotKey(
                    agentId,
                    externalLocation.lastSeen(),
                    externalLocation.latitude().setScale(COORDINATE_SCALE, RoundingMode.HALF_UP),
                    externalLocation.longitude().setScale(COORDINATE_SCALE, RoundingMode.HALF_UP));
        }
    }
}
