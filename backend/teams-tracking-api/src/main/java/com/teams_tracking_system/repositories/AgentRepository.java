package com.teams_tracking_system.repositories;

import com.teams_tracking_system.model.Agent;
import com.teams_tracking_system.model.AgentStatus;
import java.util.List;
import java.util.Optional;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentRepository extends JpaRepository<Agent, String> {

    Optional<Agent> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    List<Agent> findByActiveTrue();

    List<Agent> findByActiveTrueAndStatus(AgentStatus status);

    boolean existsByEmail(@Email(message = "email must be valid") @Size(max = 150, message = "email must have at most 150 characters") String email);

    boolean existsByEmailAndIdNot(String email, String id);
}
