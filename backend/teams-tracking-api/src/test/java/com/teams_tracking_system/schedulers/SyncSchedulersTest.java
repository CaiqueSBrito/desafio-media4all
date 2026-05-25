package com.teams_tracking_system.schedulers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teams_tracking_system.service.AgentSyncService;
import com.teams_tracking_system.service.CheckInSyncService;
import com.teams_tracking_system.service.GeofenceSyncService;
import com.teams_tracking_system.service.LocationSyncService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class SyncSchedulersTest {

    @Test
    void schedulersCallTheirSyncServices() {
        AgentSyncService agentSyncService = org.mockito.Mockito.mock(AgentSyncService.class);
        LocationSyncService locationSyncService = org.mockito.Mockito.mock(LocationSyncService.class);
        CheckInSyncService checkInSyncService = org.mockito.Mockito.mock(CheckInSyncService.class);
        GeofenceSyncService geofenceSyncService = org.mockito.Mockito.mock(GeofenceSyncService.class);

        assertThat(new AgentSyncScheduler(agentSyncService).runScheduledSync()).isTrue();
        assertThat(new LocationSyncScheduler(locationSyncService).runScheduledSync()).isTrue();
        assertThat(new CheckInSyncScheduler(checkInSyncService).runScheduledSync()).isTrue();
        assertThat(new GeofenceSyncScheduler(geofenceSyncService).runScheduledSync()).isTrue();

        verify(agentSyncService).syncAgents(null);
        verify(locationSyncService).syncLocations(null);
        verify(checkInSyncService).syncCheckIns();
        verify(geofenceSyncService).syncGeofences();
    }

    @Test
    void schedulerSkipsConcurrentExecutionOfSameJob() throws Exception {
        AgentSyncService agentSyncService = org.mockito.Mockito.mock(AgentSyncService.class);
        AgentSyncScheduler scheduler = new AgentSyncScheduler(agentSyncService);
        CountDownLatch executionStarted = new CountDownLatch(1);
        CountDownLatch releaseExecution = new CountDownLatch(1);

        when(agentSyncService.syncAgents(null)).thenAnswer(invocation -> {
            executionStarted.countDown();
            assertThat(releaseExecution.await(2, TimeUnit.SECONDS)).isTrue();
            return null;
        });

        Thread firstExecution = new Thread(scheduler::runScheduledSync);
        firstExecution.start();
        assertThat(executionStarted.await(2, TimeUnit.SECONDS)).isTrue();

        assertThat(scheduler.runScheduledSync()).isFalse();

        releaseExecution.countDown();
        firstExecution.join(2000);
        assertThat(firstExecution.isAlive()).isFalse();
        verify(agentSyncService, times(1)).syncAgents(null);
    }

    @Test
    void schedulerReleasesLockWhenSyncServiceFails() {
        AgentSyncService agentSyncService = org.mockito.Mockito.mock(AgentSyncService.class);
        AgentSyncScheduler scheduler = new AgentSyncScheduler(agentSyncService);

        when(agentSyncService.syncAgents(null))
                .thenThrow(new RuntimeException("external unavailable"))
                .thenReturn(null);

        assertThat(scheduler.runScheduledSync()).isTrue();
        assertThat(scheduler.runScheduledSync()).isTrue();
        verify(agentSyncService, times(2)).syncAgents(null);
    }
}
