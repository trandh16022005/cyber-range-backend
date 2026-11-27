package com.cyberrange.backend.assessment.repository;

import com.cyberrange.backend.assessment.entity.AssessmentScoreEvidence;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssessmentScoreEvidenceRepository
        extends JpaRepository<AssessmentScoreEvidence, Long> {

    /**
     * Find all evidence linked to an assessment score.
     * Fetch related entities to avoid lazy-loading issues.
     */
    @EntityGraph(attributePaths = {
            "assessmentScore",
            "evidence",
            "evidence.exercise",
            "evidence.event"
    })
    List<AssessmentScoreEvidence>
    findByAssessmentScore_Id(Long assessmentScoreId);

    /**
     * Find all assessment score links associated with an evidence.
     */
    List<AssessmentScoreEvidence> findByEvidence_Id(Long evidenceId);

    /**
     * Check whether evidence is already linked to an assessment score.
     */
    boolean existsByAssessmentScore_IdAndEvidence_Id(
            Long assessmentScoreId,
            Long evidenceId
    );
}