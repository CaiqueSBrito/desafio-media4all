package com.teams_tracking_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teams_tracking_system.core.exception.ResourceConflictException;
import com.teams_tracking_system.core.exception.ResourceNotFoundException;
import com.teams_tracking_system.dtos.requests.AgentCreateRequest;
import com.teams_tracking_system.dtos.requests.AgentUpdateRequest;
import com.teams_tracking_system.model.Agent;
import com.teams_tracking_system.model.AgentRole;
import com.teams_tracking_system.model.AgentStatus;
import com.teams_tracking_system.repositories.AgentRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private AgentRepository agentRepository;

    @InjectMocks
    private AgentService agentService;

    @Test
    void createPersistsAgentWhenBusinessIdentifiersAreUnique() {
        AgentCreateRequest request = new AgentCreateRequest(
                "ext-1",
                "Agent One",
                AgentRole.TECHNICIAN,
                "Field",
                "555-0100",
                "agent.one@example.com",
                true,
                AgentStatus.ONLINE,
                90,
                Instant.parse("2026-05-22T10:00:00Z"));

        when(agentRepository.existsByExternalId("ext-1")).thenReturn(false);
        when(agentRepository.existsByEmail("agent.one@example.com")).thenReturn(false);
        when(agentRepository.save(any(Agent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = agentService.create(request);

        assertThat(response.id()).isNotBlank();
        assertThat(response.externalId()).isEqualTo("ext-1");
        assertThat(response.name()).isEqualTo("Agent One");
        assertThat(response.active()).isTrue();

        ArgumentCaptor<Agent> agentCaptor = ArgumentCaptor.forClass(Agent.class);
        verify(agentRepository).save(agentCaptor.capture());
        assertThat(agentCaptor.getValue().getCreatedAt()).isNotNull();
        assertThat(agentCaptor.getValue().getUpdatedAt()).isNotNull();
    }

    @Test
    void createRejectsDuplicatedExternalId() {
        AgentCreateRequest request = new AgentCreateRequest(
                "ext-1",
                "Agent One",
                AgentRole.TECHNICIAN,
                null,
                null,
                null,
                true,
                AgentStatus.ONLINE,
                null,
                null);

        when(agentRepository.existsByExternalId("ext-1")).thenReturn(true);

        assertThatThrownBy(() -> agentService.create(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Agent already exists.");
    }

    @Test
    void updateRejectsDuplicatedEmailFromAnotherAgent() {
        Agent agent = buildAgent("agent-1", "ext-1", "agent.one@example.com", true);
        AgentUpdateRequest request = new AgentUpdateRequest(
                null,
                null,
                null,
                null,
                "agent.two@example.com",
                null,
                null,
                null,
                null);

        when(agentRepository.findById("agent-1")).thenReturn(Optional.of(agent));
        when(agentRepository.existsByEmailAndIdNot("agent.two@example.com", "agent-1")).thenReturn(true);

        assertThatThrownBy(() -> agentService.update("agent-1", request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Agent already exists.");
    }

    @Test
    void deactivateMarksAgentInactiveAndOffline() {
        Agent agent = buildAgent("agent-1", "ext-1", "agent.one@example.com", true);
        when(agentRepository.findById("agent-1")).thenReturn(Optional.of(agent));
        when(agentRepository.save(any(Agent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        agentService.deactivate("agent-1");

        assertThat(agent.isActive()).isFalse();
        assertThat(agent.getStatus()).isEqualTo(AgentStatus.OFFLINE);
        verify(agentRepository).save(agent);
    }

    @Test
    void getByIdRejectsMissingAgent() {
        when(agentRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agentService.getById("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Agent not found.");
    }

    private Agent buildAgent(String id, String externalId, String email, boolean active) {
        Instant now = Instant.parse("2026-05-22T10:00:00Z");
        return new Agent(
                id,
                externalId,
                "Agent One",
                AgentRole.TECHNICIAN,
                "Field",
                "555-0100",
                email,
                active,
                AgentStatus.ONLINE,
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
