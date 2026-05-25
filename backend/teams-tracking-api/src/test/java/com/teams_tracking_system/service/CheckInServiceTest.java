package com.teams_tracking_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teams_tracking_system.core.exception.InvalidRequestException;
import com.teams_tracking_system.core.exception.ResourceNotFoundException;
import com.teams_tracking_system.dtos.requests.ManualCheckInRequest;
import com.teams_tracking_system.model.Agent;
import com.teams_tracking_system.model.AgentRole;
import com.teams_tracking_system.model.AgentStatus;
import com.teams_tracking_system.model.CheckIn;
import com.teams_tracking_system.model.CheckInType;
import com.teams_tracking_system.model.SyncSource;
import com.teams_tracking_system.repositories.AgentRepository;
import com.teams_tracking_system.repositories.CheckInRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckInServiceTest {

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private CheckInRepository checkInRepository;

    @InjectMocks
    private CheckInService checkInService;

    @Test
    void createManualPersistsCheckInForActiveAgent() {
        Agent agent = buildAgent("agent-1", true);
        ManualCheckInRequest request = new ManualCheckInRequest(
                CheckInType.CHECKIN,
                new BigDecimal("-23.5505200"),
                new BigDecimal("-46.6333090"),
                "Sao Paulo",
                new BigDecimal("12.50"),
                BigDecimal.ZERO,
                "Manual check-in",
                Instant.parse("2026-05-23T10:00:00Z"),
                "request-1");

        when(agentRepository.findById("agent-1")).thenReturn(Optional.of(agent));
        when(checkInRepository.findByManualIdempotencyKey("request-1")).thenReturn(Optional.empty());
        when(checkInRepository.save(any(CheckIn.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = checkInService.createManual("agent-1", request);

        assertThat(response.agentId()).isEqualTo("agent-1");
        assertThat(response.type()).isEqualTo(CheckInType.CHECKIN);
        assertThat(response.source()).isEqualTo(SyncSource.MANUAL);
        assertThat(response.manualIdempotencyKey()).isEqualTo("request-1");
        assertThat(response.occurredAt()).isEqualTo(Instant.parse("2026-05-23T10:00:00Z"));

        ArgumentCaptor<CheckIn> checkInCaptor = ArgumentCaptor.forClass(CheckIn.class);
        verify(checkInRepository).save(checkInCaptor.capture());
        assertThat(checkInCaptor.getValue().getExternalEventId()).isNull();
        assertThat(checkInCaptor.getValue().getSyncedAt()).isNotNull();
    }

    @Test
    void createManualReturnsExistingCheckInWhenIdempotencyKeyWasAlreadyProcessed() {
        Agent agent = buildAgent("agent-1", true);
        CheckIn existing = buildCheckIn(agent, "idem-1");
        ManualCheckInRequest request = new ManualCheckInRequest(
                CheckInType.CHECKIN,
                BigDecimal.ONE,
                BigDecimal.TEN,
                null,
                null,
                null,
                null,
                null,
                " idem-1 ");

        when(agentRepository.findById("agent-1")).thenReturn(Optional.of(agent));
        when(checkInRepository.findByManualIdempotencyKey("idem-1")).thenReturn(Optional.of(existing));

        var response = checkInService.createManual("agent-1", request);

        assertThat(response.id()).isEqualTo("check-in-1");
        assertThat(response.manualIdempotencyKey()).isEqualTo("idem-1");
        verify(checkInRepository, never()).save(any(CheckIn.class));
    }

    @Test
    void listByAgentReturnsCheckInsOrderedByMostRecentFirst() {
        Agent agent = buildAgent("agent-1", true);
        CheckIn newest = buildCheckIn(
                agent,
                "idem-newest",
                "check-in-newest",
                Instant.parse("2026-05-23T11:00:00Z"));
        CheckIn oldest = buildCheckIn(
                agent,
                "idem-oldest",
                "check-in-oldest",
                Instant.parse("2026-05-23T10:00:00Z"));

        when(agentRepository.findById("agent-1")).thenReturn(Optional.of(agent));
        when(checkInRepository.findByAgent_IdOrderByOccurredAtDesc("agent-1"))
                .thenReturn(java.util.List.of(newest, oldest));

        var response = checkInService.listByAgent("agent-1");

        assertThat(response.data()).hasSize(2);
        assertThat(response.data().get(0).id()).isEqualTo("check-in-newest");
        assertThat(response.data().get(0).occurredAt()).isEqualTo(Instant.parse("2026-05-23T11:00:00Z"));
        assertThat(response.data().get(1).id()).isEqualTo("check-in-oldest");
    }

    @Test
    void listByAgentRejectsMissingAgent() {
        when(agentRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkInService.listByAgent("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Agent not found.");

        verify(checkInRepository, never()).findByAgent_IdOrderByOccurredAtDesc(any());
    }

    @Test
    void createManualRejectsInactiveAgent() {
        Agent agent = buildAgent("agent-1", false);
        ManualCheckInRequest request = new ManualCheckInRequest(
                CheckInType.CHECKIN,
                BigDecimal.ONE,
                BigDecimal.TEN,
                null,
                null,
                null,
                null,
                null,
                null);

        when(agentRepository.findById("agent-1")).thenReturn(Optional.of(agent));

        assertThatThrownBy(() -> checkInService.createManual("agent-1", request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Agent is inactive.");
    }

    @Test
    void createManualRejectsMissingAgent() {
        ManualCheckInRequest request = new ManualCheckInRequest(
                CheckInType.CHECKIN,
                BigDecimal.ONE,
                BigDecimal.TEN,
                null,
                null,
                null,
                null,
                null,
                null);

        when(agentRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkInService.createManual("missing", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Agent not found.");
    }

    private Agent buildAgent(String id, boolean active) {
        Instant now = Instant.parse("2026-05-23T10:00:00Z");
        return new Agent(
                id,
                "ext-" + id,
                "Agent One",
                AgentRole.TECHNICIAN,
                "Field",
                null,
                null,
                active,
                active ? AgentStatus.ONLINE : AgentStatus.OFFLINE,
                80,
                now,
                null,
                null,
                null,
                null,
                null,
                now,
                now);
    }

    private CheckIn buildCheckIn(Agent agent, String idempotencyKey) {
        return buildCheckIn(
                agent,
                idempotencyKey,
                "check-in-1",
                Instant.parse("2026-05-23T10:00:00Z"));
    }

    private CheckIn buildCheckIn(
            Agent agent,
            String idempotencyKey,
            String id,
            Instant occurredAt) {
        Instant now = Instant.parse("2026-05-23T10:00:00Z");
        return new CheckIn(
                id,
                agent,
                CheckInType.CHECKIN,
                SyncSource.MANUAL,
                BigDecimal.ONE,
                BigDecimal.TEN,
                null,
                null,
                null,
                null,
                null,
                null,
                idempotencyKey,
                occurredAt,
                now,
                now,
                now);
    }
}
