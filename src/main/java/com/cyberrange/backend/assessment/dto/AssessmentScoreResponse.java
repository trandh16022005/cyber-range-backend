package com.cyberrange.backend.assessment.dto;

import java.math.BigDecimal;

public record AssessmentScoreResponse(
        Long id,
        Long criterionId,
        String criterionName,
        String criterionType,
        BigDecimal awardedScore,
        BigDecimal maxScore,
        String reason,
        String scoreSource
) {
}
