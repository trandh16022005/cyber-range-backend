package com.cyberrange.backend.assessment.service;

import com.cyberrange.backend.assessment.entity.Assessment;
import com.cyberrange.backend.assessment.repository.AssessmentRepository;
import com.cyberrange.backend.assessment.repository.AssessmentResultRepository;
import com.cyberrange.backend.assessment.repository.AssessmentScoreRepository;
import com.cyberrange.backend.common.exception.BusinessException;
import com.cyberrange.backend.evidence.entity.Evidence;
import com.cyberrange.backend.evidence.repository.EvidenceRepository;
import com.cyberrange.backend.lab.repository.SshPracticeRepository.Run;
import com.cyberrange.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class SshPracticeAssessmentPublisher {

    private static final List<String> PROOFS = List.of("FAILURE_OBSERVED", "ALERT_DETECTED", "LOG_ANALYZED",
            "IDENTITY_VERIFIED", "CONTAINMENT_VERIFIED", "RECOVERY_VERIFIED", "REPORT_SUBMITTED");

    private final AssessmentRepository assessments;
    private final AssessmentResultRepository results;
    private final AssessmentScoreRepository scores;
    private final AssessmentScoreEvidenceService links;
    private final EvidenceRepository evidence;
    private final UserRepository users;
    private final AutomatedScoringService scoring;
    private final JsonMapper json;

    public SshPracticeAssessmentPublisher(AssessmentRepository assessments, AssessmentResultRepository results,
                                          AssessmentScoreRepository scores, AssessmentScoreEvidenceService links,
                                          EvidenceRepository evidence, UserRepository users,
                                          AutomatedScoringService scoring, JsonMapper json) {
        this.assessments = assessments;
        this.results = results;
        this.scores = scores;
        this.links = links;
        this.evidence = evidence;
        this.users = users;
        this.scoring = scoring;
        this.json = json;
    }

    public void requireUnreviewed(Assessment assessment) {
        if (!"PENDING".equals(assessment.getStatus()) || "REVIEWED".equals(assessment.getReviewStatus())
                || results.findByAssessment_IdOrderByDefensiveTask_SequenceOrderAsc(assessment.getId()).stream()
                .anyMatch(result -> !"PENDING".equals(result.getStatus()))) {
            throw new BusinessException("Assessment đã có kết quả. Chọn Assessment mới cho lượt thực hành.");
        }
    }

    @Transactional
    public int publish(Run run, ObjectNode submission, long reviewer, boolean[] passed) {
        var assessment = assessments.findByIdForUpdate(run.assessmentId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy Assessment."));
        if (assessment.getExercise().getId() != run.exerciseId() || assessment.getTeam().getId() != run.blueTeamId()
                || !BlueSshAccess.isSsh(assessment)) {
            throw new BusinessException("Assessment nằm ngoài phạm vi lượt thực hành.");
        }
        requireUnreviewed(assessment);
        var actor = users.findById(reviewer).orElseThrow(() -> new BusinessException("Không tìm thấy người duyệt."));
        var tasks = results.findByAssessment_IdOrderByDefensiveTask_SequenceOrderAsc(assessment.getId());
        if (tasks.size() != PROOFS.size() || SshAssessmentEvaluator.applicableMaximum(tasks).intValueExact() != 100) {
            throw new BusinessException("Assessment SSH cần đủ bảy tiêu chí, tổng 100 điểm.");
        }
        Instant started = timestamp(submission, "practiceStartedAt");
        Instant acknowledged = timestamp(submission, "studentAcknowledgedAt");
        Instant proposed = timestamp(submission, "responseProposedAt");
        Instant submitted = timestamp(submission, "reportSubmittedAt");
        assessment.start(started);
        var marks = submission.putArray("marks");
        for (int i = 0; i < tasks.size(); i++) {
            var task = tasks.get(i);
            var entries = scores.findByAssessmentResult_IdOrderByIdAsc(task.getId());
            if (task.getDefensiveTask().getSequenceOrder() != i + 1 || entries.size() != 1
                    || entries.getFirst().getMaxScore().compareTo(task.getDefensiveTask().getMaxScore()) != 0) {
                throw new BusinessException("Cấu hình Criterion của bài SSH không hợp lệ.");
            }
            var score = entries.getFirst();
            String proof = PROOFS.get(i);
            var condition = score.getCriterion().getScoringRule();
            boolean manual = "MANUAL".equals(score.getCriterion().getCriterionType());
            if (!manual && (condition == null || !SshAssessmentEvaluator.EVIDENCE_TYPE.equals(condition.get("evidenceType"))
                    || !(condition.get("conditions") instanceof java.util.Map<?, ?> fields)
                    || !Objects.equals(fields.get("proof"), proof))) {
                throw new BusinessException("Scoring Rule không khớp tiêu chí SSH.");
            }
            String reason = passed[i] ? "Bằng chứng bài thực hành mô phỏng đáp ứng tiêu chí."
                    : "Bằng chứng bài thực hành mô phỏng chưa đáp ứng tiêu chí.";
            ObjectNode data = json.createObjectNode();
            data.put("origin", "TRAINING_SIMULATION");
            data.put("practiceRunId", run.id().toString());
            data.put("scenarioVersion", run.version());
            data.put("proof", passed[i] ? proof : "CRITERION_NOT_MET");
            data.set("observationIds", submission.path("observationIds"));
            var item = new Evidence(assessment.getExercise(), null, SshAssessmentEvaluator.EVIDENCE_TYPE, data);
            item.scopeToAssessment(assessment.getId());
            item.verify("VERIFIED", actor, reason);
            evidence.save(item);
            links.linkEvidence(score.getId(), item.getId());
            if (manual && !"MANUAL".equals(score.getScoreSource())) {
                score.updateScore(passed[i] ? score.getMaxScore() : java.math.BigDecimal.ZERO, reason);
            }
            task.completeFromVerifiedSshEvidence(started, started, acknowledged, proposed, submitted, reason, passed[i]);
            var mark = marks.addObject();
            mark.put("name", task.getDefensiveTask().getName());
            mark.put("maximum", score.getMaxScore());
            mark.put("evidenceId", item.getId());
        }
        var evaluated = scoring.evaluate(assessment.getId());
        for (int i = 0; i < tasks.size(); i++) {
            var score = scores.findByAssessmentResult_IdOrderByIdAsc(tasks.get(i).getId()).getFirst();
            var mark = (ObjectNode) marks.get(i);
            mark.put("points", score.getAwardedScore());
            mark.put("reason", score.getReason());
        }
        assessment.complete(evaluated.totalScore());
        assessment.review(actor, "[Mô phỏng] " + submission.path("review").asString());
        return evaluated.totalScore().intValueExact();
    }

    private static Instant timestamp(JsonNode submission, String field) {
        if (!submission.hasNonNull(field)) {
            throw new BusinessException("Thiếu mốc thao tác của lượt thực hành. Mở một lượt mới.");
        }
        return Instant.parse(submission.path(field).asString());
    }
}

