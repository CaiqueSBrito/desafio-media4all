package com.teams_tracking_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teams_tracking_system.integrations.ExternalGpsApiClient;
import com.teams_tracking_system.integrations.dtos.ExternalGeofenceListResponse;
import com.teams_tracking_system.integrations.dtos.ExternalGeofenceResponse;
import com.teams_tracking_system.integrations.dtos.ExternalGeofenceType;
import com.teams_tracking_system.model.Geofence;
import com.teams_tracking_system.model.GeofenceType;
import com.teams_tracking_system.model.SyncType;
import com.teams_tracking_system.repositories.GeofenceRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GeofenceSyncServiceTest {

    private static final Instant GEOFENCE_SYNCED_AT = Instant.parse("2026-05-23T02:35:40.532Z");
    private static final String GEOFENCE_CURSOR = "2026-05-23T02:35:40.532Z";

    @Mock
    private ExternalGpsApiClient externalGpsApiClient;

    @Mock
    private GeofenceRepository geofenceRepository;

    @Mock
    private SyncExecutionService syncExecutionService;

    @Mock
    private SyncFailureService syncFailureService;

    @Mock
    private SyncCursorService syncCursorService;

    @Mock
    private SyncConflictService syncConflictService;

    @InjectMocks
    private GeofenceSyncService geofenceSyncService;

    @Test
    void syncGeofencesCreatesMissingGeofencesUsingExternalIdForIdempotency() {
        ExternalGeofenceResponse externalGeofence = externalGeofence(
                "seed_geo_001",
                "ext-geo-001",
                ExternalGeofenceType.CIRCLE);

        when(syncExecutionService.start(eq(SyncType.GEOFENCES), isNull())).thenReturn(40L);
        when(externalGpsApiClient.findGeofences())
                .thenReturn(new ExternalGeofenceListResponse(List.of(externalGeofence)));
        when(geofenceRepository.findByExternalId("ext-geo-001")).thenReturn(Optional.empty());
        when(geofenceRepository.save(any(Geofence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = geofenceSyncService.syncGeofences();

        assertThat(result.read()).isEqualTo(1);
        assertThat(result.created()).isEqualTo(1);
        assertThat(result.updated()).isZero();
        assertThat(result.skipped()).isZero();

        ArgumentCaptor<Geofence> geofenceCaptor = ArgumentCaptor.forClass(Geofence.class);
        verify(geofenceRepository).save(geofenceCaptor.capture());
        Geofence savedGeofence = geofenceCaptor.getValue();
        assertThat(savedGeofence.getId()).isEqualTo("seed_geo_001");
        assertThat(savedGeofence.getExternalId()).isEqualTo("ext-geo-001");
        assertThat(savedGeofence.getType()).isEqualTo(GeofenceType.CIRCLE);
        assertThat(savedGeofence.getCoordinatesJson()).isEqualTo("{\"lat\":-23.55,\"lng\":-46.63,\"radius\":100}");
        assertThat(savedGeofence.isAlertOnEnter()).isTrue();
        assertThat(savedGeofence.isAlertOnExit()).isTrue();

        verify(syncCursorService).upsertAfterSuccessfulSync(
                eq(SyncType.GEOFENCES),
                eq(GEOFENCE_CURSOR),
                isNull(),
                eq(GEOFENCE_SYNCED_AT),
                any(Instant.class));
        verify(syncExecutionService).finishSuccessfully(40L, 1, 1, 0, 0, 0, 0, 0, 0, GEOFENCE_CURSOR);
    }

    @Test
    void syncGeofencesUpdatesExistingGeofences() {
        ExternalGeofenceResponse externalGeofence = externalGeofence(
                "seed_geo_001",
                "ext-geo-001",
                ExternalGeofenceType.POLYGON);
        Geofence existingGeofence = new Geofence(
                "seed_geo_001",
                "ext-geo-001",
                "Old Geofence",
                GeofenceType.CIRCLE,
                "{\"lat\":0,\"lng\":0,\"radius\":10}",
                false,
                false,
                "Old Team",
                Instant.parse("2026-05-22T00:00:00Z"));

        when(syncExecutionService.start(eq(SyncType.GEOFENCES), isNull())).thenReturn(41L);
        when(externalGpsApiClient.findGeofences())
                .thenReturn(new ExternalGeofenceListResponse(List.of(externalGeofence)));
        when(geofenceRepository.findByExternalId("ext-geo-001")).thenReturn(Optional.of(existingGeofence));
        when(geofenceRepository.save(any(Geofence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = geofenceSyncService.syncGeofences();

        assertThat(result.read()).isEqualTo(1);
        assertThat(result.created()).isZero();
        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.skipped()).isZero();
        assertThat(existingGeofence.getName()).isEqualTo("Area Operacional");
        assertThat(existingGeofence.getType()).isEqualTo(GeofenceType.POLYGON);
        assertThat(existingGeofence.getAssignedTeams()).isEqualTo("Alpha,Beta");
        verify(geofenceRepository).save(existingGeofence);
        verify(syncExecutionService).finishSuccessfully(41L, 1, 0, 1, 0, 0, 0, 0, 0, GEOFENCE_CURSOR);
    }

    @Test
    void syncGeofencesSkipsInvalidPayloadsAndRecordsFailure() {
        ExternalGeofenceResponse invalidGeofence = new ExternalGeofenceResponse(
                "seed_geo_001",
                null,
                "Area Operacional",
                ExternalGeofenceType.CIRCLE,
                "{\"lat\":-23.55,\"lng\":-46.63,\"radius\":100}",
                true,
                true,
                "Alpha,Beta",
                GEOFENCE_SYNCED_AT);

        when(syncExecutionService.start(eq(SyncType.GEOFENCES), isNull())).thenReturn(42L);
        when(externalGpsApiClient.findGeofences())
                .thenReturn(new ExternalGeofenceListResponse(List.of(invalidGeofence)));

        var result = geofenceSyncService.syncGeofences();

        assertThat(result.read()).isEqualTo(1);
        assertThat(result.created()).isZero();
        assertThat(result.updated()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
        verify(geofenceRepository, never()).save(any(Geofence.class));
        verify(syncFailureService).recordInvalidPayload(
                42L,
                SyncType.GEOFENCES,
                "geofence",
                "geofence.externalId is required",
                invalidGeofence);
        verify(syncExecutionService).finishWithWarning(
                42L,
                1,
                0,
                0,
                0,
                1,
                0,
                0,
                0,
                null,
                "Geofence sync finished with 1 invalid payload(s).");
    }

    @Test
    void syncGeofencesDoesNotAdvanceCursorWhenExternalCallFails() {
        RuntimeException exception = new RuntimeException("external unavailable");

        when(syncCursorService.getLastCursorValue(SyncType.GEOFENCES)).thenReturn("cursor-before");
        when(syncExecutionService.start(SyncType.GEOFENCES, "cursor-before")).thenReturn(43L);
        when(externalGpsApiClient.findGeofences()).thenThrow(exception);

        assertThatThrownBy(geofenceSyncService::syncGeofences)
                .isSameAs(exception);

        verify(syncCursorService, never()).upsertAfterSuccessfulSync(any(), any(), any(), any(), any());
        verify(syncExecutionService).fail(43L, exception, 0, 0, 0);
    }

    private ExternalGeofenceResponse externalGeofence(
            String id,
            String externalId,
            ExternalGeofenceType type) {
        return new ExternalGeofenceResponse(
                id,
                externalId,
                "Area Operacional",
                type,
                "{\"lat\":-23.55,\"lng\":-46.63,\"radius\":100}",
                true,
                true,
                "Alpha,Beta",
                GEOFENCE_SYNCED_AT);
    }
}
