package com.teams_tracking_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teams_tracking_system.integrations.ExternalGpsApiClient;
import com.teams_tracking_system.integrations.dtos.ExternalAgentStatus;
import com.teams_tracking_system.integrations.dtos.ExternalLocationListResponse;
import com.teams_tracking_system.integrations.dtos.ExternalLocationResponse;
import com.teams_tracking_system.model.Agent;
import com.teams_tracking_system.model.AgentPosition;
import com.teams_tracking_system.model.AgentRole;
import com.teams_tracking_system.model.AgentStatus;
import com.teams_tracking_system.model.SyncType;
import com.teams_tracking_system.repositories.AgentPositionRepository;
import com.teams_tracking_system.repositories.AgentRepository;
import java.math.BigDecimal;
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
class LocationSyncServiceTest {

    private static final Instant LOCATION_LAST_SEEN = Instant.parse("2026-05-22T06:00:00Z");
    private static final String LOCATION_CURSOR = "2026-05-22T06:00:00Z";

    @Mock
    private ExternalGpsApiClient externalGpsApiClient;

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private AgentPositionRepository agentPositionRepository;

    @Mock
    private AgentPositionPersistenceService agentPositionPersistenceService;

    @Mock
    private SyncExecutionService syncExecutionService;

    @Mock
    private SyncFailureService syncFailureService;

    @Mock
    private SyncCursorService syncCursorService;

    @Mock
    private SyncConflictService syncConflictService;

    @InjectMocks
    private LocationSyncService locationSyncService;

    @Test
    void syncLocationsUpdatesCurrentAgentLocationAndPersistsPositionHistory() {
        Agent agent = agent("seed_agent_001", Instant.parse("2026-05-22T05:00:00Z"));
        ExternalLocationResponse externalLocation = externalLocation();

        when(syncCursorService.getLastCursorValue(SyncType.LOCATIONS)).thenReturn("cursor-before");
        when(syncExecutionService.start(SyncType.LOCATIONS, "cursor-before")).thenReturn(30L);
        when(externalGpsApiClient.findLocations(true))
                .thenReturn(new ExternalLocationListResponse(List.of(externalLocation)));
        when(agentRepository.findById("seed_agent_001")).thenReturn(Optional.of(agent));
        when(agentPositionRepository.existsByAgent_IdAndLastSeenAndLatitudeAndLongitude(
                "seed_agent_001",
                LOCATION_LAST_SEEN,
                new BigDecimal("-23.5505000"),
                new BigDecimal("-46.6333000")))
                .thenReturn(false);
        when(agentPositionPersistenceService.saveSnapshot(any(AgentPosition.class))).thenReturn(true);
        when(agentRepository.save(agent)).thenReturn(agent);

        var result = locationSyncService.syncLocations(true);

        assertThat(result.read()).isEqualTo(1);
        assertThat(result.positionsCreated()).isEqualTo(1);
        assertThat(result.agentsUpdated()).isEqualTo(1);
        assertThat(result.ignored()).isZero();
        assertThat(result.skipped()).isZero();
        assertThat(agent.getCurrentLatitude()).isEqualByComparingTo(new BigDecimal("-23.5505000"));
        assertThat(agent.getCurrentLongitude()).isEqualByComparingTo(new BigDecimal("-46.6333000"));
        assertThat(agent.getStatus()).isEqualTo(AgentStatus.ONLINE);

        ArgumentCaptor<AgentPosition> positionCaptor = ArgumentCaptor.forClass(AgentPosition.class);
        verify(agentPositionPersistenceService).saveSnapshot(positionCaptor.capture());
        assertThat(positionCaptor.getValue().getAgent()).isSameAs(agent);
        assertThat(positionCaptor.getValue().getLastSeen()).isEqualTo(LOCATION_LAST_SEEN);

        verify(syncCursorService).upsertAfterSuccessfulSync(
                eq(SyncType.LOCATIONS),
                eq(LOCATION_CURSOR),
                isNull(),
                eq(LOCATION_LAST_SEEN),
                any(Instant.class));
        verify(syncExecutionService).finishSuccessfully(30L, 1, 1, 1, 0, 0, 0, 0, 0, LOCATION_CURSOR);
    }

