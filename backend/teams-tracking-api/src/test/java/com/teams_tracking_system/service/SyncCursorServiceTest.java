package com.teams_tracking_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teams_tracking_system.model.SyncCursor;
import com.teams_tracking_system.model.SyncType;
import com.teams_tracking_system.repositories.SyncCursorRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SyncCursorServiceTest {

    @Mock
    private SyncCursorRepository syncCursorRepository;

    @InjectMocks
    private SyncCursorService syncCursorService;

    @Test
    void getLastCursorValueReturnsPersistedCursor() {
        SyncCursor syncCursor = new SyncCursor(
                SyncType.CHECK_INS,
                "token-before",
                null,
                null,
                Instant.parse("2026-05-23T10:00:00Z"),
                Instant.parse("2026-05-23T10:00:00Z"),
                Instant.parse("2026-05-23T10:00:00Z"),
                Instant.parse("2026-05-23T10:00:00Z"));
        when(syncCursorRepository.findBySyncType(SyncType.CHECK_INS)).thenReturn(Optional.of(syncCursor));

        String cursorValue = syncCursorService.getLastCursorValue(SyncType.CHECK_INS);

        assertThat(cursorValue).isEqualTo("token-before");
    }

    @Test
    void upsertAfterSuccessfulSyncCreatesCursorWhenMissing() {
        Instant lastSyncedAt = Instant.parse("2026-05-23T11:00:00Z");
        when(syncCursorRepository.findBySyncType(SyncType.CHECK_INS)).thenReturn(Optional.empty());
        when(syncCursorRepository.save(any(SyncCursor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SyncCursor syncCursor = syncCursorService.upsertAfterSuccessfulSync(
                SyncType.CHECK_INS,
                "token-after",
                null,
                null,
                lastSyncedAt);

        assertThat(syncCursor.getSyncType()).isEqualTo(SyncType.CHECK_INS);
        assertThat(syncCursor.getLastCursorValue()).isEqualTo("token-after");
        assertThat(syncCursor.getLastSyncedAt()).isEqualTo(lastSyncedAt);
        assertThat(syncCursor.getLastSuccessfulSyncAt()).isNotNull();

        ArgumentCaptor<SyncCursor> captor = ArgumentCaptor.forClass(SyncCursor.class);
        verify(syncCursorRepository).save(captor.capture());
        assertThat(captor.getValue().getLastCursorValue()).isEqualTo("token-after");
    }

    @Test
    void upsertAfterSuccessfulSyncUpdatesExistingCursor() {
        Instant createdAt = Instant.parse("2026-05-23T10:00:00Z");
        SyncCursor existingCursor = new SyncCursor(
                SyncType.AGENTS,
                "2026-05-23T09:00:00Z",
                null,
                Instant.parse("2026-05-23T09:00:00Z"),
                createdAt,
                createdAt,
                createdAt,
                createdAt);
        Instant lastOccurredAt = Instant.parse("2026-05-23T11:00:00Z");
        Instant lastSyncedAt = Instant.parse("2026-05-23T11:01:00Z");
        when(syncCursorRepository.findBySyncType(SyncType.AGENTS)).thenReturn(Optional.of(existingCursor));
        when(syncCursorRepository.save(existingCursor)).thenReturn(existingCursor);

        SyncCursor syncCursor = syncCursorService.upsertAfterSuccessfulSync(
                SyncType.AGENTS,
                "2026-05-23T11:00:00Z",
                null,
                lastOccurredAt,
                lastSyncedAt);

        assertThat(syncCursor.getLastCursorValue()).isEqualTo("2026-05-23T11:00:00Z");
        assertThat(syncCursor.getLastOccurredAt()).isEqualTo(lastOccurredAt);
        assertThat(syncCursor.getLastSyncedAt()).isEqualTo(lastSyncedAt);
        assertThat(syncCursor.getCreatedAt()).isEqualTo(createdAt);
        assertThat(syncCursor.getUpdatedAt()).isAfterOrEqualTo(lastSyncedAt);
        verify(syncCursorRepository).save(existingCursor);
    }
}
