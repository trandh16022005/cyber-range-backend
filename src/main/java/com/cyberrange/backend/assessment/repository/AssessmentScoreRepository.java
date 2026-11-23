package com.cyberrange.backend.assessment.repository;

import com.cyberrange.backend.assessment.entity.AssessmentScore;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
}