    @Test
    void syncLocationsIgnoresDuplicatePositionButKeepsCurrentAgentUpdateIdempotent() {
        Agent agent = agentWithCurrentLocation("seed_agent_001");
        ExternalLocationResponse externalLocation = externalLocation();

        when(syncExecutionService.start(SyncType.LOCATIONS, null)).thenReturn(31L);
        when(externalGpsApiClient.findLocations(null))
                .thenReturn(new ExternalLocationListResponse(List.of(externalLocation)));
        when(agentRepository.findById("seed_agent_001")).thenReturn(Optional.of(agent));
        when(agentPositionRepository.existsByAgent_IdAndLastSeenAndLatitudeAndLongitude(
                "seed_agent_001",
                LOCATION_LAST_SEEN,
                new BigDecimal("-23.5505000"),
                new BigDecimal("-46.6333000")))
                .thenReturn(true);

        var result = locationSyncService.syncLocations(null);

        assertThat(result.positionsCreated()).isZero();
        assertThat(result.agentsUpdated()).isZero();
        assertThat(result.ignored()).isEqualTo(1);
        verify(agentPositionPersistenceService, never()).saveSnapshot(any(AgentPosition.class));
        verify(agentRepository, never()).save(any(Agent.class));
        verify(syncExecutionService).finishSuccessfully(31L, 1, 0, 0, 1, 0, 0, 0, 0, LOCATION_CURSOR);
    }

    @Test
    void syncLocationsIgnoresDuplicatePositionWithinSamePayload() {
        Agent agent = agent("seed_agent_001", Instant.parse("2026-05-22T05:00:00Z"));
        ExternalLocationResponse externalLocation = externalLocation();

        when(syncExecutionService.start(SyncType.LOCATIONS, null)).thenReturn(36L);
        when(externalGpsApiClient.findLocations(null))
                .thenReturn(new ExternalLocationListResponse(List.of(externalLocation, externalLocation)));
        when(agentRepository.findById("seed_agent_001")).thenReturn(Optional.of(agent));
        when(agentPositionRepository.existsByAgent_IdAndLastSeenAndLatitudeAndLongitude(
                "seed_agent_001",
                LOCATION_LAST_SEEN,
                new BigDecimal("-23.5505000"),
                new BigDecimal("-46.6333000")))
                .thenReturn(false);
        when(agentPositionPersistenceService.saveSnapshot(any(AgentPosition.class))).thenReturn(true);
        when(agentRepository.save(agent)).thenReturn(agent);

        var result = locationSyncService.syncLocations(null);

        assertThat(result.read()).isEqualTo(2);
        assertThat(result.positionsCreated()).isEqualTo(1);
        assertThat(result.agentsUpdated()).isEqualTo(1);
        assertThat(result.ignored()).isEqualTo(1);
        assertThat(result.skipped()).isZero();
        verify(agentPositionPersistenceService).saveSnapshot(any(AgentPosition.class));
        verify(syncExecutionService).finishSuccessfully(36L, 2, 1, 1, 1, 0, 0, 0, 0, LOCATION_CURSOR);
    }

