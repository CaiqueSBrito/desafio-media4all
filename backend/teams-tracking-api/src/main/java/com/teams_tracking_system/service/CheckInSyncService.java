package com.teams_tracking_system.service;

import com.teams_tracking_system.dtos.schemas.CheckInSyncResultResponse;
import com.teams_tracking_system.integrations.ExternalGpsApiClient;
import com.teams_tracking_system.integrations.ExternalGpsRetryStats;
import com.teams_tracking_system.integrations.dtos.ExternalCheckInSyncResponse;
import com.teams_tracking_system.model.SyncType;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckInSyncService {

    private final ExternalGpsApiClient externalGpsApiClient;
    private final SyncExecutionService syncExecutionService;
    private final SyncCursorService syncCursorService;

    public CheckInSyncService(
            ExternalGpsApiClient externalGpsApiClient,
            SyncExecutionService syncExecutionService,
            SyncCursorService syncCursorService) {
        this.externalGpsApiClient = externalGpsApiClient;
        this.syncExecutionService = syncExecutionService;
        this.syncCursorService = syncCursorService;
    }

    @Transactional
    public CheckInSyncResultResponse syncCheckIns() {
        String previousSyncToken = syncCursorService.getLastCursorValue(SyncType.CHECK_INS);
        Long executionId = syncExecutionService.start(SyncType.CHECK_INS, previousSyncToken);

        try {
            // The external OpenAPI exposes syncToken only in the response, not as input.
            ExternalCheckInSyncResponse externalResponse = externalGpsApiClient.syncCheckIns();
            String syncToken = validateAndGetSyncToken(externalResponse);
            int synced = resolveSyncedCount(externalResponse);
            Instant now = Instant.now();

            syncCursorService.upsertAfterSuccessfulSync(
                    SyncType.CHECK_INS,
                    syncToken,
                    null,
                    null,
                    now);

            ExternalGpsRetryStats retryStats = resolveRetryStats();
            syncExecutionService.finishSuccessfully(
                    executionId,
                    synced,
                    synced,
                    0,
                    0,
                    0,
                    retryStats.getRetryAttempts(),
                    retryStats.getRateLimitErrors(),
                    retryStats.getServiceUnavailableErrors(),
                    syncToken);

            return new CheckInSyncResultResponse(synced, previousSyncToken, syncToken);
        } catch (RuntimeException exception) {
            ExternalGpsRetryStats retryStats = resolveRetryStats();
            syncExecutionService.fail(
                    executionId,
                    exception,
                    retryStats.getRetryAttempts(),
                    retryStats.getRateLimitErrors(),
                    retryStats.getServiceUnavailableErrors());
            throw exception;
        }
    }

    private String validateAndGetSyncToken(ExternalCheckInSyncResponse externalResponse) {
        if (externalResponse == null || externalResponse.syncToken() == null || externalResponse.syncToken().isBlank()) {
            throw new IllegalStateException("External check-in sync response is missing syncToken.");
        }

        return externalResponse.syncToken();
    }

    private int resolveSyncedCount(ExternalCheckInSyncResponse externalResponse) {
        if (externalResponse.synced() == null) {
            return 0;
        }
        if (externalResponse.synced() < 0) {
            throw new IllegalStateException("External check-in sync response has negative synced count.");
        }

        return externalResponse.synced();
    }

    private ExternalGpsRetryStats resolveRetryStats() {
        ExternalGpsRetryStats retryStats = externalGpsApiClient.getLastRetryStats();
        return retryStats != null ? retryStats : new ExternalGpsRetryStats();
    }
}
