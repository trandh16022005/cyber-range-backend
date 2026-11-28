package com.cyberrange.backend.lab.service;

import com.cyberrange.backend.lab.dto.SshPracticeActionRequest;
import com.cyberrange.backend.lab.dto.SshPracticeResponse;
import com.cyberrange.backend.lab.dto.SshPracticeMetricsResponse;
import com.cyberrange.backend.common.exception.ServiceUnavailableException;
import com.cyberrange.backend.assessment.repository.AssessmentRepository;
import com.cyberrange.backend.assessment.repository.AssessmentResultRepository;
import com.cyberrange.backend.lab.dto.SshPracticeTaskResponse;
import com.cyberrange.backend.assessment.entity.Assessment;
import com.cyberrange.backend.assessment.service.BlueSshAccess;
import com.cyberrange.backend.assessment.service.SshPracticeAssessmentPublisher;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import com.cyberrange.backend.common.exception.BusinessException;
import com.cyberrange.backend.common.exception.ResourceNotFoundException;
import com.cyberrange.backend.event.service.SshPracticeScenario;
import com.cyberrange.backend.event.service.SshObservationService;
import com.cyberrange.backend.event.service.SshPracticeScenario.Observation;
import com.cyberrange.backend.exercise.repository.ExerciseParticipantRepository;
import com.cyberrange.backend.exercise.service.ExerciseLifecycle;
import com.cyberrange.backend.lab.repository.SshPracticeRepository;
import com.cyberrange.backend.lab.repository.SshPracticeRepository.Run;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SshPracticeService {



    private static final List<String> REPORT_SECTIONS = List.of("summary", "timeline", "relatedEvents", "affectedAccount",
            "affectedHost", "findings", "proposedResponse", "responseOutcome", "recoveryVerification", "lessonsLearned");

    private final SshPracticeRepository runs;
    private final SshLabService labs;
    private final AssessmentRepository assessments;
    private final AssessmentResultRepository assessmentResults;
    private final ExerciseParticipantRepository participants;
    private final JsonMapper json;
    private final SshPracticeAssessmentPublisher publisher;
    private final ExerciseLifecycle lifecycle;
    private final SshObservationService observations;

    public SshPracticeService(SshPracticeRepository runs, SshLabService labs, AssessmentRepository assessments,
                              ExerciseParticipantRepository participants, JsonMapper json, SshPracticeAssessmentPublisher publisher,
                              ExerciseLifecycle lifecycle, SshObservationService observations, AssessmentResultRepository assessmentResults) {
        this.runs = runs;
        this.labs = labs;
        this.assessments = assessments;
        this.assessmentResults = assessmentResults;
        this.participants = participants;
        this.json = json;
        this.publisher = publisher;
        this.lifecycle = lifecycle;
        this.observations = observations;
    }

    public void requireSchema() {
        if (!runs.schemaReady()) {
            throw new ServiceUnavailableException("Bài thực hành SSH chưa sẵn sàng. Vui lòng liên hệ giảng viên hoặc quản trị viên.");
        }
    }

    public List<SshPracticeResponse> assigned(long actor) {
        return runs.forActor(actor).stream().map(run -> view(run, role(run, actor))).toList();
    }

    public List<SshPracticeResponse> forAssessment(long assessmentId) {
        return runs.forAssessment(assessmentId).stream().map(run -> view(run, "INSTRUCTOR")).toList();
    }

    @Transactional
    public SshPracticeResponse approve(long assessmentId, long redTeam, long instructor) {
        return approve(assessmentId, redTeam, instructor, null);
    }

    @Transactional
    public SshPracticeResponse approve(long assessmentId, long redTeam, long instructor, UUID expectedSession) {
        lifecycle.lockLabChanges();
        var assessment = assessments.findByIdForUpdate(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        var lab = validateApproval(assessment, redTeam);
        if (expectedSession != null && !expectedSession.equals(lab.id())) {
            throw new BusinessException("Phiên Lab đã thay đổi. Kiểm tra lại trước khi duyệt.");
        }
        var existing = runs.forSession(lab.id());
        if (existing.isPresent()) {
            return view(existing.get(), "INSTRUCTOR");
        }
        UUID id = UUID.randomUUID();
        runs.create(id, lab.id(), assessment.getExercise().getId(), assessmentId, redTeam,
                assessment.getTeam().getId(), SshPracticeScenario.VERSION);
        var run = runs.forSession(lab.id()).orElseThrow();
        if (run.id().equals(id)) {
            runs.history(id, instructor, "APPROVED", null);
        }
        return view(run, "INSTRUCTOR");
    }

    public UUID approvalSession(long assessmentId, long redTeam) {
        var assessment = assessments.findWithExerciseAndTeamById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        return validateApproval(assessment, redTeam).id();
    }

    private SshLabService.Status validateApproval(Assessment assessment, long redTeam) {
        long assessmentId = assessment.getId();
        long exercise = assessment.getExercise().getId();
        long blue = assessment.getTeam().getId();
        if (!BlueSshAccess.isSsh(assessment) || redTeam == blue) {
            throw new BusinessException("Cần hai đội RED và BLUE khác nhau trong bài SSH.");
        }
        if (!assessment.getExercise().allowsStudentPractice()) {
            throw new BusinessException("Exercise đang tạm dừng hoặc đã kết thúc.");
        }
        var roster = participants.findByExercise_IdOrderByJoinedAtAsc(exercise);
        boolean hasRed = roster.stream().anyMatch(p -> "RED".equals(p.getRole()) && p.getTeam() != null
                && p.getTeam().getId() == redTeam && "STUDENT".equals(p.getUser().getRole().getName()));
        boolean hasBlue = roster.stream().anyMatch(p -> "BLUE".equals(p.getRole()) && p.getTeam() != null
                && p.getTeam().getId() == blue && "STUDENT".equals(p.getUser().getRole().getName()));
        if (!hasRed || !hasBlue) {
            throw new BusinessException("Phân công ít nhất một học viên STUDENT cho mỗi đội RED và BLUE trước khi duyệt.");
        }
        var lab = labs.status(exercise);
        if (lab == null || !lab.ready() || lab.assessmentId() != assessmentId) {
            throw new BusinessException("Khởi động đúng Lab của Assessment và đợi READY.");
        }
        var existing = runs.forSession(lab.id());
        if (existing.isPresent()) {
            if (existing.get().redTeamId() != redTeam) {
                throw new BusinessException("Phiên Lab này đã cấp lượt cho một đội RED khác.");
            }
            return lab;
        }
        publisher.requireUnreviewed(assessment);
        if (runs.forAssessment(assessmentId).stream().anyMatch(previous ->
                !labs.isStopped(previous.labSessionId()) || Set.of("SUBMITTED", "REVIEWED").contains(previous.state()))) {
            throw new BusinessException("Assessment đã có một lượt thực hành đang xử lý hoặc đã nộp.");
        }
        return lab;
    }

    @Transactional
    public SshPracticeResponse act(UUID id, long actor, boolean instructor, SshPracticeActionRequest request) {
        if (request.action() == null) {
            throw new IllegalArgumentException("Thiếu thao tác.");
        }
        var run = lock(id);
        String role = instructor ? "INSTRUCTOR" : role(run, actor);
        if (!instructor) {
            lifecycle.requireStudentPractice(run.exerciseId());
        }
        ObjectNode input = json.createObjectNode().setAll((ObjectNode) run.submission());
        String next = run.state();
        Integer score = run.score();
        String note = null;
        Instant now = Instant.now();
        switch (request.action()) {
            case "CHECK_CONNECTION" -> {
                requireRole(role, "RED");
                requireReady(run);
                if (runs.connection(id, "NONE", "QUEUED")) {
                    runs.history(id, actor, request.action(), null);
                }
                return view(lock(id), role);
            }
            case "START_SCENARIO" -> {
                requireRole(role, "RED");
                if (!canStartScenario(run)) {
                    return view(run, role);
                }
                requireReady(run);
                if (!"SUCCEEDED".equals(run.connectionStatus())) {
                    throw new BusinessException("Cần xác nhận kết nối từ Linux client trước.");
                }
                if (!input.hasNonNull("practiceStartedAt")) {
                    input.put("practiceStartedAt", now.toString());
                }
                var detection = observations.ingestPractice(run.labSessionId(), id, now);
                input.put("simulationPublishedAt", now.toString());
                input.put("simulatedAlert", detection.isPresent());
                detection.ifPresent(result -> {
                    input.put("simulatedAlertId", result.alertId());
                    input.put("systemDetectedAt", result.detectedAt().toString());
                    input.put("systemDetectionMillis", Duration.between(now, result.detectedAt()).toMillis());
                });
                next = "ACTIVE";
            }
            case "ACKNOWLEDGE" -> {
                requireRole(role, "BLUE");
                requireState(run, "ACTIVE");
                if (!input.path("simulatedAlert").asBoolean(false)) {
                    throw new BusinessException("Lượt này chưa có cảnh báo mô phỏng.");
                }
                if (input.hasNonNull("studentAcknowledgedAt")) return view(run, role);
                input.put("studentAcknowledgedAt", now.toString());
            }
            case "SAVE_INVESTIGATION" -> {
                requireRole(role, "BLUE");
                requireState(run, "ACTIVE");
                requireAcknowledgment(input);
                var ids = request.observationIds();
                var available = SshPracticeScenario.OBSERVATIONS.stream().map(Observation::id).toList();
                if (ids == null || ids.isEmpty() || ids.size() > available.size() || !available.containsAll(ids)
                        || new HashSet<>(ids).size() != ids.size()) {
                    throw new IllegalArgumentException("Chọn các bản ghi hợp lệ trong lượt này.");
                }
                var selected = input.putArray("observationIds");
                ids.forEach(selected::add);
                input.put("analysis", required(request.note(), 10, 4000));
                input.put("account", required(request.account(), 1, 100));
                input.put("target", required(request.target(), 1, 255));
                input.put("source", required(request.source(), 1, 45));
                input.put("impact", required(request.impact(), 3, 1000));
                if (!input.hasNonNull("investigationStartedAt")) input.put("investigationStartedAt", now.toString());
            }
            case "PROPOSE_RESPONSE" -> {
                requireRole(role, "BLUE");
                requireState(run, "ACTIVE");
                if (!input.has("analysis")) {
                    throw new BusinessException("Lưu điều tra trước khi gửi đề xuất.");
                }
                input.put("proposal", required(request.note(), 10, 4000));
                input.put("responseProposedAt", now.toString());
                next = "PROPOSED";
            }
            case "APPROVE_RESPONSE", "RETURN_RESPONSE" -> {
                requireRole(role, "INSTRUCTOR");
                requireState(run, "PROPOSED");
                note = required(request.note(), 3, 4000);
                input.put("responseReview", note);
                next = "APPROVE_RESPONSE".equals(request.action()) ? "RECOVERY" : "ACTIVE";
                if ("RECOVERY".equals(next)) {
                    input.put("responseApprovedAt", now.toString());
                    input.put("simulatedResponseCompletedAt", Instant.now().toString());
                }
            }
            case "SAVE_REPORT_DRAFT" -> {
                requireRole(role, "BLUE");
                requireState(run, "RECOVERY");
                saveReport(input, request.reportSections(), false);
                if (request.recovered() != null) input.put("recovered", request.recovered());
            }
            case "SUBMIT_REPORT" -> {
                requireRole(role, "BLUE");
                if ("SUBMITTED".equals(run.state())) return view(run, role);
                requireState(run, "RECOVERY");
                if (request.recovered() == null) {
                    throw new IllegalArgumentException("Chọn kết luận phục hồi dựa trên log.");
                }
                input.put("recovered", request.recovered());
                saveReport(input, request.reportSections(), true);
                input.put("recoveryReviewedAt", now.toString());
                input.put("reportSubmittedAt", now.toString());
                input.put("closureProposedAt", now.toString());
                input.put("reportVersion", input.path("reportVersion").asInt(0) + 1);
                input.put("submittedBy", actor);
                input.remove("returnReason");
                var snapshot = json.createObjectNode();
                snapshot.put("exerciseId", run.exerciseId());
                snapshot.put("assessmentId", run.assessmentId());
                snapshot.put("teamId", run.blueTeamId());
                snapshot.put("labSessionId", run.labSessionId().toString());
                snapshot.set("submission", input.deepCopy());
                runs.history(id, actor, "SUBMISSION_SNAPSHOT", snapshot.toString());
                next = "SUBMITTED";
            }
            case "RETURN_SUBMISSION" -> {
                requireRole(role, "INSTRUCTOR");
                requireState(run, "SUBMITTED");
                note = required(request.note(), 3, 4000);
                input.put("returnReason", note);
                input.remove("confirmedReview");
                next = "RECOVERY";
            }
            case "CONFIRM_REVIEW" -> {
                requireRole(role, "INSTRUCTOR");
                requireState(run, "SUBMITTED");
                if (request.analysisAccepted() == null || request.reportAccepted() == null) {
                    throw new IllegalArgumentException("Đánh giá cả phân tích và báo cáo.");
                }
                note = required(request.note(), 3, 4000);
                var confirmation = input.putObject("confirmedReview");
                confirmation.put("note", note);
                confirmation.put("analysisAccepted", request.analysisAccepted());
                confirmation.put("reportAccepted", request.reportAccepted());
                confirmation.put("version", input.path("reportVersion").asInt());
            }
            case "PUBLISH_REVIEW" -> {
                requireRole(role, "INSTRUCTOR");
                if ("REVIEWED".equals(run.state())) return view(run, role);
                requireState(run, "SUBMITTED");
                var confirmation = input.path("confirmedReview");
                if (!confirmation.isObject() || confirmation.path("version").asInt() != input.path("reportVersion").asInt()) {
                    throw new BusinessException("Xác nhận kết quả của phiên bản hiện tại trước khi công bố.");
                }
                note = confirmation.path("note").asString();
                input.put("review", note);
                score = publisher.publish(run, input, actor,
                        criteriaMet(run.id(), input, confirmation.path("analysisAccepted").asBoolean(),
                                confirmation.path("reportAccepted").asBoolean()));
                input.put("assessmentReviewedAt", now.toString());
                input.remove("confirmedReview");
                next = "REVIEWED";
            }
            case "REVIEW" -> {
                requireRole(role, "INSTRUCTOR");
                if ("REVIEWED".equals(run.state())) return view(run, role);
                requireState(run, "SUBMITTED");
                if (input.hasNonNull("submittedBy")) {
                    throw new BusinessException("Xác nhận kết quả trước khi công bố bài nộp.");
                }
                if (request.analysisAccepted() == null || request.reportAccepted() == null) {
                    throw new IllegalArgumentException("Đánh giá cả phân tích và báo cáo.");
                }
                note = required(request.note(), 3, 4000);
                input.put("review", note);
                score = publisher.publish(run, input, actor,
                        criteriaMet(run.id(), input, request.analysisAccepted(), request.reportAccepted()));
                input.put("assessmentReviewedAt", now.toString());
                next = "REVIEWED";
            }
            default -> throw new IllegalArgumentException("Thao tác chưa được hỗ trợ.");
        }
        runs.save(id, next, input, score);
        runs.history(id, actor, request.action(), note);
        return view(lock(id), role);
    }

    @Transactional
    public UUID claimConnection(UUID session) {
        var run = runs.forSession(session).orElse(null);
        if (run == null) return null;
        var lab = labs.status(run.exerciseId());
        if (lab == null || !lab.id().equals(session) || !lab.ready()) return null;
        if ("CHECKING".equals(run.connectionStatus()) && run.connectionAt().isBefore(OffsetDateTime.now().minusMinutes(1))) {
            if (runs.connection(run.id(), "CHECKING", "FAILED")) {
                runs.history(run.id(), null, "CONNECTION_TIMEOUT", null);
            }
            return null;
        }
        return runs.connection(run.id(), "QUEUED", "CHECKING") ? run.id() : null;
    }

    @Transactional
    public void finishConnection(UUID session, UUID id, boolean success) {
        var run = lock(id);
        if (!run.labSessionId().equals(session)) {
            throw new AccessDeniedException("Khác phiên Lab.");
        }
        if (runs.connection(id, "CHECKING", success ? "SUCCEEDED" : "FAILED")) {
            runs.history(id, null, success ? "CONNECTION_CONFIRMED" : "CONNECTION_FAILED", null);
        }
    }

    private SshPracticeResponse view(Run run, String role) {
        var lab = labs.status(run.exerciseId());
        boolean ready = lab != null && lab.id().equals(run.labSessionId()) && lab.ready();
        boolean reviewAccess = !"RED".equals(role);
        boolean released = !"APPROVED".equals(run.state()) && reviewAccess;
        boolean recoveryReleased = Set.of("RECOVERY", "SUBMITTED", "REVIEWED").contains(run.state()) && reviewAccess;
        return new SshPracticeResponse(run.id(), run.exerciseId(), run.assessmentId(), run.redTeamId(), run.blueTeamId(), role,
                run.state(), ready, "RED".equals(role) && canStartScenario(run), run.connectionStatus(), run.version(), "ubuntu-target",
                reviewAccess ? assessmentResults.findByAssessment_IdOrderByDefensiveTask_SequenceOrderAsc(run.assessmentId()).stream()
                        .map(result -> SshPracticeTaskResponse.from(result.getDefensiveTask())).toList() : List.of(),
                released ? SshPracticeScenario.OBSERVATIONS : List.of(),
                recoveryReleased ? SshPracticeScenario.RECOVERY : List.of(),
                released && run.submission().path("simulatedAlert").asBoolean(false),
                reviewAccess ? run.submission() : null, reviewAccess ? run.score() : null,
                reviewAccess ? runs.history(run.id()).stream().filter(entry -> !"SUBMISSION_SNAPSHOT".equals(entry.action())).toList()
                        : runs.history(run.id()).stream()
                        .filter(entry -> Set.of("CHECK_CONNECTION", "CONNECTION_CONFIRMED", "CONNECTION_FAILED",
                                "CONNECTION_TIMEOUT", "START_SCENARIO").contains(entry.action())).toList(),
                reviewAccess ? runs.observedEvents(run.labSessionId(), run.exerciseId()) : List.of(),
                reviewAccess ? metrics(run.submission()) : null);
    }

    private static boolean canStartScenario(Run run) {
        return "APPROVED".equals(run.state()) || ("ACTIVE".equals(run.state())
                && !run.submission().hasNonNull("simulationPublishedAt")
                && !run.submission().hasNonNull("simulatedAlertId"));
    }

    private Run lock(UUID id) {
        return runs.lock(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt thực hành."));
    }

    private String role(Run run, long actor) {
        var member = participants.findByExercise_IdAndUser_Id(run.exerciseId(), actor)
                .orElseThrow(() -> new AccessDeniedException("Bạn chưa được phân công."));
        if ("WHITE".equals(member.getRole())) return "WHITE";
        if (member.getTeam() == null) throw new AccessDeniedException("Bạn chưa được phân công vào một đội.");
        if ("RED".equals(member.getRole()) && member.getTeam().getId() == run.redTeamId()) return "RED";
        if ("BLUE".equals(member.getRole()) && member.getTeam().getId() == run.blueTeamId()) return "BLUE";
        throw new AccessDeniedException("Lượt thực hành thuộc đội khác.");
    }

    private void requireReady(Run run) {
        var lab = labs.status(run.exerciseId());
        if (lab == null || !lab.id().equals(run.labSessionId()) || !lab.ready()) {
            throw new BusinessException("Chờ giảng viên đưa Lab về READY.");
        }
    }

    private static void requireRole(String actual, String expected) {
        if (!expected.equals(actual)) throw new AccessDeniedException("Vai trò không được phép thực hiện thao tác.");
    }

    private static void requireState(Run run, String expected) {
        if (!expected.equals(run.state())) throw new BusinessException("Thao tác không hợp lệ ở bước hiện tại.");
    }

    private static String required(String value, int min, int max) {
        if (value == null || value.strip().length() < min || value.length() > max) {
            throw new IllegalArgumentException("Nội dung phải dài từ " + min + " đến " + max + " ký tự.");
        }
        return value.strip();
    }

    private static void requireAcknowledgment(ObjectNode input) {
        if (!input.hasNonNull("studentAcknowledgedAt")) {
            throw new BusinessException("Xác nhận đã nhận diện sự cố trước khi lưu điều tra.");
        }
    }

    private static void saveReport(ObjectNode input, Map<String, String> sections, boolean submit) {
        if (sections == null || !REPORT_SECTIONS.containsAll(sections.keySet())) {
            throw new IllegalArgumentException("Các trường báo cáo không hợp lệ.");
        }
        var draft = input.putObject("reportSections");
        for (String section : REPORT_SECTIONS) {
            String value = sections.getOrDefault(section, "");
            draft.put(section, submit ? required(value, 3, 2000) :
                    value == null || value.isBlank() ? "" : required(value, 1, 2000));
        }
    }

    private static SshPracticeMetricsResponse metrics(JsonNode input) {
        return new SshPracticeMetricsResponse(input.hasNonNull("systemDetectionMillis") ? input.path("systemDetectionMillis").asLong() : null,
                elapsed(input, "practiceStartedAt", "studentAcknowledgedAt"),
                elapsed(input, "studentAcknowledgedAt", "responseProposedAt"),
                elapsed(input, "responseApprovedAt", "simulatedResponseCompletedAt"));
    }

    private static Long elapsed(JsonNode input, String from, String to) {
        if (!input.hasNonNull(from) || !input.hasNonNull(to)) return null;
        return Duration.between(Instant.parse(input.path(from).asString()),
                Instant.parse(input.path(to).asString())).toSeconds();
    }

    private boolean[] criteriaMet(UUID runId, ObjectNode input, boolean analysisAccepted, boolean reportAccepted) {
        var selection = new HashSet<Long>();
        input.path("observationIds").forEach(node -> selection.add(node.asLong()));
        Set<Long> correlated = input.hasNonNull("simulatedAlertId")
                ? Set.copyOf(observations.practiceObservationIds(input.path("simulatedAlertId").asLong(), runId))
                : SshPracticeScenario.alertObservations();
        var reference = SshPracticeScenario.OBSERVATIONS.stream().filter(row -> correlated.contains(row.id()))
                .findFirst().orElseThrow();
        return new boolean[] {
                selection.stream().anyMatch(correlated::contains),
                selection.equals(correlated) && reference.source().equals(input.path("source").asString()),
                analysisAccepted,
                reference.account().equals(input.path("account").asString()) && reference.target().equals(input.path("target").asString()),
                input.hasNonNull("simulatedResponseCompletedAt"), input.path("recovered").asBoolean(false), reportAccepted
        };
    }
}
