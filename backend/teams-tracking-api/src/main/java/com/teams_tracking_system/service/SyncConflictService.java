package com.teams_tracking_system.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teams_tracking_system.model.SyncConflict;
import com.teams_tracking_system.model.SyncExecution;
import com.teams_tracking_system.model.SyncType;
import com.teams_tracking_system.repositories.SyncConflictRepository;
import com.teams_tracking_system.repositories.SyncExecutionRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncConflictService {

    private static final int MAX_REASON_LENGTH = 500;

    private final SyncConflictRepository syncConflictRepository;
    private final SyncExecutionRepository syncExecutionRepository;
    private final ObjectMapper objectMapper;

    public SyncConflictService(
            SyncConflictRepository syncConflictRepository,
            SyncExecutionRepository syncExecutionRepository,
            ObjectMapper objectMapper) {
        this.syncConflictRepository = syncConflictRepository;
        this.syncExecutionRepository = syncExecutionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordConflict(
            Long executionId,
            SyncType syncType,
            String entityType,
            String conflictKey,
            String reason,
            Object localSnapshot,
            Object externalPayload) {
        SyncExecution syncExecution = syncExecutionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalStateException("Sync execution not found: " + executionId));

        SyncConflict syncConflict = new SyncConflict(
                syncExecution,
                syncType,
                entityType,
                conflictKey,
                truncate(reason),
                serialize(localSnapshot),
                serialize(externalPayload),
                Instant.now());

        syncConflictRepository.save(syncConflict);
    }

    private String serialize(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            return "{\"serializationError\":\"" + exception.getClass().getSimpleName() + "\"}";
        }
    }

    private String truncate(String reason) {
        if (reason == null || reason.length() <= MAX_REASON_LENGTH) {
            return reason;
        }
        return reason.substring(0, MAX_REASON_LENGTH);
    }
}
