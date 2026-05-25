package com.teams_tracking_system.service;

import com.teams_tracking_system.core.exception.InvalidRequestException;
import com.teams_tracking_system.core.exception.ResourceNotFoundException;
import com.teams_tracking_system.dtos.requests.ManualCheckInRequest;
import com.teams_tracking_system.dtos.schemas.CheckInListResponse;
import com.teams_tracking_system.dtos.schemas.CheckInResponse;
import com.teams_tracking_system.model.Agent;
import com.teams_tracking_system.model.CheckIn;
import com.teams_tracking_system.model.SyncSource;
import com.teams_tracking_system.repositories.AgentRepository;
import com.teams_tracking_system.repositories.CheckInRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CheckInService {

    private final AgentRepository agentRepository;
    private final CheckInRepository checkInRepository;

    public CheckInService(
            AgentRepository agentRepository,
            CheckInRepository checkInRepository) {
        this.agentRepository = agentRepository;
        this.checkInRepository = checkInRepository;
    }

    @Transactional
    public CheckInResponse createManual(String agentId, ManualCheckInRequest request) {
        Agent agent = findAgentById(agentId);

        if (!agent.isActive()) {
            throw new InvalidRequestException(
                    "Agent is inactive.",
                    "manual check-in requires an active agent: " + agentId);
        }

        String idempotencyKey = normalizeIdempotencyKey(request.idempotencyKey());
        if (idempotencyKey != null) {
            return checkInRepository.findByManualIdempotencyKey(idempotencyKey)
                    .map(CheckInResponse::fromEntity)
                    .orElseGet(() -> persistManualCheckIn(agent, request, idempotencyKey));
        }

        return persistManualCheckIn(agent, request, null);
    }

    @Transactional(readOnly = true)
    public CheckInListResponse listByAgent(String agentId) {
        findAgentById(agentId);

        return new CheckInListResponse(checkInRepository.findByAgent_IdOrderByOccurredAtDesc(agentId)
                .stream()
                .map(CheckInResponse::fromEntity)
                .toList());
    }

    private CheckInResponse persistManualCheckIn(
            Agent agent,
            ManualCheckInRequest request,
            String idempotencyKey) {
        Instant now = Instant.now();
        Instant occurredAt = request.occurredAt() == null ? now : request.occurredAt();

        CheckIn checkIn = new CheckIn(
                UUID.randomUUID().toString(),
                agent,
                request.type(),
                SyncSource.MANUAL,
                request.latitude(),
                request.longitude(),
                request.address(),
                request.accuracy(),
                request.speed(),
                request.notes(),
                null,
                null,
                idempotencyKey,
                occurredAt,
                now,
                now,
                now);

        return CheckInResponse.fromEntity(checkInRepository.save(checkIn));
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return null;
        }
        return idempotencyKey.trim();
    }

    private Agent findAgentById(String agentId) {
        return agentRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Agent not found.",
                        "id not found: " + agentId));
    }
}
