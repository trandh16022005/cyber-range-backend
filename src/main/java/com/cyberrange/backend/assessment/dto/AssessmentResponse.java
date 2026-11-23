package com.cyberrange.backend.assessment.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AssessmentResponse(
        Long id,
        Long exerciseId,
        Long teamId,
        String teamName,
        String status,
        BigDecimal totalScore,
        Instant startedAt,
        Instant completedAt,
        String reviewStatus,
        Long reviewedById,
        Instant reviewedAt,
        String reviewComment,
        Instant createdAt
) {
}
