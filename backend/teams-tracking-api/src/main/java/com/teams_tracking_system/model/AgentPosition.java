package com.teams_tracking_system.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "agent_positions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_agent_positions_snapshot",
                columnNames = {"agent_id", "last_seen", "latitude", "longitude"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Column(name = "latitude", nullable = false)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false)
    private BigDecimal longitude;

    @Column(name = "current_address", length = 500)
    private String currentAddress;

    @Column(name = "accuracy")
    private BigDecimal accuracy;

    @Column(name = "speed")
    private BigDecimal speed;

    @Column(name = "battery")
    private Integer battery;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AgentStatus status;

    @Column(name = "last_seen", nullable = false)
    private Instant lastSeen;

    public AgentPosition(
            Agent agent,
            BigDecimal latitude,
            BigDecimal longitude,
            String currentAddress,
            BigDecimal accuracy,
            BigDecimal speed,
            Integer battery,
            AgentStatus status,
            Instant lastSeen) {
        this.agent = agent;
        this.latitude = latitude;
        this.longitude = longitude;
        this.currentAddress = currentAddress;
        this.accuracy = accuracy;
        this.speed = speed;
        this.battery = battery;
        this.status = status;
        this.lastSeen = lastSeen;
    }

    public void updateSnapshot(
            BigDecimal latitude,
            BigDecimal longitude,
            String currentAddress,
            BigDecimal accuracy,
            BigDecimal speed,
            Integer battery,
            AgentStatus status,
            Instant lastSeen) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.currentAddress = currentAddress;
        this.accuracy = accuracy;
        this.speed = speed;
        this.battery = battery;
        this.status = status;
        this.lastSeen = lastSeen;
    }
}
