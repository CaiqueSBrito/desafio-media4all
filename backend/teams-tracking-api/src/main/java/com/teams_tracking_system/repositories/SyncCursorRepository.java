package com.teams_tracking_system.repositories;

import com.teams_tracking_system.model.SyncCursor;
import com.teams_tracking_system.model.SyncType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncCursorRepository extends JpaRepository<SyncCursor, Long> {

    List<SyncCursor> findAllByOrderBySyncTypeAsc();

    Optional<SyncCursor> findBySyncType(SyncType syncType);

    boolean existsBySyncType(SyncType syncType);
}
