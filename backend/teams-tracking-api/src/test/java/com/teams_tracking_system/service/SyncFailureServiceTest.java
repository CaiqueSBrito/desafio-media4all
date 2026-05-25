package com.teams_tracking_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teams_tracking_system.integrations.dtos.ExternalAgentResponse;
import com.teams_tracking_system.integrations.dtos.ExternalAgentRole;
import com.teams_tracking_system.integrations.dtos.ExternalAgentStatus;
import com.teams_tracking_system.model.SyncExecution;
import com.teams_tracking_system.model.SyncExecutionStatus;
import com.teams_tracking_system.model.SyncFailure;
import com.teams_tracking_system.model.SyncType;
import com.teams_tracking_system.repositories.SyncExecutionRepository;
import com.teams_tracking_system.repositories.SyncFailureRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SyncFailureServiceTest {

    @Mock
    private SyncFailureRepository syncFailureRepository;

    @Mock
    private SyncExecutionRepository syncExecutionRepository;

    private SyncFailureService syncFailureService;

    @BeforeEach
    void setUp() {
        syncFailureService = new SyncFailureService(
                syncFailureRepository,
                syncExecutionRepository,
                new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void recordInvalidPayloadPersistsStructuredFailure() {
        SyncExecution execution = buildRunningExecution();
        ExternalAgentResponse payload = new ExternalAgentResponse(
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

        when(syncExecutionRepository.findById(10L)).thenReturn(Optional.of(execution));
        when(syncFailureRepository.save(any(SyncFailure.class))).thenAnswer(invocation -> invocation.getArgument(0));

        syncFailureService.recordInvalidPayload(
                10L,
                SyncType.AGENTS,
                "agent",
                "agent.id is required",
                payload);

        ArgumentCaptor<SyncFailure> captor = ArgumentCaptor.forClass(SyncFailure.class);
        verify(syncFailureRepository).save(captor.capture());
        SyncFailure saved = captor.getValue();
        assertThat(saved.getSyncExecution()).isEqualTo(execution);
        assertThat(saved.getSyncType()).isEqualTo(SyncType.AGENTS);
        assertThat(saved.getEntityType()).isEqualTo("agent");
        assertThat(saved.getReason()).isEqualTo("agent.id is required");
        assertThat(saved.getPayloadJson()).contains("\"externalId\":\"ext-agent-001\"");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    private SyncExecution buildRunningExecution() {
        Instant now = Instant.parse("2026-05-23T10:00:00Z");
        return new SyncExecution(
                SyncType.AGENTS,
                SyncExecutionStatus.RUNNING,
                now,
                null,
                now,
                now);
    }
}
