package com.cyberrange.backend.scenario.dto;

public record CreateScenarioRequest(
        String name,
        String description,
        String type,
        String difficulty
) {
}