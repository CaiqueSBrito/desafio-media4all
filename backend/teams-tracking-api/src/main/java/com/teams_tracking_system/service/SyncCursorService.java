package com.teams_tracking_system.service;

import com.teams_tracking_system.model.SyncCursor;
import com.teams_tracking_system.model.SyncType;
import com.teams_tracking_system.repositories.SyncCursorRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncCursorService {

    private final SyncCursorRepository syncCursorRepository;

    public SyncCursorService(SyncCursorRepository syncCursorRepository) {
        this.syncCursorRepository = syncCursorRepository;
    }

    @Transactional(readOnly = true)
    public String getLastCursorValue(SyncType syncType) {
        return syncCursorRepository.findBySyncType(syncType)
                .map(SyncCursor::getLastCursorValue)
                .orElse(null);
    }

    @Transactional
    public SyncCursor upsertAfterSuccessfulSync(
            SyncType syncType,
            String lastCursorValue,
            Integer lastPage,
            Instant lastOccurredAt,
            Instant lastSyncedAt) {
        Instant now = Instant.now();

        return syncCursorRepository.findBySyncType(syncType)
                .map(syncCursor -> updateExisting(
                        syncCursor,
                        lastCursorValue,
                        lastPage,
                        lastOccurredAt,
                        lastSyncedAt,
                        now))
                .orElseGet(() -> createNew(
                        syncType,
                        lastCursorValue,
                        lastPage,
                        lastOccurredAt,
                        lastSyncedAt,
                        now));
    }

    private SyncCursor updateExisting(
            SyncCursor syncCursor,
            String lastCursorValue,
            Integer lastPage,
            Instant lastOccurredAt,
            Instant lastSyncedAt,
            Instant now) {
        syncCursor.updateAfterSuccessfulSync(
                lastCursorValue,
                lastPage,
                lastOccurredAt,
                lastSyncedAt,
                now,
                now);

        return syncCursorRepository.save(syncCursor);
    }

    private SyncCursor createNew(
            SyncType syncType,
            String lastCursorValue,
            Integer lastPage,
            Instant lastOccurredAt,
            Instant lastSyncedAt,
            Instant now) {
        SyncCursor syncCursor = new SyncCursor(
                syncType,
                lastCursorValue,
                lastPage,
                lastOccurredAt,
                lastSyncedAt,
                now,
                now,
                now);

        return syncCursorRepository.save(syncCursor);
    }
}
