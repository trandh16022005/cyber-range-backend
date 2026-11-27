package com.cyberrange.backend.assessment.repository;

import com.cyberrange.backend.assessment.entity.AssessmentScoreEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssessmentScoreEvidenceRepository
        extends JpaRepository<AssessmentScoreEvidence, Long> {

    List<AssessmentScoreEvidence> findByAssessmentScore_Id(Long assessmentScoreId);

    List<AssessmentScoreEvidence> findByEvidence_Id(Long evidenceId);

    boolean existsByAssessmentScore_IdAndEvidence_Id(
            Long assessmentScoreId,
            Long evidenceId
    );
}
