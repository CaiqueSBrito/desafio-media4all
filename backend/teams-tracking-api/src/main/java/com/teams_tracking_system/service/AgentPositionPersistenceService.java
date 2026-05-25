package com.teams_tracking_system.service;

import com.teams_tracking_system.model.AgentPosition;
import com.teams_tracking_system.repositories.AgentPositionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentPositionPersistenceService {

    private final AgentPositionRepository agentPositionRepository;

    public AgentPositionPersistenceService(AgentPositionRepository agentPositionRepository) {
        this.agentPositionRepository = agentPositionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean saveSnapshot(AgentPosition agentPosition) {
        try {
            agentPositionRepository.save(agentPosition);
            return true;
        } catch (DataIntegrityViolationException exception) {
            return false;
        }
    }
}
