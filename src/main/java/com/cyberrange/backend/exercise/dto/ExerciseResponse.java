package com.cyberrange.backend.exercise.dto;

import com.cyberrange.backend.exercise.entity.Exercise;

import java.time.Instant;

public record ExerciseResponse(
        Long id,
        Long scenarioId,
        String scenarioName,
        Long cyberRangeId,
        String cyberRangeName,
        String name,
        String status,
        Instant startedAt,
        Instant endedAt,
        Long createdBy,
        Instant createdAt
) {

    public static ExerciseResponse fromEntity(Exercise exercise) {
        return new ExerciseResponse(
                exercise.getId(),
                exercise.getScenario().getId(),
                exercise.getScenario().getName(),
                exercise.getCyberRange().getId(),
                exercise.getCyberRange().getName(),
                exercise.getName(),
                exercise.getStatus(),
                exercise.getStartedAt(),
                exercise.getEndedAt(),
                exercise.getCreatedBy() != null
                        ? exercise.getCreatedBy().getId()
                        : null,
                exercise.getCreatedAt()
        );
    }
}