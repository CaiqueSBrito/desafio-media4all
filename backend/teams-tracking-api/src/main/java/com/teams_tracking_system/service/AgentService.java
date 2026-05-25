package com.teams_tracking_system.service;

import com.teams_tracking_system.core.exception.ResourceConflictException;
import com.teams_tracking_system.core.exception.ResourceNotFoundException;
import com.teams_tracking_system.dtos.requests.AgentCreateRequest;
import com.teams_tracking_system.dtos.requests.AgentUpdateRequest;
import com.teams_tracking_system.dtos.schemas.AgentListResponse;
import com.teams_tracking_system.dtos.schemas.AgentResponse;
import com.teams_tracking_system.model.Agent;
import com.teams_tracking_system.repositories.AgentRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentService {

    private final AgentRepository agentRepository;

    public AgentService(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    @Transactional(readOnly = true)
    public AgentListResponse list() {
        return new AgentListResponse(agentRepository.findAll()
                .stream()
                .map(AgentResponse::fromEntity)
                .toList());
    }

    @Transactional(readOnly = true)
    public AgentResponse getById(String id) {
        return AgentResponse.fromEntity(findAgentById(id));
    }

    @Transactional
    public AgentResponse create(AgentCreateRequest request) {
        if (agentRepository.existsByExternalId(request.externalId())) {
            throw new ResourceConflictException(
                    "Agent already exists.",
                    "externalId already registered: " + request.externalId());
        }
        if (request.email() != null && agentRepository.existsByEmail(request.email())) {
            throw new ResourceConflictException(
                    "Agent already exists.",
                    "email already registered: " + request.email());
        }

        Instant now = Instant.now();
        Agent agent = new Agent(
                UUID.randomUUID().toString(),
                request.externalId(),
                request.name(),
                request.role(),
                request.team(),
                request.phone(),
                request.email(),
                request.active(),
                request.status(),
                request.battery(),
                request.lastSeen(),
                null,
                null,
                null,
                null,
                null,
                now,
                now);

        return AgentResponse.fromEntity(agentRepository.save(agent));
    }

    @Transactional
    public AgentResponse update(String id, AgentUpdateRequest request) {
        Agent agent = findAgentById(id);

        if (request.email() != null && agentRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new ResourceConflictException(
                    "Agent already exists.",
                    "email already registered: " + request.email());
        }

        agent.updateFromRequest(
                request.name(),
                request.role(),
                request.team(),
                request.phone(),
                request.email(),
                request.active(),
                request.status(),
                request.battery(),
                request.lastSeen(),
                Instant.now());

        return AgentResponse.fromEntity(agentRepository.save(agent));
    }

    @Transactional
    public void deactivate(String id) {
        Agent agent = findAgentById(id);
        agent.deactivate(Instant.now());
        agentRepository.save(agent);
    }

    private Agent findAgentById(String id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Agent not found.",
                        "id not found: " + id));
    }
}
