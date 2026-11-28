package com.cyberrange.backend.lab.dto;

import com.cyberrange.backend.scenario.entity.DefensiveTask;
import java.math.BigDecimal;

public record SshPracticeTaskResponse(long taskId, String name, BigDecimal maximum) {
    public static SshPracticeTaskResponse from(DefensiveTask task) {
        return new SshPracticeTaskResponse(task.getId(), task.getName(), task.getMaxScore());
    }
}