package com.cyberrange.backend.event.service;

import com.cyberrange.backend.exercise.service.ExerciseLifecycle;

import com.cyberrange.backend.assessment.repository.AssessmentRepository;
import com.cyberrange.backend.assessment.service.BlueSshAccess;
import org.springframework.security.access.AccessDeniedException;
import com.cyberrange.backend.common.exception.BusinessException;
import com.cyberrange.backend.common.exception.ResourceNotFoundException;
import com.cyberrange.backend.event.dto.EventResponse;
import com.cyberrange.backend.event.repository.SshAlertRepository;
import com.cyberrange.backend.event.repository.SshAlertRepository.Action;
import com.cyberrange.backend.event.repository.SshAlertRepository.Alert;
import com.cyberrange.backend.incident.repository.IncidentReportRepository;
import com.cyberrange.backend.incident.repository.IncidentReportRepository.Report;
import com.cyberrange.backend.lab.repository.SshLabRepository;
import com.cyberrange.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SshResponseWorkflow {

    public record Details(Alert alert, List<EventResponse> events, List<Action> history, Report report) {}
    public record Command(String action, String detail, String title, Long assessmentId) {}
    public record Containment(long alertId, String sourceIp) {}
    public record ProbeResult(boolean isolated, boolean recovered) {}

    private final SshAlertRepository alerts;
    private final SshLabRepository labs;
    private final AssessmentRepository assessments;
    private final IncidentReportRepository reports;
    private final EventService events;
    private final UserRepository users;
    private final BlueSshAccess blueAccess;
    private final ExerciseLifecycle lifecycle;

    public SshResponseWorkflow(SshAlertRepository alerts, SshLabRepository labs, AssessmentRepository assessments,
                               IncidentReportRepository reports, EventService events, UserRepository users, BlueSshAccess blueAccess,
                               ExerciseLifecycle lifecycle) {
        this.alerts = alerts;
        this.labs = labs;
        this.assessments = assessments;
        this.reports = reports;
        this.events = events;
        this.users = users;
        this.blueAccess = blueAccess;
        this.lifecycle = lifecycle;
    }

    public List<Alert> list(long exercise, String status) {
        return list(exercise, status, null);
    }

    public List<Alert> list(long exercise, String status, Long studentId) {
        if (status != null && !Set.of("OPEN", "ACKNOWLEDGED", "IN_PROGRESS", "CLOSED").contains(status)) {
            throw new IllegalArgumentException("Unknown Alert status");
        }
        String filter = status == null ? "" : status;
        return studentId == null ? alerts.list(exercise, filter)
                : alerts.listForTeam(exercise, filter, blueAccess.requireTeam(exercise, studentId));
    }

    public Details detail(long exercise, long id) {
        return detail(exercise, id, null);
    }

    public Details detail(long exercise, long id, Long studentId) {
        var alert = requireAlert(exercise, id, false);
        if (studentId != null) {
            requireStudentAlert(alert, studentId);
        }
        return new Details(alert, alerts.eventIds(id).stream().map(events::getEventById).toList(), alerts.history(id),
                alert.reportId() == null ? null : reports.find(alert.reportId()).orElse(null));
    }

    @Transactional
    public Details command(long exercise, long id, long actor, Command command) {
        return command(exercise, id, actor, command, false);
    }

    @Transactional
    public Details command(long exercise, long id, long actor, Command command, boolean student) {
        if (!users.existsById(actor) || command.action() == null) {
            throw new IllegalArgumentException("Invalid response action");
        }
        var alert = requireAlert(exercise, id, true);
        if (student) {
            if (!Set.of("ACKNOWLEDGE", "ANALYZE", "SUBMIT_REPORT").contains(command.action())) {
                throw new AccessDeniedException("Action is reserved for instructors");
            }
            requireStudentAlert(alert, actor);
            lifecycle.requireStudentPractice(exercise);
        }
        if (alerts.isSimulated(alert.id())) {
            throw new BusinessException("Xử lý cảnh báo mô phỏng trong trang Bài thực hành SSH.");
        }
        var history = alerts.history(id);
        switch (command.action()) {
            case "ACKNOWLEDGE" -> {
                requireStatus(alert, "OPEN");
                var session = labs.find(alert.labSessionId()).orElseThrow();
                if (command.assessmentId() == null || command.assessmentId() != session.assessmentId()) {
                    throw new BusinessException("Alert does not belong to the selected Team Assessment");
                }
                alerts.assign(id, session.assessmentId());
                alerts.append(id, actor, "ACKNOWLEDGED", "Alert accepted for investigation", false);
            }
            case "ANALYZE" -> {
                requireStatus(alert, "ACKNOWLEDGED");
                alerts.status(id, "IN_PROGRESS");
                alerts.append(id, actor, "ANALYZED", text(command.detail(), 4000), false);
            }
            case "REQUEST_CONTAINMENT" -> {
                requireStatus(alert, "IN_PROGRESS");
                var session = labs.active().filter(value -> value.id().equals(alert.labSessionId()) && value.desiredRunning())
                        .orElseThrow(() -> new BusinessException("The Alert's Lab is not active"));
                if (session.health() == null || session.heartbeatAt() == null
                        || session.heartbeatAt().isBefore(Instant.now().minusSeconds(30))
                        || !alert.sourceIp().equals(session.health().path("clientAddress").asString())) {
                    throw new BusinessException("The alert source has not been confirmed as the fixed Lab client");
                }
                if (has(history, "CONTAINMENT_VERIFIED") || pending(history)) {
                    throw new BusinessException("Containment has already been requested or verified");
                }
                alerts.append(id, actor, "CONTAINMENT_REQUESTED", text(command.detail(), 2000), false);
            }
            case "SUBMIT_REPORT" -> {
                requireStatus(alert, "IN_PROGRESS");
                if (alert.reportId() != null) {
                    throw new BusinessException("Incident Report already submitted");
                }
                var assessment = assessments.findWithExerciseAndTeamById(alert.assessmentId()).orElseThrow();
                var report = reports.submit(exercise, assessment.getTeam().getId(), actor, text(command.title(), 255),
                        text(command.detail(), 8000), alert.severity());
                alerts.attachReport(id, report.id());
                alerts.append(id, actor, "REPORT_SUBMITTED", "Incident Report #" + report.id() + " submitted", true);
            }
            case "CLOSE" -> {
                requireStatus(alert, "IN_PROGRESS");
                if (!has(history, "CONTAINMENT_VERIFIED") || !has(history, "RECOVERY_VERIFIED") || alert.reportId() == null) {
                    throw new BusinessException("Verified containment, recovery, and a submitted Incident Report are required");
                }
                alerts.status(id, "CLOSED");
                alerts.append(id, actor, "CLOSED", "Response verification and Incident Report confirmed", true);
            }
            default -> throw new IllegalArgumentException("Unknown response action");
        }
        return detail(exercise, id);
    }

    private void requireStudentAlert(Alert alert, long actor) {
        long assessmentId = alert.assessmentId() == null
                ? labs.find(alert.labSessionId()).orElseThrow(() -> new AccessDeniedException("Lab unavailable")).assessmentId()
                : alert.assessmentId();
        blueAccess.requireAssessment(alert.exerciseId(), assessmentId, actor);
    }

    public Containment nextContainment(UUID session) {
        var active = labs.active().filter(value -> value.id().equals(session) && value.desiredRunning());
        if (active.isEmpty()) {
            return null;
        }
        return alerts.nextContainment(session).flatMap(id -> alerts.find(active.get().exerciseId(), id, false))
                .map(alert -> new Containment(alert.id(), alert.sourceIp())).orElse(null);
    }

    @Transactional
    public void recordContainment(UUID session, long alertId, ProbeResult result) {
        var active = labs.active().filter(value -> value.id().equals(session) && value.desiredRunning())
                .orElseThrow(() -> new BusinessException("The Alert's Lab is not active"));
        var alert = requireAlert(active.exerciseId(), alertId, true);
        if (!alert.labSessionId().equals(session)) {
            throw new BusinessException("Alert belongs to a different Lab");
        }
        var history = alerts.history(alertId);
        if (!pending(history)) {
            return;
        }
        var latest = history.stream().filter(action -> "CONTAINMENT_REQUESTED".equals(action.action()))
                .reduce((first, last) -> last).orElseThrow();
        if (!result.isolated()) {
            alerts.append(alertId, latest.actorId(), "CONTAINMENT_FAILED", "Lab runner could not verify network isolation", false);
            return;
        }
        if (!has(history, "CONTAINMENT_VERIFIED")) {
            alerts.append(alertId, latest.actorId(), "CONTAINMENT_VERIFIED", "Lab client removed from the exercise network", true);
        }
        if (result.recovered()) {
            alerts.append(alertId, latest.actorId(), "RECOVERY_VERIFIED", "Independent SSH authentication probe succeeded", true);
        }
    }

    private Alert requireAlert(long exercise, long id, boolean lock) {
        return alerts.find(exercise, id, lock).orElseThrow(() -> new ResourceNotFoundException("Alert not found for this Exercise"));
    }

    private static void requireStatus(Alert alert, String expected) {
        if (!expected.equals(alert.status())) {
            throw new BusinessException("Action requires Alert status " + expected);
        }
    }

    private static boolean has(List<Action> history, String code) {
        return history.stream().anyMatch(action -> code.equals(action.action()));
    }

    private static boolean pending(List<Action> history) {
        return history.stream().map(Action::action)
                .filter(Set.of("CONTAINMENT_REQUESTED", "RECOVERY_VERIFIED", "CONTAINMENT_FAILED")::contains)
                .reduce((first, last) -> last).filter("CONTAINMENT_REQUESTED"::equals).isPresent();
    }

    private static String text(String value, int maxLength) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new IllegalArgumentException("Response text is required and must not exceed " + maxLength + " characters");
        }
        return value.trim();
    }
}
