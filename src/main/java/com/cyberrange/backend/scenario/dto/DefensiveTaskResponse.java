package com.cyberrange.backend.scenario.dto;

import com.cyberrange.backend.scenario.entity.DefensiveTask;

import java.math.BigDecimal;

public record DefensiveTaskResponse(
        Long id,
        Long scenarioId,
        String name,
        String description,
        String taskType,
        Integer sequenceOrder,
        BigDecimal maxScore,
        Boolean required
) {

    public static DefensiveTaskResponse fromEntity(
            DefensiveTask task
    ) {
        return new DefensiveTaskResponse(
                task.getId(),
                task.getScenario().getId(),
                task.getName(),
                task.getDescription(),
                task.getTaskType(),
                task.getSequenceOrder(),
                task.getMaxScore(),
                task.getRequired()
        );
    }
}
