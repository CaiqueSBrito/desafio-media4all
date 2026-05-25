package com.teams_tracking_system.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;

@Getter
public class RouteHistory {

    private final String agentId;
    private final String agentName;
    private final LocalDate date;
    private final BigDecimal totalDistanceMeters;
    private final List<RoutePoint> points;

    public RouteHistory(
            String agentId,
            String agentName,
            LocalDate date,
            BigDecimal totalDistanceMeters,
            List<RoutePoint> points) {
        this.agentId = agentId;
        this.agentName = agentName;
        this.date = date;
        this.totalDistanceMeters = totalDistanceMeters;
        this.points = List.copyOf(points);
    }

    @Getter
    public static class RoutePoint {

        private final BigDecimal latitude;
        private final BigDecimal longitude;
        private final BigDecimal accuracy;
        private final BigDecimal speed;
        private final String address;
        private final Instant timestamp;
        private final SyncSource source;

        public RoutePoint(
                BigDecimal latitude,
                BigDecimal longitude,
                BigDecimal accuracy,
                BigDecimal speed,
                String address,
                Instant timestamp,
                SyncSource source) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.accuracy = accuracy;
            this.speed = speed;
            this.address = address;
            this.timestamp = timestamp;
            this.source = source;
        }
    }
}
