package com.teams_tracking_system.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teams_tracking_system.model.SyncFailure;
import com.teams_tracking_system.model.SyncExecution;
import com.teams_tracking_system.model.SyncType;
import com.teams_tracking_system.repositories.SyncExecutionRepository;
import com.teams_tracking_system.repositories.SyncFailureRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncFailureService {

    private static final int MAX_REASON_LENGTH = 500;

    private final SyncFailureRepository syncFailureRepository;
    private final SyncExecutionRepository syncExecutionRepository;
    private final ObjectMapper objectMapper;

    public SyncFailureService(
            SyncFailureRepository syncFailureRepository,
            SyncExecutionRepository syncExecutionRepository,
            ObjectMapper objectMapper) {
        this.syncFailureRepository = syncFailureRepository;
        this.syncExecutionRepository = syncExecutionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordInvalidPayload(
            Long syncExecutionId,
            SyncType syncType,
            String entityType,
            String reason,
            Object payload) {
        SyncExecution syncExecution = syncExecutionRepository.findById(syncExecutionId)
                .orElseThrow(() -> new IllegalStateException("Sync execution not found: " + syncExecutionId));

        SyncFailure syncFailure = new SyncFailure(
                syncExecution,
                syncType,
                entityType,
                truncate(reason, MAX_REASON_LENGTH),
                serializePayload(payload),
                Instant.now());

        syncFailureRepository.save(syncFailure);
    }

    private String serializePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            return "{\"serializationError\":\"" + sanitize(exception.getOriginalMessage()) + "\"}";
        }
    }

    private String sanitize(String value) {
        if (value == null) {
            return "unknown";
        }
        return value.replace("\"", "'");
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
