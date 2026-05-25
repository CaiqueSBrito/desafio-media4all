package com.teams_tracking_system.service;

import com.teams_tracking_system.dtos.schemas.AgentSyncResultResponse;
import com.teams_tracking_system.integrations.ExternalGpsApiClient;
import com.teams_tracking_system.integrations.ExternalGpsRetryStats;
import com.teams_tracking_system.integrations.dtos.ExternalAgentResponse;
import com.teams_tracking_system.model.Agent;
import com.teams_tracking_system.model.AgentRole;
import com.teams_tracking_system.model.AgentStatus;
import com.teams_tracking_system.model.SyncType;
import com.teams_tracking_system.repositories.AgentRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentSyncService {

    private final ExternalGpsApiClient externalGpsApiClient;
    private final AgentRepository agentRepository;
    private final SyncExecutionService syncExecutionService;
    private final SyncFailureService syncFailureService;
    private final SyncCursorService syncCursorService;
    private final SyncConflictService syncConflictService;

    public AgentSyncService(
            ExternalGpsApiClient externalGpsApiClient,
            AgentRepository agentRepository,
            SyncExecutionService syncExecutionService,
            SyncFailureService syncFailureService,
            SyncCursorService syncCursorService,
            SyncConflictService syncConflictService) {
        this.externalGpsApiClient = externalGpsApiClient;
        this.agentRepository = agentRepository;
        this.syncExecutionService = syncExecutionService;
        this.syncFailureService = syncFailureService;
        this.syncCursorService = syncCursorService;
        this.syncConflictService = syncConflictService;
    }

    @Transactional
    public AgentSyncResultResponse syncAgents(Boolean active) {
        String previousCursor = syncCursorService.getLastCursorValue(SyncType.AGENTS);
        Long executionId = syncExecutionService.start(SyncType.AGENTS, previousCursor);

        try {
            AgentSyncOutcome outcome = executeSyncAgents(active, executionId, previousCursor);
            syncCursorService.upsertAfterSuccessfulSync(
                    SyncType.AGENTS,
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

    private AgentSyncOutcome executeSyncAgents(Boolean active, Long executionId, String previousCursor) {
        var response = externalGpsApiClient.findAgents(active);
        if (response == null || response.data() == null) {
            return new AgentSyncOutcome(new AgentSyncResultResponse(0, 0, 0, 0), previousCursor, null);
        }

        int created = 0;
        int updated = 0;
        int skipped = 0;
        Instant checkpoint = null;
        Set<String> seenExternalIds = new HashSet<>();
        Set<String> seenSourceIds = new HashSet<>();

        for (ExternalAgentResponse externalAgent : response.data()) {
            String invalidReason = validate(externalAgent);
            if (invalidReason != null) {
                recordInvalidPayload(executionId, invalidReason, externalAgent);
                skipped++;
                continue;
            }

            String duplicateReason = detectDuplicateInPayload(externalAgent, seenExternalIds, seenSourceIds);
            if (duplicateReason != null) {
                recordInvalidPayload(executionId, duplicateReason, externalAgent);
                skipped++;
                continue;
            }

            var existingAgent = agentRepository.findByExternalId(externalAgent.externalId());
            if (existingAgent.isPresent()) {
                updateAgent(existingAgent.get(), externalAgent);
                updated++;
                checkpoint = max(checkpoint, resolveCheckpoint(externalAgent));
            } else if (agentRepository.findById(externalAgent.id()).isPresent()) {
                recordInvalidPayload(
                        executionId,
                        "agent.id already exists with another externalId: " + externalAgent.id(),
                        externalAgent);
                recordAgentIdConflict(executionId, externalAgent);
                skipped++;
            } else {
                createAgent(externalAgent);
                created++;
                checkpoint = max(checkpoint, resolveCheckpoint(externalAgent));
            }
        }

        String cursorValueAfter = checkpoint != null ? checkpoint.toString() : previousCursor;
        return new AgentSyncOutcome(
                new AgentSyncResultResponse(response.data().size(), created, updated, skipped),
                cursorValueAfter,
                checkpoint);
    }

    private void finishExecution(
            Long executionId,
            AgentSyncResultResponse result,
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
                    "Agent sync finished with " + result.skipped() + " invalid payload(s).");
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

    private ExternalGpsRetryStats resolveRetryStats() {
        ExternalGpsRetryStats retryStats = externalGpsApiClient.getLastRetryStats();
        return retryStats != null ? retryStats : new ExternalGpsRetryStats();
    }

    private void recordInvalidPayload(
            Long executionId,
            String reason,
            ExternalAgentResponse externalAgent) {
        syncFailureService.recordInvalidPayload(
                executionId,
                SyncType.AGENTS,
                "agent",
                reason,
                externalAgent);
    }

    private void recordAgentIdConflict(Long executionId, ExternalAgentResponse externalAgent) {
        syncConflictService.recordConflict(
                executionId,
                SyncType.AGENTS,
                "agent",
                externalAgent.id(),
                "External agent id conflicts with an existing local agent using another externalId.",
                Map.of("id", externalAgent.id()),
                externalAgent);
    }

    private String detectDuplicateInPayload(
            ExternalAgentResponse externalAgent,
            Set<String> seenExternalIds,
            Set<String> seenSourceIds) {
        if (seenSourceIds.contains(externalAgent.id())) {
            return "duplicate agent.id in external payload: " + externalAgent.id();
        }
        if (seenExternalIds.contains(externalAgent.externalId())) {
            return "duplicate agent.externalId in external payload: " + externalAgent.externalId();
        }

        seenSourceIds.add(externalAgent.id());
        seenExternalIds.add(externalAgent.externalId());
        return null;
    }

    private String validate(ExternalAgentResponse externalAgent) {
        if (externalAgent == null) {
            return "agent payload is null";
        }
        if (externalAgent.id() == null || externalAgent.id().isBlank()) {
            return "agent.id is required";
        }
        if (externalAgent.externalId() == null || externalAgent.externalId().isBlank()) {
            return "agent.externalId is required";
        }
        if (externalAgent.name() == null || externalAgent.name().isBlank()) {
            return "agent.name is required";
        }
        if (externalAgent.role() == null) {
            return "agent.role is required";
        }
        if (externalAgent.active() == null) {
            return "agent.active is required";
        }
        if (externalAgent.status() == null) {
            return "agent.status is required";
        }
        return null;
    }

    private void updateAgent(Agent agent, ExternalAgentResponse externalAgent) {
        agent.updateFromExternalData(
                externalAgent.externalId(),
                externalAgent.name(),
                AgentRole.valueOf(externalAgent.role().name()),
                externalAgent.team(),
                externalAgent.phone(),
                externalAgent.email(),
                externalAgent.active(),
                AgentStatus.valueOf(externalAgent.status().name()),
                externalAgent.battery(),
                externalAgent.lastSeen(),
                resolveUpdatedAt(externalAgent));

        agentRepository.save(agent);
    }

    private void createAgent(ExternalAgentResponse externalAgent) {
        Instant now = Instant.now();
        Agent agent = new Agent(
                externalAgent.id(),
                externalAgent.externalId(),
                externalAgent.name(),
                AgentRole.valueOf(externalAgent.role().name()),
                externalAgent.team(),
                externalAgent.phone(),
                externalAgent.email(),
                externalAgent.active(),
                AgentStatus.valueOf(externalAgent.status().name()),
                externalAgent.battery(),
                externalAgent.lastSeen(),
                null,
                null,
                null,
                null,
                null,
                externalAgent.createdAt() != null ? externalAgent.createdAt() : now,
                resolveUpdatedAt(externalAgent));

        agentRepository.save(agent);
    }

    private Instant resolveUpdatedAt(ExternalAgentResponse externalAgent) {
        return externalAgent.updatedAt() != null ? externalAgent.updatedAt() : Instant.now();
    }

    private Instant resolveCheckpoint(ExternalAgentResponse externalAgent) {
        if (externalAgent.updatedAt() != null) {
            return externalAgent.updatedAt();
        }
        if (externalAgent.createdAt() != null) {
            return externalAgent.createdAt();
        }
        return externalAgent.lastSeen();
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

    private record AgentSyncOutcome(
            AgentSyncResultResponse result,
            String cursorValueAfter,
            Instant lastOccurredAt) {
    }
}
