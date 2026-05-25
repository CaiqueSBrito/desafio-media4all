package com.teams_tracking_system.repositories;

import com.teams_tracking_system.model.AgentPosition;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentPositionRepository extends JpaRepository<AgentPosition, Long> {

    Optional<AgentPosition> findFirstByAgent_IdOrderByLastSeenDesc(String agentId);

    boolean existsByAgent_IdAndLastSeenAndLatitudeAndLongitude(
            String agentId,
            Instant lastSeen,
            BigDecimal latitude,
            BigDecimal longitude);

    List<AgentPosition> findByAgent_IdOrderByLastSeenAsc(String agentId);

    List<AgentPosition> findByAgent_IdAndLastSeenBetweenOrderByLastSeenAsc(
            String agentId,
            Instant start,
            Instant end);

    List<AgentPosition> findByAgent_IdAndLastSeenGreaterThanEqualAndLastSeenLessThanOrderByLastSeenAsc(
            String agentId,
            Instant startInclusive,
            Instant endExclusive);

    List<AgentPosition> findByLastSeenBetweenOrderByLastSeenAsc(
            Instant start,
            Instant end);
}
