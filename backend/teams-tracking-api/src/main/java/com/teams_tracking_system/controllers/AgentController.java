package com.teams_tracking_system.controllers;

import com.teams_tracking_system.dtos.requests.AgentCreateRequest;
import com.teams_tracking_system.dtos.requests.AgentUpdateRequest;
import com.teams_tracking_system.dtos.schemas.AgentListResponse;
import com.teams_tracking_system.dtos.schemas.AgentResponse;
import com.teams_tracking_system.dtos.schemas.RouteHistoryResponse;
import com.teams_tracking_system.service.AgentService;
import com.teams_tracking_system.service.RouteHistoryService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agents")
public class AgentController {

    private final AgentService agentService;
    private final RouteHistoryService routeHistoryService;

    public AgentController(
            AgentService agentService,
            RouteHistoryService routeHistoryService) {
        this.agentService = agentService;
        this.routeHistoryService = routeHistoryService;
    }

    @GetMapping
    public AgentListResponse list() {
        return agentService.list();
    }

    @GetMapping("/{id}")
    public AgentResponse getById(@PathVariable String id) {
        return agentService.getById(id);
    }

    @GetMapping("/{id}/route")
    public RouteHistoryResponse getDailyRoute(
            @PathVariable String id,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return routeHistoryService.getDailyRoute(id, date);
    }

    @PostMapping
    public ResponseEntity<AgentResponse> create(@Valid @RequestBody AgentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(agentService.create(request));
    }

    @PutMapping("/{id}")
    public AgentResponse update(
            @PathVariable String id,
            @Valid @RequestBody AgentUpdateRequest request) {
        return agentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable String id) {
        agentService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
