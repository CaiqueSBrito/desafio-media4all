package com.teams_tracking_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.teams_tracking_system.model.SyncCursor;
import com.teams_tracking_system.model.SyncExecution;
import com.teams_tracking_system.model.SyncExecutionStatus;
import com.teams_tracking_system.model.SyncFailure;
import com.teams_tracking_system.model.SyncType;
import com.teams_tracking_system.repositories.SyncCursorRepository;
import com.teams_tracking_system.repositories.SyncExecutionRepository;
import com.teams_tracking_system.repositories.SyncFailureRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class SyncMonitoringServiceTest {

    @Mock
    private SyncExecutionRepository syncExecutionRepository;

    @Mock
    private SyncFailureRepository syncFailureRepository;

    @Mock
    private SyncCursorRepository syncCursorRepository;

    @InjectMocks
    private SyncMonitoringService syncMonitoringService;

    @Test
    void getOverviewReturnsLatestExecutionsFailuresRetriesAndCursor() {
        SyncExecution agentExecution = syncExecution(
                1L,
                SyncType.AGENTS,
                "2026-05-24T10:00:00Z",
                "cursor-before",
                "cursor-after");
        agentExecution.updateCounters(5, 2, 1, 1, 1, 3, 2, 1, Instant.parse("2026-05-24T10:01:00Z"));
        agentExecution.finishWithWarning(
                Instant.parse("2026-05-24T10:01:00Z"),
                60000L,
                "cursor-after",
                "Agent sync finished with 1 invalid payload(s).",
                Instant.parse("2026-05-24T10:01:00Z"));

        SyncExecution locationExecution = syncExecution(
                2L,
                SyncType.LOCATIONS,
                "2026-05-24T10:05:00Z",
                null,
                null);
        locationExecution.updateCounters(3, 1, 1, 0, 0, 1, 0, 1, Instant.parse("2026-05-24T10:05:30Z"));
        locationExecution.fail(
                Instant.parse("2026-05-24T10:05:30Z"),
                30000L,
                "external unavailable",
                Instant.parse("2026-05-24T10:05:30Z"));

        SyncCursor agentCursor = new SyncCursor(
                SyncType.AGENTS,
                "cursor-after",
                null,
                Instant.parse("2026-05-24T09:59:00Z"),
                Instant.parse("2026-05-24T10:01:00Z"),
                Instant.parse("2026-05-24T10:01:00Z"),
                Instant.parse("2026-05-24T10:00:00Z"),
                Instant.parse("2026-05-24T10:01:00Z"));
        SyncFailure failure = syncFailure(10L, agentExecution);

        when(syncExecutionRepository.findAll()).thenReturn(List.of(agentExecution, locationExecution));
        when(syncExecutionRepository.findAllByOrderByStartedAtDesc(any(Pageable.class)))
                .thenReturn(List.of(locationExecution, agentExecution));
        when(syncExecutionRepository.findFirstBySyncTypeOrderByStartedAtDesc(SyncType.AGENTS))
                .thenReturn(Optional.of(agentExecution));
        when(syncExecutionRepository.findFirstBySyncTypeOrderByStartedAtDesc(SyncType.LOCATIONS))
                .thenReturn(Optional.of(locationExecution));
        when(syncExecutionRepository.findFirstBySyncTypeOrderByStartedAtDesc(SyncType.CHECK_INS))
                .thenReturn(Optional.empty());
        when(syncExecutionRepository.findFirstBySyncTypeOrderByStartedAtDesc(SyncType.GEOFENCES))
                .thenReturn(Optional.empty());
        when(syncFailureRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(List.of(failure));
        when(syncFailureRepository.count()).thenReturn(1L);
        when(syncCursorRepository.findAllByOrderBySyncTypeAsc()).thenReturn(List.of(agentCursor));

        var overview = syncMonitoringService.getOverview(10, 10);

        assertThat(overview.totals().executions()).isEqualTo(2);
        assertThat(overview.totals().failures()).isEqualTo(1);
        assertThat(overview.totals().recordsRead()).isEqualTo(8);
        assertThat(overview.totals().recordsFailed()).isEqualTo(1);
        assertThat(overview.totals().retryAttempts()).isEqualTo(4);
        assertThat(overview.totals().rateLimitErrors()).isEqualTo(2);
        assertThat(overview.totals().serviceUnavailableErrors()).isEqualTo(2);

        assertThat(overview.schedulers()).hasSize(4);
        assertThat(overview.schedulers().get(0).syncType()).isEqualTo(SyncType.AGENTS);
        assertThat(overview.schedulers().get(0).status()).isEqualTo(SyncExecutionStatus.WARNING);
        assertThat(overview.schedulers().get(0).lastCursorValue()).isEqualTo("cursor-after");
        assertThat(overview.schedulers().get(1).syncType()).isEqualTo(SyncType.LOCATIONS);
        assertThat(overview.schedulers().get(1).status()).isEqualTo(SyncExecutionStatus.FAILED);

        assertThat(overview.latestExecutions()).hasSize(2);
        assertThat(overview.latestExecutions().get(0).syncType()).isEqualTo(SyncType.LOCATIONS);
        assertThat(overview.recentFailures()).hasSize(1);
        assertThat(overview.recentFailures().get(0).reason()).isEqualTo("agent.id is required");
        assertThat(overview.cursors()).hasSize(1);
        assertThat(overview.cursors().get(0).lastCursorValue()).isEqualTo("cursor-after");
    }

    private SyncExecution syncExecution(
            Long id,
            SyncType syncType,
            String startedAt,
            String cursorBefore,
            String cursorAfter) {
        Instant started = Instant.parse(startedAt);
        SyncExecution execution = new SyncExecution(
                syncType,
                SyncExecutionStatus.RUNNING,
                started,
                cursorBefore,
                started,
                started);
        setField(execution, "id", id);
        if (cursorAfter != null) {
            execution.finishSuccessfully(started.plusSeconds(1), 1000L, cursorAfter, started.plusSeconds(1));
        }
        return execution;
    }

    private SyncFailure syncFailure(Long id, SyncExecution execution) {
        SyncFailure failure = new SyncFailure(
                execution,
                SyncType.AGENTS,
                "agent",
                "agent.id is required",
                "{\"externalId\":\"ext-agent-001\"}",
                Instant.parse("2026-05-24T10:01:00Z"));
        setField(failure, "id", id);
        return failure;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
