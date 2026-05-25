package com.teams_tracking_system.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import com.teams_tracking_system.model.Agent;
import com.teams_tracking_system.model.AgentRole;
import com.teams_tracking_system.model.AgentStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class AgentRepositoryTest {

    @Autowired
    private AgentRepository agentRepository;

    @Test
    void findsActiveAgentsByStatus() {
        Agent onlineAgent = buildAgent("agent-1", "ext-1", "agent.one@example.com", true, AgentStatus.ONLINE);
        Agent offlineAgent = buildAgent("agent-2", "ext-2", "agent.two@example.com", true, AgentStatus.OFFLINE);
        Agent inactiveAgent = buildAgent("agent-3", "ext-3", "agent.three@example.com", false, AgentStatus.ONLINE);
        agentRepository.save(onlineAgent);
        agentRepository.save(offlineAgent);
        agentRepository.save(inactiveAgent);

        var activeOnlineAgents = agentRepository.findByActiveTrueAndStatus(AgentStatus.ONLINE);

        assertThat(activeOnlineAgents)
                .extracting(Agent::getId)
                .containsExactly("agent-1");
    }

    @Test
    void detectsExistingBusinessIdentifiers() {
        agentRepository.save(buildAgent("agent-1", "ext-1", "agent.one@example.com", true, AgentStatus.ONLINE));

        assertThat(agentRepository.existsByExternalId("ext-1")).isTrue();
        assertThat(agentRepository.existsByEmail("agent.one@example.com")).isTrue();
        assertThat(agentRepository.existsByEmailAndIdNot("agent.one@example.com", "agent-2")).isTrue();
        assertThat(agentRepository.existsByEmailAndIdNot("agent.one@example.com", "agent-1")).isFalse();
    }

    private Agent buildAgent(
            String id,
            String externalId,
            String email,
            boolean active,
            AgentStatus status) {
        Instant now = Instant.parse("2026-05-22T10:00:00Z");
        return new Agent(
                id,
                externalId,
                "Agent " + id,
                AgentRole.TECHNICIAN,
                "Field",
                "555-0100",
                email,
                active,
                status,
                80,
                now,
                null,
                null,
                null,
                null,
                null,
                now,
                now);
    }
}
