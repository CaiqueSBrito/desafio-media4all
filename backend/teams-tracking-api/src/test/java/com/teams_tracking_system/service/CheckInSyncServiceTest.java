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
import com.teams_tracking_system.integrations.ExternalGpsRetryStats;
import com.teams_tracking_system.integrations.dtos.ExternalCheckInSyncResponse;
import com.teams_tracking_system.model.SyncType;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckInSyncServiceTest {

    @Mock
    private ExternalGpsApiClient externalGpsApiClient;

    @Mock
    private SyncExecutionService syncExecutionService;

    @Mock
    private SyncCursorService syncCursorService;

    @InjectMocks
    private CheckInSyncService checkInSyncService;

    @Test
    void syncCheckInsPersistsReturnedTokenAndFinishesExecution() {
        ExternalGpsRetryStats retryStats = new ExternalGpsRetryStats();
        retryStats.recordRetry(429);

        when(syncCursorService.getLastCursorValue(SyncType.CHECK_INS)).thenReturn("token-before");
        when(syncExecutionService.start(SyncType.CHECK_INS, "token-before")).thenReturn(20L);
        when(externalGpsApiClient.syncCheckIns())
                .thenReturn(new ExternalCheckInSyncResponse(3, "token-after"));
        when(externalGpsApiClient.getLastRetryStats()).thenReturn(retryStats);

        var response = checkInSyncService.syncCheckIns();

        assertThat(response.synced()).isEqualTo(3);
        assertThat(response.previousSyncToken()).isEqualTo("token-before");
        assertThat(response.syncToken()).isEqualTo("token-after");
        verify(syncCursorService).upsertAfterSuccessfulSync(
                eq(SyncType.CHECK_INS),
                eq("token-after"),
                isNull(),
                isNull(),
                any(Instant.class));
        verify(syncExecutionService).finishSuccessfully(20L, 3, 3, 0, 0, 0, 1, 1, 0, "token-after");
    }

    @Test
    void syncCheckInsDoesNotAdvanceCursorWhenExternalCallFails() {
        RuntimeException exception = new RuntimeException("external unavailable");

        when(syncCursorService.getLastCursorValue(SyncType.CHECK_INS)).thenReturn("token-before");
        when(syncExecutionService.start(SyncType.CHECK_INS, "token-before")).thenReturn(21L);
        when(externalGpsApiClient.syncCheckIns()).thenThrow(exception);

        assertThatThrownBy(() -> checkInSyncService.syncCheckIns())
                .isSameAs(exception);

        verify(syncCursorService, never()).upsertAfterSuccessfulSync(any(), any(), any(), any(), any());
        verify(syncExecutionService).fail(21L, exception, 0, 0, 0);
    }

    @Test
    void syncCheckInsDoesNotAdvanceCursorWhenResponseHasNoSyncToken() {
        when(syncCursorService.getLastCursorValue(SyncType.CHECK_INS)).thenReturn("token-before");
        when(syncExecutionService.start(SyncType.CHECK_INS, "token-before")).thenReturn(22L);
        when(externalGpsApiClient.syncCheckIns())
                .thenReturn(new ExternalCheckInSyncResponse(1, null));

        assertThatThrownBy(() -> checkInSyncService.syncCheckIns())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("External check-in sync response is missing syncToken.");

        verify(syncCursorService, never()).upsertAfterSuccessfulSync(any(), any(), any(), any(), any());
        verify(syncExecutionService).fail(eq(22L), any(IllegalStateException.class), eq(0), eq(0), eq(0));
    }
}
