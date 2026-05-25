package com.teams_tracking_system.service;

import com.teams_tracking_system.core.exception.ResourceNotFoundException;
import com.teams_tracking_system.dtos.schemas.RouteHistoryResponse;
import com.teams_tracking_system.dtos.schemas.RoutePointResponse;
import com.teams_tracking_system.model.Agent;
import com.teams_tracking_system.model.AgentPosition;
import com.teams_tracking_system.model.CheckIn;
import com.teams_tracking_system.model.RouteHistory;
import com.teams_tracking_system.model.SyncSource;
import com.teams_tracking_system.repositories.AgentPositionRepository;
import com.teams_tracking_system.repositories.AgentRepository;
import com.teams_tracking_system.repositories.CheckInRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RouteHistoryService {

    private final AgentRepository agentRepository;
    private final AgentPositionRepository agentPositionRepository;
    private final CheckInRepository checkInRepository;
    private final GeoDistanceService geoDistanceService;
    private final ZoneId routeZone;

    @Autowired
    public RouteHistoryService(
            AgentRepository agentRepository,
            AgentPositionRepository agentPositionRepository,
            CheckInRepository checkInRepository,
            GeoDistanceService geoDistanceService) {
        this(
                agentRepository,
                agentPositionRepository,
                checkInRepository,
                geoDistanceService,
                ZoneId.systemDefault());
    }

    RouteHistoryService(
            AgentRepository agentRepository,
            AgentPositionRepository agentPositionRepository,
            CheckInRepository checkInRepository,
            GeoDistanceService geoDistanceService,
            ZoneId routeZone) {
        this.agentRepository = agentRepository;
        this.agentPositionRepository = agentPositionRepository;
        this.checkInRepository = checkInRepository;
        this.geoDistanceService = geoDistanceService;
        this.routeZone = routeZone;
    }

    @Transactional(readOnly = true)
    public RouteHistoryResponse getDailyRoute(String agentId, LocalDate date) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Agent not found.",
                        "id not found: " + agentId));

        Instant startInclusive = date.atStartOfDay(routeZone).toInstant();
        Instant endExclusive = date.plusDays(1).atStartOfDay(routeZone).toInstant();
        List<RouteHistory.RoutePoint> points = findRoutePoints(agentId, startInclusive, endExclusive);

        RouteHistory routeHistory = new RouteHistory(
                agent.getId(),
                agent.getName(),
                date,
                calculateTotalDistanceMeters(points),
                points);

        return toResponse(routeHistory);
    }

    private List<RouteHistory.RoutePoint> findRoutePoints(
            String agentId,
            Instant startInclusive,
            Instant endExclusive) {
        List<RouteHistory.RoutePoint> points = new ArrayList<>();

        agentPositionRepository
                .findByAgent_IdAndLastSeenGreaterThanEqualAndLastSeenLessThanOrderByLastSeenAsc(
                        agentId,
                        startInclusive,
                        endExclusive)
                .stream()
                .map(this::fromAgentPosition)
                .forEach(points::add);

        checkInRepository
                .findByAgent_IdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAsc(
                        agentId,
                        startInclusive,
                        endExclusive)
                .stream()
                .filter(checkIn -> checkIn.getLatitude() != null && checkIn.getLongitude() != null)
                .map(this::fromCheckIn)
                .forEach(points::add);

        points.sort(Comparator
                .comparing(RouteHistory.RoutePoint::getTimestamp)
                .thenComparing(point -> point.getSource().name()));
        return points;
    }

    private RouteHistory.RoutePoint fromAgentPosition(AgentPosition position) {
        return new RouteHistory.RoutePoint(
                position.getLatitude(),
                position.getLongitude(),
                position.getAccuracy(),
                position.getSpeed(),
                position.getCurrentAddress(),
                position.getLastSeen(),
                SyncSource.GPS_SYNC);
    }

    private RouteHistory.RoutePoint fromCheckIn(CheckIn checkIn) {
        return new RouteHistory.RoutePoint(
                checkIn.getLatitude(),
                checkIn.getLongitude(),
                checkIn.getAccuracy(),
                checkIn.getSpeed(),
                checkIn.getAddress(),
                checkIn.getOccurredAt(),
                checkIn.getSource());
    }

    private BigDecimal calculateTotalDistanceMeters(List<RouteHistory.RoutePoint> points) {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (int index = 1; index < points.size(); index++) {
            RouteHistory.RoutePoint previous = points.get(index - 1);
            RouteHistory.RoutePoint current = points.get(index);
            total = total.add(geoDistanceService.calculateHaversineDistanceMeters(
                    previous.getLatitude(),
                    previous.getLongitude(),
                    current.getLatitude(),
                    current.getLongitude()));
        }
        return total;
    }

    private RouteHistoryResponse toResponse(RouteHistory routeHistory) {
        return new RouteHistoryResponse(
                routeHistory.getAgentId(),
                routeHistory.getAgentName(),
                routeHistory.getDate(),
                routeHistory.getTotalDistanceMeters(),
                routeHistory.getPoints()
                        .stream()
                        .map(this::toPointResponse)
                        .toList());
    }

    private RoutePointResponse toPointResponse(RouteHistory.RoutePoint point) {
        return new RoutePointResponse(
                point.getLatitude(),
                point.getLongitude(),
                point.getAccuracy(),
                point.getSpeed(),
                point.getAddress(),
                point.getTimestamp(),
                point.getSource());
    }
}
