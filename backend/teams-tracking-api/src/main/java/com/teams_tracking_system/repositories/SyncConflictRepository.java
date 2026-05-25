package com.teams_tracking_system.repositories;

import com.teams_tracking_system.model.SyncConflict;
import com.teams_tracking_system.model.SyncType;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncConflictRepository extends JpaRepository<SyncConflict, Long> {

    List<SyncConflict> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<SyncConflict> findBySyncTypeOrderByCreatedAtDesc(SyncType syncType);
}
