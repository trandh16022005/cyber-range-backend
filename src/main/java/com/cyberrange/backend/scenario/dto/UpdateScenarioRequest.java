package com.cyberrange.backend.scenario.dto;

public record UpdateScenarioRequest(
        String name,
        String description,
        String type,
        String difficulty
) {
}
