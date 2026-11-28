package com.cyberrange.backend.assessment.service;

import com.cyberrange.backend.assessment.dto.AutomatedScoringResponse;
import com.cyberrange.backend.assessment.entity.Assessment;
import com.cyberrange.backend.assessment.entity.AssessmentCriterion;
import com.cyberrange.backend.assessment.entity.AssessmentResult;
import com.cyberrange.backend.assessment.entity.AssessmentScore;
import com.cyberrange.backend.assessment.entity.AssessmentScoreEvidence;
import com.cyberrange.backend.assessment.repository.AssessmentRepository;
import com.cyberrange.backend.assessment.repository.AssessmentResultRepository;
import com.cyberrange.backend.assessment.repository.AssessmentScoreEvidenceRepository;
import com.cyberrange.backend.assessment.repository.AssessmentScoreRepository;
import com.cyberrange.backend.common.exception.BusinessException;
import com.cyberrange.backend.evidence.entity.Evidence;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cyberrange.backend.scoring.engine.ScoringEngine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AutomatedScoringService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentResultRepository assessmentResultRepository;
    private final AssessmentScoreRepository assessmentScoreRepository;
    private final AssessmentScoreEvidenceRepository scoreEvidenceRepository;

    public AutomatedScoringService(
            AssessmentRepository assessmentRepository,
            AssessmentResultRepository assessmentResultRepository,
            AssessmentScoreRepository assessmentScoreRepository,
            AssessmentScoreEvidenceRepository scoreEvidenceRepository
    ) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentResultRepository = assessmentResultRepository;
        this.assessmentScoreRepository = assessmentScoreRepository;
        this.scoreEvidenceRepository = scoreEvidenceRepository;
    }

    public AutomatedScoringResponse evaluate(Long assessmentId) {

        if (assessmentId == null || assessmentId <= 0) {
            throw new IllegalArgumentException(
                    "Assessment ID must be positive"
            );
        }

        Assessment assessment = assessmentRepository
                .findByIdForUpdate(assessmentId)
                .orElseThrow(() -> new BusinessException(
                        "Assessment not found"
                ));

        List<AssessmentResult> results =
                assessmentResultRepository
                        .findByAssessment_IdOrderByDefensiveTask_SequenceOrderAsc(
                                assessmentId
                        );

        boolean published = "REVIEWED".equals(assessment.getReviewStatus());
        int automatedCount = 0;
        int manualSkippedCount = 0;
        int unconfiguredCount = 0;
        int taskSkippedCount = 0;

        List<AutomatedScoringResponse.ScoreEvaluation> evaluations =
                new ArrayList<>();

        for (AssessmentResult result : results) {

            String taskStatus = result.getStatus();

            boolean eligible =
                    "COMPLETED".equals(taskStatus)
                            || "FAILED".equals(taskStatus);

            List<AssessmentScore> scores =
                    assessmentScoreRepository
                            .findByAssessmentResult_IdOrderByIdAsc(
                                    result.getId()
                            );

            if (!eligible) {
                taskSkippedCount++;

                for (AssessmentScore score : scores) {
                    evaluations.add(toEvaluation(
                            score,
                            "TASK_SKIPPED"
                    ));
                }

                continue;
            }

            for (AssessmentScore score : scores) {

                if (published) {
                    evaluations.add(toEvaluation(score, "PUBLISHED"));
                    continue;
                }

                // Never overwrite manually assigned scores.
                if ("MANUAL".equals(score.getScoreSource())) {
                    manualSkippedCount++;

                    evaluations.add(toEvaluation(
                            score,
                            "MANUAL_SKIPPED"
                    ));

                    continue;
                }

                AssessmentCriterion criterion = score.getCriterion();

                Map<String, Object> rule =
                        criterion.getScoringRule();

                // No rule configured: preserve the existing score.
                if (rule == null || rule.isEmpty()) {
                    unconfiguredCount++;

                    evaluations.add(toEvaluation(
                            score,
                            "UNCONFIGURED"
                    ));

                    continue;
                }

                ScoringEngine.validateRule(rule);

                BigDecimal ruleScore =
                        ScoringEngine.toBigDecimal(rule.get("score"));

                BigDecimal awardedScore =
                        ruleScore.min(score.getMaxScore());

                List<AssessmentScoreEvidence> links =
                        scoreEvidenceRepository
                                .findByAssessmentScore_Id(score.getId());

                boolean validTask = !"FAILED".equals(taskStatus)
                        || !SshAssessmentEvaluator.EVIDENCE_TYPE.equals(rule.get("evidenceType"));
                var evidenceItems = links.stream()
                        .map(AssessmentScoreEvidence::getEvidence)
                        .filter(evidence -> eligibleEvidence(evidence, assessment))
                        .toList();
                BigDecimal ratio = validTask ? evidenceItems.stream()
                        .map(evidence -> ScoringEngine.evidenceRatio(evidence.getEvidenceType(), evidence.getData(), rule))
                        .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO) : BigDecimal.ZERO;
                boolean matched = ratio.signum() > 0;
                String outcome = matched
                        ? (ratio.compareTo(BigDecimal.ONE) < 0 ? "AUTOMATED_PARTIAL" : "AUTOMATED_MATCH")
                        : "AUTOMATED_NO_MATCH";

                if (matched) {
                    score.updateAutomatedScore(
                            awardedScore.multiply(ratio).setScale(2, java.math.RoundingMode.HALF_UP),
                            ratio.compareTo(BigDecimal.ONE) < 0 ? "Verified selection partially matched the reference set."
                                    : "Automated rule matched verified evidence."
                    );
                } else {
                    String reason = "No linked verified evidence matched the scoring rule.";
                    if (validTask && "TIME_THRESHOLD".equals(rule.get("ruleType"))) {
                        var outcomes = evidenceItems.stream()
                                .filter(evidence -> rule.get("evidenceType").equals(evidence.getEvidenceType()))
                                .map(evidence -> ScoringEngine.timeOutcome(evidence.getData(), rule)).toList();
                        if (outcomes.contains("TIME_THRESHOLD_EXCEEDED")) {
                            outcome = "TIME_THRESHOLD_EXCEEDED";
                            reason = "Verified duration exceeded the configured threshold.";
                        } else if (outcomes.contains("TIME_INVALID")) {
                            outcome = "TIME_INVALID";
                            reason = "Verified Evidence contains an invalid duration.";
                        } else {
                            outcome = "TIME_NOT_MEASURED";
                            reason = "No verified measured duration is available.";
                        }
                    }
                    score.updateAutomatedScore(
                            BigDecimal.ZERO,
                            reason
                    );
                }

                automatedCount++;

                evaluations.add(toEvaluation(score, outcome));
            }
        }

        // Recalculate total after all eligible scores are evaluated.
        BigDecimal totalScore =
                assessmentScoreRepository
                        .sumAwardedScoreByAssessmentId(assessmentId);

        if (published) {
            totalScore = assessment.getTotalScore();
        } else {
            assessment.updateTotalScore(totalScore);
        }

        return new AutomatedScoringResponse(
                assessment.getId(),
                totalScore,
                automatedCount,
                manualSkippedCount,
                unconfiguredCount,
                taskSkippedCount,
                evaluations
        );
    }

    private boolean eligibleEvidence(
            Evidence evidence,
            Assessment assessment
    ) {
        if (evidence == null
                || !"VERIFIED".equals(evidence.getVerificationStatus())) {
            return false;
        }

        // Evidence must belong to the same exercise.
        if (evidence.getExercise() == null
                || !assessment.getExercise().getId()
                .equals(evidence.getExercise().getId())) {
            return false;
        }

        if (SshAssessmentEvaluator.EVIDENCE_TYPE.equals(evidence.getEvidenceType())
                && !assessment.getId().equals(evidence.getSshAssessmentId())) {
            return false;
        }

        return evidence.getSshAssessmentId() == null || assessment.getId().equals(evidence.getSshAssessmentId());
    }

    private AutomatedScoringResponse.ScoreEvaluation toEvaluation(
            AssessmentScore score,
            String evaluationStatus
    ) {
        return new AutomatedScoringResponse.ScoreEvaluation(
                score.getId(),
                score.getAssessmentResult().getId(),
                score.getCriterion().getId(),
                score.getAwardedScore(),
                score.getMaxScore(),
                score.getScoreSource(),
                score.getReason(),
                evaluationStatus
        );
    }
}

