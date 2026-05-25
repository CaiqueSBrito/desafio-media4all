package com.teams_tracking_system.schedulers;

import com.teams_tracking_system.service.AgentSyncService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "sync.schedulers", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgentSyncScheduler extends NonConcurrentSyncScheduler {

    private final AgentSyncService agentSyncService;

    public AgentSyncScheduler(AgentSyncService agentSyncService) {
        super("Agents");
        this.agentSyncService = agentSyncService;
    }

    @Scheduled(
            initialDelayString = "${sync.schedulers.agents.initial-delay-ms:30000}",
            fixedDelayString = "${sync.schedulers.agents.fixed-delay-ms:300000}")
    public boolean runScheduledSync() {
        return runIfIdle(() -> agentSyncService.syncAgents(null));
    }
}
