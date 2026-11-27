package com.cyberrange.backend.assessment.service;

import com.cyberrange.backend.assessment.dto.AssessmentScoreEvidenceResponse;
import com.cyberrange.backend.assessment.entity.AssessmentScore;
import com.cyberrange.backend.assessment.entity.AssessmentScoreEvidence;
import com.cyberrange.backend.assessment.repository.AssessmentScoreEvidenceRepository;
import com.cyberrange.backend.assessment.repository.AssessmentScoreRepository;
import com.cyberrange.backend.assessment.repository.AssessmentResultRepository;
import com.cyberrange.backend.common.exception.BusinessException;
import com.cyberrange.backend.common.exception.ResourceNotFoundException;
import com.cyberrange.backend.evidence.entity.Evidence;
import com.cyberrange.backend.evidence.repository.EvidenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AssessmentScoreEvidenceService {

    private static final String VERIFIED = "VERIFIED";

    private final AssessmentScoreEvidenceRepository linkRepository;
    private final AssessmentScoreRepository scoreRepository;
    private final AssessmentResultRepository resultRepository;
    private final EvidenceRepository evidenceRepository;

    public AssessmentScoreEvidenceService(
            AssessmentScoreEvidenceRepository linkRepository,
            AssessmentScoreRepository scoreRepository,
            AssessmentResultRepository resultRepository,
            EvidenceRepository evidenceRepository
    ) {
        this.linkRepository = linkRepository;
        this.scoreRepository = scoreRepository;
        this.resultRepository = resultRepository;
        this.evidenceRepository = evidenceRepository;
    }

    /**
     * Link verified Evidence to an Assessment Score.
     *
     * Evidence and Assessment Score must belong
     * to the same Exercise.
     */
    @Transactional
    public AssessmentScoreEvidence linkEvidence(
            Long assessmentScoreId,
            Long evidenceId
    ) {
        validateId(assessmentScoreId, "Assessment Score ID");
        validateId(evidenceId, "Evidence ID");

        AssessmentScore score = scoreRepository.findById(assessmentScoreId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assessment Score not found with ID: "
                                + assessmentScoreId
                ));

        Evidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Evidence not found with ID: " + evidenceId
                ));

        if (!VERIFIED.equals(evidence.getVerificationStatus())) {
            throw new BusinessException(
                    "Only VERIFIED Evidence can be linked to an Assessment Score"
            );
        }

        Long assessmentResultId =
                score.getAssessmentResult().getId();

        var assessmentResult = resultRepository
                .findWithAssessmentDetailsById(assessmentResultId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assessment Result not found with ID: "
                                + assessmentResultId
                ));

        Long assessmentExerciseId =
                assessmentResult.getAssessment().getExercise().getId();

        Long evidenceExerciseId =
                evidence.getExercise().getId();

        if (!assessmentExerciseId.equals(evidenceExerciseId)) {
            throw new BusinessException(
                    "Evidence and Assessment Score must belong to the same Exercise"
            );
        }

        if (linkRepository
                .existsByAssessmentScore_IdAndEvidence_Id(
                        assessmentScoreId,
                        evidenceId
                )) {
            throw new BusinessException(
                    "Evidence is already linked to this Assessment Score"
            );
        }

        AssessmentScoreEvidence link =
                new AssessmentScoreEvidence(score, evidence);

        return linkRepository.save(link);
    }

    /**
     * Retrieve Evidence linked to an Assessment Score.
     */
    public List<AssessmentScoreEvidenceResponse> getEvidenceByScore(
            Long assessmentScoreId
    ) {
        validateId(assessmentScoreId, "Assessment Score ID");

        if (!scoreRepository.existsById(assessmentScoreId)) {
            throw new ResourceNotFoundException(
                    "Assessment Score not found with ID: "
                            + assessmentScoreId
            );
        }

        return linkRepository
                .findByAssessmentScore_Id(assessmentScoreId)
                .stream()
                .map(AssessmentScoreEvidenceResponse::fromEntity)
                .toList();
    }

    private void validateId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be greater than 0"
            );
        }
    }
}
