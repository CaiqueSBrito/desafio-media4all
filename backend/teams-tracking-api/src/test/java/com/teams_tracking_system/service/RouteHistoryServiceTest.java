package com.teams_tracking_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.teams_tracking_system.core.exception.ResourceNotFoundException;
import com.teams_tracking_system.model.Agent;
import com.teams_tracking_system.model.AgentPosition;
import com.teams_tracking_system.model.AgentRole;
import com.teams_tracking_system.model.AgentStatus;
import com.teams_tracking_system.model.CheckIn;
import com.teams_tracking_system.model.CheckInType;
import com.teams_tracking_system.model.SyncSource;
import com.teams_tracking_system.repositories.AgentPositionRepository;
import com.teams_tracking_system.repositories.AgentRepository;
import com.teams_tracking_system.repositories.CheckInRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RouteHistoryServiceTest {

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private AgentPositionRepository agentPositionRepository;

    @Mock
    private CheckInRepository checkInRepository;

    private RouteHistoryService routeHistoryService;

    @BeforeEach
    void setUp() {
        routeHistoryService = new RouteHistoryService(
                agentRepository,
                agentPositionRepository,
                checkInRepository,
                new GeoDistanceService(),
                ZoneOffset.UTC);
    }

    @Test
    void getDailyRouteBuildsOrderedRouteAndCalculatesDistance() {
        Agent agent = agent();
        LocalDate date = LocalDate.of(2026, 5, 22);
        Instant start = Instant.parse("2026-05-22T00:00:00Z");
        Instant end = Instant.parse("2026-05-23T00:00:00Z");
        AgentPosition firstPosition = position(
                agent,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Instant.parse("2026-05-22T10:00:00Z"));
        AgentPosition lastPosition = position(
                agent,
                BigDecimal.ZERO,
                new BigDecimal("2"),
                Instant.parse("2026-05-22T11:00:00Z"));
        CheckIn manualCheckIn = checkIn(
                agent,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                Instant.parse("2026-05-22T10:30:00Z"),
                SyncSource.MANUAL);

        when(agentRepository.findById("agent-1")).thenReturn(Optional.of(agent));
        when(agentPositionRepository.findByAgent_IdAndLastSeenGreaterThanEqualAndLastSeenLessThanOrderByLastSeenAsc(
                "agent-1",
                start,
                end))
                .thenReturn(List.of(firstPosition, lastPosition));
        when(checkInRepository.findByAgent_IdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAsc(
                "agent-1",
                start,
                end))
                .thenReturn(List.of(manualCheckIn));

        var response = routeHistoryService.getDailyRoute("agent-1", date);

        assertThat(response.agentId()).isEqualTo("agent-1");
        assertThat(response.agentName()).isEqualTo("Agent One");
        assertThat(response.date()).isEqualTo(date);
        assertThat(response.points()).hasSize(3);
        assertThat(response.points().get(0).timestamp()).isEqualTo(Instant.parse("2026-05-22T10:00:00Z"));
        assertThat(response.points().get(0).source()).isEqualTo(SyncSource.GPS_SYNC);
        assertThat(response.points().get(1).timestamp()).isEqualTo(Instant.parse("2026-05-22T10:30:00Z"));
        assertThat(response.points().get(1).source()).isEqualTo(SyncSource.MANUAL);
        assertThat(response.points().get(2).timestamp()).isEqualTo(Instant.parse("2026-05-22T11:00:00Z"));
        assertThat(response.totalDistanceMeters()).isEqualByComparingTo(new BigDecimal("222389.86"));
    }

    @Test
    void getDailyRouteIgnoresCheckInsWithoutCoordinates() {
        Agent agent = agent();
        LocalDate date = LocalDate.of(2026, 5, 22);
        Instant start = Instant.parse("2026-05-22T00:00:00Z");
        Instant end = Instant.parse("2026-05-23T00:00:00Z");
        CheckIn checkInWithoutCoordinates = checkIn(
                agent,
                null,
                null,
                Instant.parse("2026-05-22T10:30:00Z"),
                SyncSource.MANUAL);

        when(agentRepository.findById("agent-1")).thenReturn(Optional.of(agent));
        when(agentPositionRepository.findByAgent_IdAndLastSeenGreaterThanEqualAndLastSeenLessThanOrderByLastSeenAsc(
                "agent-1",
                start,
                end))
                .thenReturn(List.of());
        when(checkInRepository.findByAgent_IdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAsc(
                "agent-1",
                start,
                end))
                .thenReturn(List.of(checkInWithoutCoordinates));

        var response = routeHistoryService.getDailyRoute("agent-1", date);

        assertThat(response.points()).isEmpty();
        assertThat(response.totalDistanceMeters()).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    void getDailyRouteRejectsMissingAgent() {
        when(agentRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeHistoryService.getDailyRoute("missing", LocalDate.of(2026, 5, 22)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Agent not found.");
    }

    private Agent agent() {
        Instant now = Instant.parse("2026-05-22T09:00:00Z");
        return new Agent(
                "agent-1",
                "ext-agent-1",
                "Agent One",
                AgentRole.TECHNICIAN,
                "Field",
                null,
                null,
                true,
                AgentStatus.ONLINE,
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

    private AgentPosition position(
            Agent agent,
            BigDecimal latitude,
            BigDecimal longitude,
            Instant lastSeen) {
        return new AgentPosition(
                agent,
                latitude,
                longitude,
                "address",
                BigDecimal.ONE,
                BigDecimal.ZERO,
                80,
                AgentStatus.ONLINE,
                lastSeen);
    }

    private CheckIn checkIn(
            Agent agent,
            BigDecimal latitude,
            BigDecimal longitude,
            Instant occurredAt,
            SyncSource source) {
        return new CheckIn(
                "check-in-1",
                agent,
                CheckInType.CHECKIN,
                source,
                latitude,
                longitude,
                "address",
                BigDecimal.ONE,
                BigDecimal.ZERO,
                null,
                null,
                null,
                "manual-key-1",
                occurredAt,
                occurredAt,
                occurredAt,
                occurredAt);
    }
}
