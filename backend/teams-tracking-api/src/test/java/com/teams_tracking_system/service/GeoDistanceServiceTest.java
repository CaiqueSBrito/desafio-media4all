package com.teams_tracking_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class GeoDistanceServiceTest {

    private final GeoDistanceService geoDistanceService = new GeoDistanceService();

    @Test
    void calculateHaversineDistanceMetersReturnsZeroForSamePoint() {
        BigDecimal distance = geoDistanceService.calculateHaversineDistanceMeters(
                new BigDecimal("-23.5505000"),
                new BigDecimal("-46.6333000"),
                new BigDecimal("-23.5505000"),
                new BigDecimal("-46.6333000"));

        assertThat(distance).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    void calculateHaversineDistanceMetersUsesEarthGreatCircleDistance() {
        BigDecimal distance = geoDistanceService.calculateHaversineDistanceMeters(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ONE);

        assertThat(distance).isEqualByComparingTo(new BigDecimal("111194.93"));
    }

    @Test
    void calculateHaversineDistanceMetersRejectsNullCoordinates() {
        assertThatThrownBy(() -> geoDistanceService.calculateHaversineDistanceMeters(
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("coordinates must not be null");
    }
}
