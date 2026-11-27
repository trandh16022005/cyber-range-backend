package com.cyberrange.backend.assessment.repository;

import com.cyberrange.backend.assessment.entity.AssessmentResult;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssessmentResultRepository
        extends JpaRepository<AssessmentResult, Long> {

    @EntityGraph(attributePaths = {
            "assessment",
            "defensiveTask"
    })
    List<AssessmentResult>
    findByAssessment_IdOrderByDefensiveTask_SequenceOrderAsc(
            Long assessmentId
    );

    @EntityGraph(attributePaths = {
            "assessment",
            "assessment.exercise",
            "assessment.team",
            "defensiveTask"
    })
    Optional<AssessmentResult> findWithAssessmentDetailsById(Long id);

    Optional<AssessmentResult>
    findByAssessment_IdAndDefensiveTask_Id(
            Long assessmentId,
            Long defensiveTaskId
    );

    boolean existsByAssessment_IdAndDefensiveTask_Id(
            Long assessmentId,
            Long defensiveTaskId
    );
}
