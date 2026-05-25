package com.teams_tracking_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teams_tracking_system.model.SyncConflict;
import com.teams_tracking_system.model.SyncExecution;
import com.teams_tracking_system.model.SyncExecutionStatus;
import com.teams_tracking_system.model.SyncType;
import com.teams_tracking_system.repositories.SyncConflictRepository;
import com.teams_tracking_system.repositories.SyncExecutionRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SyncConflictServiceTest {

    @Mock
    private SyncConflictRepository syncConflictRepository;

    @Mock
    private SyncExecutionRepository syncExecutionRepository;

    private SyncConflictService syncConflictService;

    @BeforeEach
    void setUp() {
        syncConflictService = new SyncConflictService(
                syncConflictRepository,
                syncExecutionRepository,
                new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void recordConflictPersistsLocalAndExternalSnapshots() {
        SyncExecution execution = buildRunningExecution();
        when(syncExecutionRepository.findById(10L)).thenReturn(Optional.of(execution));
        when(syncConflictRepository.save(any(SyncConflict.class))).thenAnswer(invocation -> invocation.getArgument(0));

        syncConflictService.recordConflict(
                10L,
                SyncType.LOCATIONS,
                "location",
                "agent-1:2026-05-24T10:00:00Z",
                "Delayed GPS event kept in history but ignored as current location.",
                Map.of("agentId", "agent-1", "lastSeen", "2026-05-24T10:05:00Z"),
                Map.of("agentId", "agent-1", "lastSeen", "2026-05-24T10:00:00Z"));

        ArgumentCaptor<SyncConflict> captor = ArgumentCaptor.forClass(SyncConflict.class);
        verify(syncConflictRepository).save(captor.capture());
        SyncConflict saved = captor.getValue();
        assertThat(saved.getSyncExecution()).isEqualTo(execution);
        assertThat(saved.getSyncType()).isEqualTo(SyncType.LOCATIONS);
        assertThat(saved.getEntityType()).isEqualTo("location");
        assertThat(saved.getConflictKey()).isEqualTo("agent-1:2026-05-24T10:00:00Z");
        assertThat(saved.getReason()).isEqualTo("Delayed GPS event kept in history but ignored as current location.");
        assertThat(saved.getLocalSnapshotJson()).contains("\"agentId\":\"agent-1\"");
        assertThat(saved.getExternalPayloadJson()).contains("\"lastSeen\":\"2026-05-24T10:00:00Z\"");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    private SyncExecution buildRunningExecution() {
        Instant now = Instant.parse("2026-05-24T10:00:00Z");
        return new SyncExecution(
                SyncType.LOCATIONS,
                SyncExecutionStatus.RUNNING,
                now,
                null,
                now,
                now);
    }
}
