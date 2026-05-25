package com.teams_tracking_system.repositories;

import com.teams_tracking_system.model.Geofence;
import com.teams_tracking_system.model.GeofenceType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeofenceRepository extends JpaRepository<Geofence, String> {

    Optional<Geofence> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    List<Geofence> findByType(GeofenceType type);

    List<Geofence> findBySyncedAtAfterOrderBySyncedAtAsc(Instant syncedAt);
}
