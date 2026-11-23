package com.cyberrange.backend.assessment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AssessmentResultResponse(
        Long id,
        Long assessmentId,
        Long defensiveTaskId,
        String defensiveTaskName,
        Integer sequenceOrder,
        String status,
        Instant startedAt,
        Instant detectedAt,
        Instant respondedAt,
        Instant completedAt,
        Long detectionTimeSeconds,
        Long responseTimeSeconds,
        String reason,
        BigDecimal awardedScore,
        BigDecimal maxScore,
        List<AssessmentScoreResponse> scores
) {
}
