package com.cyberrange.backend.assessment.service;

import com.cyberrange.backend.assessment.dto.AutomatedScoringResponse;
import com.cyberrange.backend.assessment.entity.Assessment;
import com.cyberrange.backend.assessment.entity.AssessmentResult;
import com.cyberrange.backend.assessment.entity.AssessmentScore;
import com.cyberrange.backend.assessment.repository.AssessmentRepository;
import com.cyberrange.backend.assessment.repository.AssessmentResultRepository;
import com.cyberrange.backend.assessment.repository.AssessmentScoreRepository;
import com.cyberrange.backend.common.exception.BusinessException;
import com.cyberrange.backend.common.exception.ResourceNotFoundException;
import com.cyberrange.backend.event.dto.EventResponse;
import com.cyberrange.backend.event.repository.EventRepository;
import com.cyberrange.backend.event.repository.SshAlertRepository;
import com.cyberrange.backend.event.repository.SshAlertRepository.Action;
import com.cyberrange.backend.event.service.SshResponseWorkflow;
import com.cyberrange.backend.evidence.entity.Evidence;
import com.cyberrange.backend.evidence.repository.EvidenceRepository;
import com.cyberrange.backend.lab.repository.SshLabRepository;
import com.cyberrange.backend.user.entity.User;
import com.cyberrange.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import com.cyberrange.backend.lab.repository.SshPracticeRepository;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class SshAssessmentEvaluator {

    public static final String EVIDENCE_TYPE = "SSH_VERIFIED_TASK";
    public static final String SCENARIO_NAME = "SSH Security Incident Response";

    public record EvaluationRequest(boolean analysisVerified, String account, String targetHost, boolean finalizeReview) {}
    public record Evaluation(AutomatedScoringResponse scoring, BigDecimal applicableMaximum, BigDecimal finalScore,
                             String reviewStatus) {}
    private record Proof(String name, Instant completed, String reason) {}

    private final AssessmentRepository assessments;
    private final AssessmentResultRepository results;
    private final AssessmentScoreRepository scores;
    private final AssessmentScoreEvidenceService links;
    private final AutomatedScoringService scoring;
    private final EvidenceRepository evidence;
    private final EventRepository events;
    private final SshAlertRepository alerts;
    private final SshResponseWorkflow response;
    private final SshLabRepository labs;
    private final UserRepository users;
    private final JsonMapper json;
    private final SshPracticeRepository practice;
    private final boolean practiceEnabled;

    public SshAssessmentEvaluator(AssessmentRepository assessments, AssessmentResultRepository results,
                                  AssessmentScoreRepository scores, AssessmentScoreEvidenceService links,
                                  AutomatedScoringService scoring, EvidenceRepository evidence, EventRepository events,
                                  SshAlertRepository alerts, SshResponseWorkflow response, SshLabRepository labs,
                                  UserRepository users, JsonMapper json, SshPracticeRepository practice,
                                  @Value("${ssh.practice.enabled:false}") boolean practiceEnabled) {
        this.assessments = assessments;
        this.results = results;
        this.scores = scores;
        this.links = links;
        this.scoring = scoring;
        this.evidence = evidence;
        this.events = events;
        this.alerts = alerts;
        this.response = response;
        this.labs = labs;
        this.users = users;
        this.json = json;
        this.practice = practice;
        this.practiceEnabled = practiceEnabled;
    }

    @Transactional
    public Evaluation evaluate(long exerciseId, long alertId, long actorId, EvaluationRequest request) {
        var alert = alerts.find(exerciseId, alertId, true)
                .orElseThrow(() -> new ResourceNotFoundException("Security Alert not found"));
        if (alert.assessmentId() == null) {
            throw new BusinessException("Assign the Alert to its Team Assessment before evaluation");
        }
        var assessment = assessments.findByIdForUpdate(alert.assessmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        if (!SCENARIO_NAME.equals(assessment.getExercise().getScenario().getName())
                || !assessment.getExercise().getId().equals(exerciseId)) {
            throw new BusinessException("Alert is outside the SSH Assessment scope");
        }
        if (practiceEnabled && !practice.forAssessment(assessment.getId()).isEmpty()) {
            throw new BusinessException("Assessment này thuộc lượt thực hành. Hãy duyệt trên màn hình điều phối bài SSH.");
        }
        var session = labs.find(alert.labSessionId()).orElseThrow();
        if (session.assessmentId() != assessment.getId()) {
            throw new BusinessException("Lab and Alert Assessment do not match");
        }
        var details = response.detail(exerciseId, alertId);
        var first = details.events().stream().filter(event -> "SSH_AUTH_FAILURE".equals(event.eventType())
                        && "OPENSSH_COLLECTOR".equals(event.source())
                        && event.eventTimestamp().equals(alert.firstEventAt().toInstant()))
                .findFirst().orElseThrow(() -> new BusinessException("Alert is missing a verified source authentication event"));
        var actor = users.findById(actorId).orElseThrow(() -> new ResourceNotFoundException("Evaluator not found"));
        var taskResults = results.findByAssessment_IdOrderByDefensiveTask_SequenceOrderAsc(assessment.getId());
        if (taskResults.isEmpty()) {
            throw new BusinessException("Assessment does not contain any task results");
        }
        if (!"REVIEWED".equals(assessment.getReviewStatus())) {
            assessment.start();
            for (AssessmentResult result : taskResults) {
                if (Set.of("PENDING", "IN_PROGRESS").contains(result.getStatus())) {
                    evaluateTask(assessment, result, details, first, session.createdAt(), actor, request);
                }
            }
        }

        var evaluated = scoring.evaluate(assessment.getId());
        if (alert.assessmentId() != null) {
            action(details, "CONTAINMENT_VERIFIED").map(Action::verifiedAt).map(java.time.OffsetDateTime::toInstant)
                    .ifPresent(respondedAt -> taskResults.stream()
                            .filter(result -> "COMPLETED".equals(result.getStatus()))
                            .forEach(result -> result.recordVerifiedSshResponse(alert.detectedAt().toInstant(), respondedAt)));
        }
        BigDecimal maximum = applicableMaximum(taskResults);
        boolean done = taskResults.stream().noneMatch(result -> Set.of("PENDING", "IN_PROGRESS").contains(result.getStatus()));
        if (request.finalizeReview() && !"REVIEWED".equals(assessment.getReviewStatus())) {
            if (!done || !"CLOSED".equals(alert.status())) {
                throw new BusinessException("Close the Alert and complete the applicable tasks before final review");
            }
            assessment.complete(evaluated.totalScore());
            assessment.review(actor, "Reviewed verified SSH workflow evidence for Alert #" + alertId);
        }
        BigDecimal finalScore = "REVIEWED".equals(assessment.getReviewStatus())
                ? normalizedScore(evaluated.totalScore(), maximum) : null;
        return new Evaluation(evaluated, maximum, finalScore, assessment.getReviewStatus());
    }

    static BigDecimal applicableMaximum(List<AssessmentResult> taskResults) {
        return taskResults.stream().filter(result -> !"NOT_APPLICABLE".equals(result.getStatus()))
                .map(result -> result.getDefensiveTask().getMaxScore()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static BigDecimal normalizedScore(BigDecimal awarded, BigDecimal maximum) {
        return maximum.signum() > 0
                ? awarded.multiply(BigDecimal.valueOf(100)).divide(maximum, 2, RoundingMode.HALF_UP) : null;
    }

    private void evaluateTask(Assessment assessment, AssessmentResult result, SshResponseWorkflow.Details details,
                              EventResponse source, Instant labStarted, User actor, EvaluationRequest request) {
        var alert = details.alert();
        var proof = proof(result, details, source, request);
        if (proof.isEmpty()) {
            return;
        }
        var criteria = scores.findByAssessmentResult_IdOrderByIdAsc(result.getId());
        if (criteria.isEmpty() || criteria.stream().anyMatch(score -> !matches(score, proof.get()))) {
            throw new BusinessException("Criteria are not configured for SSH task " + result.getDefensiveTask().getName());
        }
        Proof verified = proof.get();
        var data = json.createObjectNode();
        data.put("proof", verified.name());
        data.put("alertId", alert.id());
        data.put("sourceEventId", source.id());
        data.put("targetHost", source.data().path("targetHost").asString());
        data.put("account", alert.account());
        if (details.report() != null && verified.name().equals("REPORT_SUBMITTED")) {
            data.put("reportId", details.report().id());
        }
        var item = new Evidence(assessment.getExercise(), events.getReferenceById(source.id()), EVIDENCE_TYPE, data);
        item.scopeToAssessment(assessment.getId());
        item.verify("VERIFIED", actor, verified.reason());
        evidence.save(item);
        for (AssessmentScore score : criteria) {
            links.linkEvidence(score.getId(), item.getId());
            if ("MANUAL".equals(score.getCriterion().getCriterionType()) && !"MANUAL".equals(score.getScoreSource())) {
                score.updateScore(score.getMaxScore(), verified.reason());
            }
        }
        var detected = alert.detectedAt().toInstant();
        var responded = action(details, "CONTAINMENT_VERIFIED").map(Action::verifiedAt)
                .map(java.time.OffsetDateTime::toInstant).orElse(null);
        if (responded != null && responded.isAfter(verified.completed())) {
            responded = null;
        }
        result.completeFromVerifiedSshEvidence(labStarted, alert.firstEventAt().toInstant(), detected, responded,
                verified.completed(), verified.reason());
    }

    private boolean matches(AssessmentScore score, Proof proof) {
        var criterion = score.getCriterion();
        var rule = criterion.getScoringRule();
        if ("MANUAL".equals(criterion.getCriterionType())) {
            return Set.of("LOG_ANALYZED", "IDENTITY_VERIFIED").contains(proof.name());
        }
        return rule != null && EVIDENCE_TYPE.equals(rule.get("evidenceType"))
                && rule.get("conditions") instanceof java.util.Map<?, ?> conditions
                && Objects.equals(conditions.get("proof"), proof.name());
    }

    private static Optional<Proof> proof(AssessmentResult result, SshResponseWorkflow.Details details,
                                         EventResponse source, EvaluationRequest request) {
        Instant detection = details.alert().detectedAt().toInstant();
        return switch (result.getDefensiveTask().getSequenceOrder()) {
            case 1 -> Optional.of(new Proof("FAILURE_OBSERVED", detection, "Trusted Collector recorded the SSH authentication failure"));
            case 2 -> Optional.of(new Proof("ALERT_DETECTED", detection, "Detection Engine matched SSH failures in the configured window"));
            case 3 -> request.analysisVerified()
                    ? action(details, "ANALYZED").map(a -> new Proof("LOG_ANALYZED", Instant.now(), "Reviewer checked investigation notes against linked logs"))
                    : Optional.empty();
            case 4 -> details.alert().account().equals(request.account())
                    && source.data().path("targetHost").asString().equals(request.targetHost())
                    ? Optional.of(new Proof("IDENTITY_VERIFIED", Instant.now(), "Reported account and host match the trusted authentication event"))
                    : Optional.empty();
            case 5 -> actionProof(details, "CONTAINMENT_VERIFIED");
            case 6 -> actionProof(details, "RECOVERY_VERIFIED");
            case 7 -> details.report() != null && Set.of("SUBMITTED", "REVIEWED").contains(details.report().status())
                    ? Optional.of(new Proof("REPORT_SUBMITTED", details.report().submittedAt().toInstant(), "Incident Report was submitted for this Team and Alert"))
                    : Optional.empty();
            default -> Optional.empty();
        };
    }

    private static Optional<Proof> actionProof(SshResponseWorkflow.Details details, String kind) {
        return action(details, kind).filter(action -> action.verifiedAt() != null)
                .map(action -> new Proof(kind, action.verifiedAt().toInstant(), action.detail()));
    }

    private static Optional<Action> action(SshResponseWorkflow.Details details, String kind) {
        return details.history().stream().filter(action -> kind.equals(action.action())).findFirst();
    }
}
