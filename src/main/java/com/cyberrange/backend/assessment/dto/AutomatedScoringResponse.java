package com.cyberrange.backend.assessment.dto;

import java.math.BigDecimal;
import java.util.List;

public record AutomatedScoringResponse(
        Long assessmentId,
        BigDecimal totalScore,
        int automatedCount,
        int manualSkippedCount,
        int unconfiguredCount,
        int taskSkippedCount,
        List<ScoreEvaluation> evaluations
) {

    public record ScoreEvaluation(
            Long assessmentScoreId,
            Long assessmentResultId,
            Long criterionId,
            BigDecimal awardedScore,
            BigDecimal maxScore,
            String scoreSource,
            String reason,
            String evaluationStatus
    ) {
    }
}
