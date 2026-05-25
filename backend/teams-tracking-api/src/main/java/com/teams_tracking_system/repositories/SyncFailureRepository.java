package com.teams_tracking_system.repositories;

import com.teams_tracking_system.model.SyncFailure;
import com.teams_tracking_system.model.SyncType;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncFailureRepository extends JpaRepository<SyncFailure, Long> {

    List<SyncFailure> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<SyncFailure> findBySyncExecution_IdOrderByCreatedAtAsc(Long syncExecutionId);

    List<SyncFailure> findBySyncTypeOrderByCreatedAtDesc(SyncType syncType);
}
