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

import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AutomatedScoringService {

    private static final String RULE_TYPE = "EVIDENCE_FIELD_MATCH";

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
                .findWithExerciseAndTeamById(assessmentId)
                .orElseThrow(() -> new BusinessException(
                        "Assessment not found"
                ));

        List<AssessmentResult> results =
                assessmentResultRepository
                        .findByAssessment_IdOrderByDefensiveTask_SequenceOrderAsc(
                                assessmentId
                        );

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

                validateRule(rule);

                BigDecimal ruleScore =
                        toBigDecimal(rule.get("score"));

                BigDecimal awardedScore =
                        ruleScore.min(score.getMaxScore());

                List<AssessmentScoreEvidence> links =
                        scoreEvidenceRepository
                                .findByAssessmentScore_Id(score.getId());

                boolean validTask = !"FAILED".equals(taskStatus)
                        || !SshAssessmentEvaluator.EVIDENCE_TYPE.equals(rule.get("evidenceType"));
                boolean matched = validTask && links.stream()
                        .map(AssessmentScoreEvidence::getEvidence)
                        .anyMatch(evidence ->
                                matchesEvidence(
                                        evidence,
                                        assessment,
                                        rule
                                )
                        );

                if (matched) {
                    score.updateAutomatedScore(
                            awardedScore,
                            "Automated rule matched verified evidence."
                    );
                } else {
                    score.updateAutomatedScore(
                            BigDecimal.ZERO,
                            "No linked verified evidence matched the scoring rule."
                    );
                }

                automatedCount++;

                evaluations.add(toEvaluation(
                        score,
                        matched
                                ? "AUTOMATED_MATCH"
                                : "AUTOMATED_NO_MATCH"
                ));
            }
        }

        // Recalculate total after all eligible scores are evaluated.
        BigDecimal totalScore =
                assessmentScoreRepository
                        .sumAwardedScoreByAssessmentId(assessmentId);

        assessment.updateTotalScore(totalScore);

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

    private boolean matchesEvidence(
            Evidence evidence,
            Assessment assessment,
            Map<String, Object> rule
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

        Object expectedEvidenceType = rule.get("evidenceType");

        if (!(expectedEvidenceType instanceof String evidenceType)
                || !evidenceType.equals(evidence.getEvidenceType())) {
            return false;
        }

        JsonNode data = evidence.getData();

        if (data == null || !data.isObject()) {
            return false;
        }

        Object rawConditions = rule.get("conditions");

        if (!(rawConditions instanceof Map<?, ?> conditions)
                || conditions.isEmpty()) {
            return false;
        }

        // Every configured condition must match.
        for (Map.Entry<?, ?> condition : conditions.entrySet()) {

            if (!(condition.getKey() instanceof String fieldName)) {
                return false;
            }

            JsonNode actualValue = data.get(fieldName);

            if (actualValue == null
                    || actualValue.isNull()
                    || !jsonValueMatches(
                    actualValue,
                    condition.getValue()
            )) {
                return false;
            }
        }

        return true;
    }

    private boolean jsonValueMatches(
            JsonNode actual,
            Object expected
    ) {
        if (expected instanceof Number number) {
            if (!actual.isNumber()) {
                return false;
            }

            return actual.decimalValue().compareTo(
                    new BigDecimal(number.toString())
            ) == 0;
        }

        if (expected instanceof Boolean bool) {
            return actual.isBoolean()
                    && actual.booleanValue() == bool;
        }

        if (expected instanceof String text) {
            return actual.isString()
                    && actual.stringValue().equals(text);
        }

        return false;
    }

    static void validateRule(Map<String, Object> rule) {

        if (!RULE_TYPE.equals(rule.get("ruleType"))) {
            throw new BusinessException(
                    "Unsupported scoring rule type"
            );
        }

        if (!(rule.get("evidenceType") instanceof String evidenceType)
                || evidenceType.isBlank()) {
            throw new BusinessException(
                    "Scoring rule must define evidenceType"
            );
        }

        if (!(rule.get("conditions") instanceof Map<?, ?> conditions)
                || conditions.isEmpty()) {
            throw new BusinessException(
                    "Scoring rule must define non-empty conditions"
            );
        }

        BigDecimal score = toBigDecimal(rule.get("score"));

        if (score.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(
                    "Scoring rule score cannot be negative"
            );
        }
    }

    static BigDecimal toBigDecimal(Object value) {

        if (!(value instanceof Number)
                && !(value instanceof String)) {
            throw new BusinessException(
                    "Scoring rule score must be numeric"
            );
        }

        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException exception) {
            throw new BusinessException(
                    "Invalid scoring rule score"
            );
        }
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

