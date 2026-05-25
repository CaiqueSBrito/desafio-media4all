package com.teams_tracking_system.controllers;

import com.teams_tracking_system.dtos.requests.ManualCheckInRequest;
import com.teams_tracking_system.dtos.schemas.CheckInListResponse;
import com.teams_tracking_system.dtos.schemas.CheckInResponse;
import com.teams_tracking_system.service.CheckInService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agents/{agentId}/check-ins")
public class CheckInController {

    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @PostMapping
    public ResponseEntity<CheckInResponse> createManual(
            @PathVariable String agentId,
            @Valid @RequestBody ManualCheckInRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(checkInService.createManual(agentId, request));
    }

    @GetMapping
    public CheckInListResponse listByAgent(@PathVariable String agentId) {
        return checkInService.listByAgent(agentId);
    }
}