    @Test
    void syncLocationsIgnoresDuplicateSnapshotRejectedByDatabase() {
        Agent agent = agent("seed_agent_001", Instant.parse("2026-05-22T05:00:00Z"));
        ExternalLocationResponse externalLocation = externalLocation();

        when(syncExecutionService.start(SyncType.LOCATIONS, null)).thenReturn(37L);
        when(externalGpsApiClient.findLocations(null))
                .thenReturn(new ExternalLocationListResponse(List.of(externalLocation)));
        when(agentRepository.findById("seed_agent_001")).thenReturn(Optional.of(agent));
        when(agentPositionRepository.existsByAgent_IdAndLastSeenAndLatitudeAndLongitude(
                "seed_agent_001",
                LOCATION_LAST_SEEN,
                new BigDecimal("-23.5505000"),
                new BigDecimal("-46.6333000")))
                .thenReturn(false);
        when(agentPositionPersistenceService.saveSnapshot(any(AgentPosition.class))).thenReturn(false);
        when(agentRepository.save(agent)).thenReturn(agent);

        var result = locationSyncService.syncLocations(null);

        assertThat(result.read()).isEqualTo(1);
        assertThat(result.positionsCreated()).isZero();
        assertThat(result.agentsUpdated()).isEqualTo(1);
        assertThat(result.ignored()).isEqualTo(1);
        assertThat(result.skipped()).isZero();
        verify(syncExecutionService).finishSuccessfully(37L, 1, 0, 1, 1, 0, 0, 0, 0, LOCATION_CURSOR);
    }

    @Test
    void syncLocationsDoesNotOverwriteCurrentLocationWithOlderEvent() {
        Agent agent = agent("seed_agent_001", Instant.parse("2026-05-22T07:00:00Z"));
        ExternalLocationResponse externalLocation = externalLocation();

        when(syncExecutionService.start(SyncType.LOCATIONS, null)).thenReturn(32L);
        when(externalGpsApiClient.findLocations(null))
                .thenReturn(new ExternalLocationListResponse(List.of(externalLocation)));
        when(agentRepository.findById("seed_agent_001")).thenReturn(Optional.of(agent));
        when(agentPositionPersistenceService.saveSnapshot(any(AgentPosition.class))).thenReturn(true);

        var result = locationSyncService.syncLocations(null);

        assertThat(result.positionsCreated()).isEqualTo(1);
        assertThat(result.agentsUpdated()).isZero();
        assertThat(agent.getCurrentLatitude()).isNull();
        verify(agentRepository, never()).save(any(Agent.class));
        verify(agentPositionPersistenceService).saveSnapshot(any(AgentPosition.class));
        verify(syncConflictService).recordConflict(
                eq(32L),
                eq(SyncType.LOCATIONS),
                eq("location"),
                eq("seed_agent_001:2026-05-22T06:00:00Z"),
                eq("Delayed GPS event kept in history but ignored as current location."),
                any(),
                eq(externalLocation));
    }

