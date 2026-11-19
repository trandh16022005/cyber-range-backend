package com.cyberrange.backend.exercise.dto;

public record CreateExerciseRequest(
        Long scenarioId,
        Long cyberRangeId,
        String name
) {
}