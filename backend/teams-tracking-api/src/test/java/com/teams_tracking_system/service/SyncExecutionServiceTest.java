package com.teams_tracking_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teams_tracking_system.model.SyncExecution;
import com.teams_tracking_system.model.SyncExecutionStatus;
import com.teams_tracking_system.model.SyncType;
import com.teams_tracking_system.repositories.SyncExecutionRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SyncExecutionServiceTest {

    @Mock
    private SyncExecutionRepository syncExecutionRepository;

    @InjectMocks
    private SyncExecutionService syncExecutionService;

    @Test
    void startPersistsRunningExecution() {
        when(syncExecutionRepository.save(any(SyncExecution.class))).thenAnswer(invocation -> {
            SyncExecution execution = invocation.getArgument(0);
            setId(execution, 10L);
            return execution;
        });

        Long executionId = syncExecutionService.start(SyncType.AGENTS, "cursor-before");

        assertThat(executionId).isEqualTo(10L);

        ArgumentCaptor<SyncExecution> captor = ArgumentCaptor.forClass(SyncExecution.class);
        verify(syncExecutionRepository).save(captor.capture());
        assertThat(captor.getValue().getSyncType()).isEqualTo(SyncType.AGENTS);
        assertThat(captor.getValue().getStatus()).isEqualTo(SyncExecutionStatus.RUNNING);
        assertThat(captor.getValue().getCursorValueBefore()).isEqualTo("cursor-before");
    }

    @Test
    void finishSuccessfullyPersistsCountersAndSuccessStatus() {
        SyncExecution execution = buildRunningExecution();
        when(syncExecutionRepository.findById(10L)).thenReturn(Optional.of(execution));

        syncExecutionService.finishSuccessfully(10L, 5, 2, 1, 2, 0, 3, 1, 2, "cursor-after");

        assertThat(execution.getStatus()).isEqualTo(SyncExecutionStatus.SUCCESS);
        assertThat(execution.getRecordsRead()).isEqualTo(5);
        assertThat(execution.getRecordsCreated()).isEqualTo(2);
        assertThat(execution.getRecordsUpdated()).isEqualTo(1);
        assertThat(execution.getRecordsIgnored()).isEqualTo(2);
        assertThat(execution.getRecordsFailed()).isZero();
        assertThat(execution.getRetryAttempts()).isEqualTo(3);
        assertThat(execution.getRateLimitErrors()).isEqualTo(1);
        assertThat(execution.getServiceUnavailableErrors()).isEqualTo(2);
        assertThat(execution.getCursorValueAfter()).isEqualTo("cursor-after");
        assertThat(execution.getFinishedAt()).isNotNull();
        verify(syncExecutionRepository).save(execution);
    }

    @Test
    void finishWithWarningPersistsCountersAndWarningStatus() {
        SyncExecution execution = buildRunningExecution();
        when(syncExecutionRepository.findById(10L)).thenReturn(Optional.of(execution));

        syncExecutionService.finishWithWarning(
                10L,
                5,
                2,
                1,
                0,
                2,
                3,
                1,
                2,
                null,
                "invalid payloads");

        assertThat(execution.getStatus()).isEqualTo(SyncExecutionStatus.WARNING);
        assertThat(execution.getRecordsRead()).isEqualTo(5);
        assertThat(execution.getRecordsFailed()).isEqualTo(2);
        assertThat(execution.getErrorMessage()).isEqualTo("invalid payloads");
        assertThat(execution.getFinishedAt()).isNotNull();
        verify(syncExecutionRepository).save(execution);
    }

    @Test
    void failPersistsFailureStatusAndTruncatedMessage() {
        SyncExecution execution = buildRunningExecution();
        when(syncExecutionRepository.findById(10L)).thenReturn(Optional.of(execution));

        syncExecutionService.fail(10L, new RuntimeException("external API unavailable"));

        assertThat(execution.getStatus()).isEqualTo(SyncExecutionStatus.FAILED);
        assertThat(execution.getErrorMessage()).isEqualTo("external API unavailable");
        assertThat(execution.getFinishedAt()).isNotNull();
        verify(syncExecutionRepository).save(execution);
    }

    @Test
    void failPersistsRetryCounters() {
        SyncExecution execution = buildRunningExecution();
        when(syncExecutionRepository.findById(10L)).thenReturn(Optional.of(execution));

        syncExecutionService.fail(10L, new RuntimeException("external API unavailable"), 2, 1, 1);

        assertThat(execution.getStatus()).isEqualTo(SyncExecutionStatus.FAILED);
        assertThat(execution.getRetryAttempts()).isEqualTo(2);
        assertThat(execution.getRateLimitErrors()).isEqualTo(1);
        assertThat(execution.getServiceUnavailableErrors()).isEqualTo(1);
        verify(syncExecutionRepository).save(execution);
    }

    private SyncExecution buildRunningExecution() {
        Instant now = Instant.parse("2026-05-23T10:00:00Z");
        SyncExecution execution = new SyncExecution(
                SyncType.AGENTS,
                SyncExecutionStatus.RUNNING,
                now,
                null,
                now,
                now);
        setId(execution, 10L);
        return execution;
    }

    private void setId(SyncExecution execution, Long id) {
        try {
            var field = SyncExecution.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(execution, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
