package com.teams_tracking_system.repositories;

import com.teams_tracking_system.model.SyncExecution;
import com.teams_tracking_system.model.SyncExecutionStatus;
import com.teams_tracking_system.model.SyncType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncExecutionRepository extends JpaRepository<SyncExecution, Long> {

    Optional<SyncExecution> findFirstBySyncTypeOrderByStartedAtDesc(SyncType syncType);

    List<SyncExecution> findAllByOrderByStartedAtDesc(Pageable pageable);

    List<SyncExecution> findBySyncTypeOrderByStartedAtDesc(SyncType syncType);

    List<SyncExecution> findByStatusOrderByStartedAtDesc(SyncExecutionStatus status);

    List<SyncExecution> findByStartedAtBetweenOrderByStartedAtDesc(
            Instant start,
            Instant end);
}
