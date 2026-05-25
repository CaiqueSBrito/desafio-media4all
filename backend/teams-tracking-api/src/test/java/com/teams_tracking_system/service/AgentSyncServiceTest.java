package com.teams_tracking_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teams_tracking_system.integrations.ExternalGpsApiClient;
import com.teams_tracking_system.integrations.ExternalGpsRetryStats;
import com.teams_tracking_system.integrations.dtos.ExternalAgentListResponse;
import com.teams_tracking_system.integrations.dtos.ExternalAgentResponse;
import com.teams_tracking_system.integrations.dtos.ExternalAgentRole;
import com.teams_tracking_system.integrations.dtos.ExternalAgentStatus;
import com.teams_tracking_system.model.Agent;
import com.teams_tracking_system.model.AgentRole;
import com.teams_tracking_system.model.AgentStatus;
import com.teams_tracking_system.model.SyncType;
import com.teams_tracking_system.repositories.AgentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentSyncServiceTest {

    @Mock
    private ExternalGpsApiClient externalGpsApiClient;

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private SyncExecutionService syncExecutionService;

    @Mock
    private SyncFailureService syncFailureService;

    @Mock
    private SyncCursorService syncCursorService;

    @Mock
    private SyncConflictService syncConflictService;

    @InjectMocks
    private AgentSyncService agentSyncService;

    private static final String AGENT_CURSOR = "2026-05-23T02:35:50.470Z";

    @Test
    void syncAgentsCreatesMissingAgentsUsingExternalIdForIdempotency() {
        ExternalAgentResponse externalAgent = externalAgent(
                "seed_agent_001",
                "ext-agent-001",
                ExternalAgentStatus.ONLINE);
        when(externalGpsApiClient.findAgents(true)).thenReturn(new ExternalAgentListResponse(List.of(externalAgent)));
        when(agentRepository.findByExternalId("ext-agent-001")).thenReturn(Optional.empty());
        when(agentRepository.save(any(Agent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(syncExecutionService.start(any(), any())).thenReturn(1L);

        var result = agentSyncService.syncAgents(true);

        assertThat(result.read()).isEqualTo(1);
        assertThat(result.created()).isEqualTo(1);
        assertThat(result.updated()).isZero();
        assertThat(result.skipped()).isZero();

        ArgumentCaptor<Agent> agentCaptor = ArgumentCaptor.forClass(Agent.class);
        verify(agentRepository).save(agentCaptor.capture());
        Agent savedAgent = agentCaptor.getValue();
        assertThat(savedAgent.getId()).isEqualTo("seed_agent_001");
        assertThat(savedAgent.getExternalId()).isEqualTo("ext-agent-001");
        assertThat(savedAgent.getStatus()).isEqualTo(AgentStatus.ONLINE);
        verify(syncCursorService).upsertAfterSuccessfulSync(
                eq(SyncType.AGENTS),
                eq(AGENT_CURSOR),
                isNull(),
                eq(Instant.parse(AGENT_CURSOR)),
                any(Instant.class));
        verify(syncExecutionService).finishSuccessfully(1L, 1, 1, 0, 0, 0, 0, 0, 0, AGENT_CURSOR);
    }

    @Test
    void syncAgentsUpdatesExistingAgents() {
        ExternalAgentResponse externalAgent = externalAgent(
                "seed_agent_001",
                "ext-agent-001",
                ExternalAgentStatus.PAUSED);
        Agent existingAgent = existingAgent();
        when(externalGpsApiClient.findAgents(null)).thenReturn(new ExternalAgentListResponse(List.of(externalAgent)));
        when(agentRepository.findByExternalId("ext-agent-001")).thenReturn(Optional.of(existingAgent));
        when(agentRepository.save(any(Agent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(syncExecutionService.start(any(), any())).thenReturn(2L);

        var result = agentSyncService.syncAgents(null);

        assertThat(result.read()).isEqualTo(1);
        assertThat(result.created()).isZero();
        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.skipped()).isZero();
        assertThat(existingAgent.getStatus()).isEqualTo(AgentStatus.PAUSED);
        verify(agentRepository).save(existingAgent);
        verify(syncExecutionService).finishSuccessfully(2L, 1, 0, 1, 0, 0, 0, 0, 0, AGENT_CURSOR);
    }

    @Test
    void syncAgentsSkipsInvalidExternalPayloads() {
        ExternalAgentResponse invalidAgent = new ExternalAgentResponse(
                null,
                "ext-agent-001",
                "Carlos Silva",
                ExternalAgentRole.TECHNICIAN,
                "Alpha",
                "+5511999990001",
                "carlos@tecnico.com",
                true,
                ExternalAgentStatus.ONLINE,
                85,
                Instant.parse("2026-05-22T06:00:00Z"),
                Instant.parse("2026-05-23T02:35:33.876Z"),
                Instant.parse("2026-05-23T02:35:50.470Z"));
        when(externalGpsApiClient.findAgents(null)).thenReturn(new ExternalAgentListResponse(List.of(invalidAgent)));
        when(syncExecutionService.start(any(), any())).thenReturn(3L);

        var result = agentSyncService.syncAgents(null);

        assertThat(result.read()).isEqualTo(1);
        assertThat(result.created()).isZero();
        assertThat(result.updated()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
        verify(syncFailureService).recordInvalidPayload(
                3L,
                com.teams_tracking_system.model.SyncType.AGENTS,
                "agent",
                "agent.id is required",
                invalidAgent);
        verify(syncExecutionService).finishWithWarning(
                3L,
                1,
                0,
                0,
                0,
                1,
                0,
                0,
                0,
                null,
                "Agent sync finished with 1 invalid payload(s).");
    }

    @Test
    void syncAgentsPersistsRetryStatsInSyncExecution() {
        ExternalAgentResponse externalAgent = externalAgent(
                "seed_agent_001",
                "ext-agent-001",
                ExternalAgentStatus.ONLINE);
        ExternalGpsRetryStats retryStats = new ExternalGpsRetryStats();
        retryStats.recordRetry(429);
        retryStats.recordRetry(503);

        when(externalGpsApiClient.findAgents(null)).thenReturn(new ExternalAgentListResponse(List.of(externalAgent)));
        when(externalGpsApiClient.getLastRetryStats()).thenReturn(retryStats);
        when(agentRepository.findByExternalId("ext-agent-001")).thenReturn(Optional.empty());
        when(agentRepository.save(any(Agent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(syncExecutionService.start(any(), any())).thenReturn(4L);

        agentSyncService.syncAgents(null);

        verify(syncExecutionService).finishSuccessfully(4L, 1, 1, 0, 0, 0, 2, 1, 1, AGENT_CURSOR);
    }

    @Test
    void syncAgentsSkipsDuplicateExternalIdsFromSameExternalPayload() {
        ExternalAgentResponse firstAgent = externalAgent(
                "seed_agent_001",
                "ext-agent-001",
                ExternalAgentStatus.ONLINE);
        ExternalAgentResponse duplicatedAgent = externalAgent(
                "seed_agent_002",
                "ext-agent-001",
                ExternalAgentStatus.ONLINE);

        when(externalGpsApiClient.findAgents(null))
                .thenReturn(new ExternalAgentListResponse(List.of(firstAgent, duplicatedAgent)));
        when(agentRepository.findByExternalId("ext-agent-001")).thenReturn(Optional.empty());
        when(agentRepository.save(any(Agent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(syncExecutionService.start(any(), any())).thenReturn(5L);

        var result = agentSyncService.syncAgents(null);

        assertThat(result.read()).isEqualTo(2);
        assertThat(result.created()).isEqualTo(1);
        assertThat(result.updated()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
        verify(agentRepository, times(1)).save(any(Agent.class));
        verify(syncFailureService).recordInvalidPayload(
                5L,
                com.teams_tracking_system.model.SyncType.AGENTS,
                "agent",
                "duplicate agent.externalId in external payload: ext-agent-001",
                duplicatedAgent);
        verify(syncExecutionService).finishWithWarning(
                5L,
                2,
                1,
                0,
                0,
                1,
                0,
                0,
                0,
                AGENT_CURSOR,
                "Agent sync finished with 1 invalid payload(s).");
    }

    @Test
    void syncAgentsSkipsPayloadWhenExternalSourceIdAlreadyExistsWithAnotherExternalId() {
        ExternalAgentResponse externalAgent = externalAgent(
                "seed_agent_001",
                "ext-agent-002",
                ExternalAgentStatus.ONLINE);

        when(externalGpsApiClient.findAgents(null)).thenReturn(new ExternalAgentListResponse(List.of(externalAgent)));
        when(agentRepository.findByExternalId("ext-agent-002")).thenReturn(Optional.empty());
        when(agentRepository.findById("seed_agent_001")).thenReturn(Optional.of(existingAgent()));
        when(syncExecutionService.start(any(), any())).thenReturn(6L);

        var result = agentSyncService.syncAgents(null);

        assertThat(result.read()).isEqualTo(1);
        assertThat(result.created()).isZero();
        assertThat(result.updated()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
        verify(agentRepository, never()).save(any(Agent.class));
        verify(syncFailureService).recordInvalidPayload(
                6L,
                com.teams_tracking_system.model.SyncType.AGENTS,
                "agent",
                "agent.id already exists with another externalId: seed_agent_001",
                externalAgent);
        verify(syncConflictService).recordConflict(
                eq(6L),
                eq(SyncType.AGENTS),
                eq("agent"),
                eq("seed_agent_001"),
                eq("External agent id conflicts with an existing local agent using another externalId."),
                any(),
                eq(externalAgent));
        verify(syncExecutionService).finishWithWarning(
                6L,
                1,
                0,
                0,
                0,
                1,
                0,
                0,
                0,
                null,
                "Agent sync finished with 1 invalid payload(s).");
    }

    private ExternalAgentResponse externalAgent(
            String id,
            String externalId,
            ExternalAgentStatus status) {
        return new ExternalAgentResponse(
                id,
                externalId,
                "Carlos Silva",
                ExternalAgentRole.TECHNICIAN,
                "Alpha",
                "+5511999990001",
                "carlos@tecnico.com",
                true,
                status,
                85,
                Instant.parse("2026-05-22T06:00:00Z"),
                Instant.parse("2026-05-23T02:35:33.876Z"),
                Instant.parse("2026-05-23T02:35:50.470Z"));
    }

    private Agent existingAgent() {
        Instant now = Instant.parse("2026-05-22T10:00:00Z");
        return new Agent(
                "seed_agent_001",
                "ext-agent-001",
                "Old Name",
                AgentRole.TECHNICIAN,
                "Old Team",
                null,
                null,
                true,
                AgentStatus.ONLINE,
                10,
                now,
                null,
                null,
                null,
                null,
                null,
                now,
                now);
    }
}
