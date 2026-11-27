package com.cyberrange.backend.assessment.dto;

import com.cyberrange.backend.assessment.entity.AssessmentScoreEvidence;

import java.time.Instant;

public record AssessmentScoreEvidenceResponse(
        Long id,
        Long assessmentScoreId,
        Long evidenceId,
        String evidenceType,
        String verificationStatus,
        Instant createdAt
) {

    public static AssessmentScoreEvidenceResponse fromEntity(
            AssessmentScoreEvidence entity
    ) {
        return new AssessmentScoreEvidenceResponse(
                entity.getId(),
                entity.getAssessmentScore().getId(),
                entity.getEvidence().getId(),
                entity.getEvidence().getEvidenceType(),
                entity.getEvidence().getVerificationStatus(),
                entity.getCreatedAt()
        );
    }
}
