package com.cyberrange.backend.assessment.repository;

import com.cyberrange.backend.assessment.entity.AssessmentScore;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface AssessmentScoreRepository
        extends JpaRepository<AssessmentScore, Long> {

    @EntityGraph(attributePaths = {
            "assessmentResult",
            "criterion"
    })
    List<AssessmentScore>
    findByAssessmentResult_IdOrderByIdAsc(Long assessmentResultId);

    boolean existsByAssessmentResult_IdAndCriterion_Id(
            Long assessmentResultId,
            Long criterionId
    );

    /**
     * Calculate total awarded score of evaluated assessment results.
     *
     * Only COMPLETED and FAILED results are included.
     * PENDING, IN_PROGRESS and NOT_APPLICABLE are excluded.
     */
    @Query("""
            SELECT COALESCE(SUM(s.awardedScore), 0)
            FROM AssessmentScore s
            WHERE s.assessmentResult.assessment.id = :assessmentId
              AND s.assessmentResult.status IN ('COMPLETED', 'FAILED')
            """)
    BigDecimal sumAwardedScoreByAssessmentId(
            @Param("assessmentId") Long assessmentId
    );
}