    @Test
    void syncLocationsSkipsInvalidCoordinatesAndRecordsFailure() {
        ExternalLocationResponse invalidLocation = new ExternalLocationResponse(
                "seed_agent_001",
                "ext-agent-001",
                "Carlos Silva",
                new BigDecimal("91"),
                new BigDecimal("-46.6333000"),
                "Av. Paulista, 1000 - Sao Paulo, SP",
                new BigDecimal("8.50"),
                BigDecimal.ZERO,
                85,
                ExternalAgentStatus.ONLINE,
                LOCATION_LAST_SEEN);

        when(syncExecutionService.start(SyncType.LOCATIONS, null)).thenReturn(33L);
        when(externalGpsApiClient.findLocations(null))
                .thenReturn(new ExternalLocationListResponse(List.of(invalidLocation)));

        var result = locationSyncService.syncLocations(null);

        assertThat(result.read()).isEqualTo(1);
        assertThat(result.positionsCreated()).isZero();
        assertThat(result.agentsUpdated()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
        verify(agentRepository, never()).findById(any());
        verify(syncFailureService).recordInvalidPayload(
                33L,
                SyncType.LOCATIONS,
                "location",
                "location.latitude must be between -90 and 90",
                invalidLocation);
        verify(syncExecutionService).finishWithWarning(
                eq(33L),
                eq(1),
                eq(0),
                eq(0),
                eq(0),
                eq(1),
                eq(0),
                eq(0),
                eq(0),
                isNull(),
                eq("Location sync finished with 1 invalid payload(s)."));
    }

    @Test
    void syncLocationsSkipsImpreciseGpsCoordinatesAndRecordsFailure() {
        ExternalLocationResponse impreciseLocation = new ExternalLocationResponse(
                "seed_agent_001",
                "ext-agent-001",
                "Carlos Silva",
                new BigDecimal("-23.5505000"),
                new BigDecimal("-46.6333000"),
                "Av. Paulista, 1000 - Sao Paulo, SP",
                new BigDecimal("150.00"),
                BigDecimal.ZERO,
                85,
                ExternalAgentStatus.ONLINE,
                LOCATION_LAST_SEEN);

        when(syncExecutionService.start(SyncType.LOCATIONS, null)).thenReturn(35L);
        when(externalGpsApiClient.findLocations(null))
                .thenReturn(new ExternalLocationListResponse(List.of(impreciseLocation)));

        var result = locationSyncService.syncLocations(null);

        assertThat(result.read()).isEqualTo(1);
        assertThat(result.positionsCreated()).isZero();
        assertThat(result.agentsUpdated()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
        verify(agentRepository, never()).findById(any());
        verify(agentPositionPersistenceService, never()).saveSnapshot(any(AgentPosition.class));
        verify(syncFailureService).recordInvalidPayload(
                35L,
                SyncType.LOCATIONS,
                "location",
                "location.accuracy exceeds max accepted accuracy of 50 meters",
                impreciseLocation);
        verify(syncExecutionService).finishWithWarning(
                eq(35L),
                eq(1),
                eq(0),
                eq(0),
                eq(0),
                eq(1),
                eq(0),
                eq(0),
                eq(0),
                isNull(),
                eq("Location sync finished with 1 invalid payload(s)."));
    }

    @Test
    void syncLocationsDoesNotAdvanceCursorWhenExternalCallFails() {
        RuntimeException exception = new RuntimeException("external unavailable");

        when(syncCursorService.getLastCursorValue(SyncType.LOCATIONS)).thenReturn("cursor-before");
        when(syncExecutionService.start(SyncType.LOCATIONS, "cursor-before")).thenReturn(34L);
        when(externalGpsApiClient.findLocations(null)).thenThrow(exception);

        assertThatThrownBy(() -> locationSyncService.syncLocations(null))
                .isSameAs(exception);

        verify(syncCursorService, never()).upsertAfterSuccessfulSync(any(), any(), any(), any(), any());
        verify(syncExecutionService).fail(34L, exception, 0, 0, 0);
    }

    private ExternalLocationResponse externalLocation() {
        return new ExternalLocationResponse(
                "seed_agent_001",
                "ext-agent-001",
                "Carlos Silva",
                new BigDecimal("-23.5505000"),
                new BigDecimal("-46.6333000"),
                "Av. Paulista, 1000 - Sao Paulo, SP",
                new BigDecimal("8.50"),
                BigDecimal.ZERO,
                85,
                ExternalAgentStatus.ONLINE,
                LOCATION_LAST_SEEN);
    }

    private Agent agent(String id, Instant lastSeen) {
        Instant now = Instant.parse("2026-05-22T05:00:00Z");
        return new Agent(
                id,
                "ext-agent-001",
                "Carlos Silva",
                AgentRole.TECHNICIAN,
                "Alpha",
                null,
                null,
                true,
                AgentStatus.OFFLINE,
                20,
                lastSeen,
                null,
                null,
                null,
                null,
                null,
                now,
                now);
    }

    private Agent agentWithCurrentLocation(String id) {
        Instant now = Instant.parse("2026-05-22T05:00:00Z");
        return new Agent(
                id,
                "ext-agent-001",
                "Carlos Silva",
                AgentRole.TECHNICIAN,
                "Alpha",
                null,
                null,
                true,
                AgentStatus.ONLINE,
                85,
                LOCATION_LAST_SEEN,
                new BigDecimal("-23.5505000"),
                new BigDecimal("-46.6333000"),
                "Av. Paulista, 1000 - Sao Paulo, SP",
                new BigDecimal("8.50"),
                BigDecimal.ZERO,
                now,
                now);
    }
}
