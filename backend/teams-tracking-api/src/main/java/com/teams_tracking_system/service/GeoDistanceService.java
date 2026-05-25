package com.teams_tracking_system.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class GeoDistanceService {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    public BigDecimal calculateHaversineDistanceMeters(
            BigDecimal latitudeFrom,
            BigDecimal longitudeFrom,
            BigDecimal latitudeTo,
            BigDecimal longitudeTo) {
        if (latitudeFrom == null || longitudeFrom == null || latitudeTo == null || longitudeTo == null) {
            throw new IllegalArgumentException("coordinates must not be null");
        }

        double fromLat = Math.toRadians(latitudeFrom.doubleValue());
        double toLat = Math.toRadians(latitudeTo.doubleValue());
        double deltaLat = Math.toRadians(latitudeTo.subtract(latitudeFrom).doubleValue());
        double deltaLon = Math.toRadians(longitudeTo.subtract(longitudeFrom).doubleValue());

        double halfChordLength = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(fromLat) * Math.cos(toLat)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double angularDistance = 2 * Math.atan2(Math.sqrt(halfChordLength), Math.sqrt(1 - halfChordLength));

        return BigDecimal.valueOf(EARTH_RADIUS_METERS * angularDistance)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
