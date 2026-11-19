package com.cyberrange.backend.scenario.dto;

import com.cyberrange.backend.scenario.entity.Scenario;

import java.time.Instant;

public record ScenarioResponse(
        Long id,
        String name,
        String description,
        String type,
        String difficulty,
        String status,
        Long createdBy,
        Instant createdAt,
        Instant updatedAt
) {

    public static ScenarioResponse fromEntity(Scenario scenario) {
        return new ScenarioResponse(
                scenario.getId(),
                scenario.getName(),
                scenario.getDescription(),
                scenario.getType(),
                scenario.getDifficulty(),
                scenario.getStatus(),
                scenario.getCreatedBy() != null
                        ? scenario.getCreatedBy().getId()
                        : null,
                scenario.getCreatedAt(),
                scenario.getUpdatedAt()
        );
    }
}
