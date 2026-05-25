package com.teams_tracking_system.repositories;

import com.teams_tracking_system.model.CheckIn;
import com.teams_tracking_system.model.CheckInType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckInRepository extends JpaRepository<CheckIn, String> {

    Optional<CheckIn> findByExternalEventId(String externalEventId);

    Optional<CheckIn> findByManualIdempotencyKey(String manualIdempotencyKey);

    boolean existsByExternalEventId(String externalEventId);

    List<CheckIn> findByAgent_IdOrderByOccurredAtDesc(String agentId);

    List<CheckIn> findByAgent_IdAndOccurredAtBetweenOrderByOccurredAtAsc(
            String agentId,
            Instant start,
            Instant end);

    List<CheckIn> findByAgent_IdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAsc(
            String agentId,
            Instant startInclusive,
            Instant endExclusive);

    List<CheckIn> findByTypeAndOccurredAtBetweenOrderByOccurredAtAsc(
            CheckInType type,
            Instant start,
            Instant end);
}
