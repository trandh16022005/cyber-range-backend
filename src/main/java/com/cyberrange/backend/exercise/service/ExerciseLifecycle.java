package com.cyberrange.backend.exercise.service;

import com.cyberrange.backend.assessment.repository.AssessmentRepository;
import com.cyberrange.backend.assessment.service.SshAssessmentEvaluator;
import com.cyberrange.backend.common.exception.BusinessException;
import com.cyberrange.backend.common.exception.ResourceNotFoundException;
import com.cyberrange.backend.exercise.dto.ExerciseResponse;
import com.cyberrange.backend.exercise.entity.Exercise;
import com.cyberrange.backend.exercise.repository.ExerciseParticipantRepository;
import com.cyberrange.backend.exercise.repository.ExerciseRepository;
import com.cyberrange.backend.lab.repository.SshLabRepository;
import com.cyberrange.backend.lab.service.SshLabService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ExerciseLifecycle {
    public enum Action {
        PREPARE("READY"), START("RUNNING"), PAUSE("PAUSED"), RESUME("RUNNING"),
        STOP("STOPPED"), COMPLETE("COMPLETED"), CANCEL("CANCELLED");

        private final String target;

        Action(String target) {
            this.target = target;
        }

        boolean endsOperation() {
            return this == STOP || this == COMPLETE || this == CANCEL;
        }
    }

    public record Option(Action action, String blockedReason, boolean confirmationRequired) {}
    public record View(ExerciseResponse exercise, List<Option> actions, SshLabService.Status lab,
                       boolean awaitingLabStop) {}

    private final ExerciseRepository exercises;
    private final AssessmentRepository assessments;
    private final ExerciseParticipantRepository participants;
    private final SshLabRepository labRepository;
    private final SshLabService labs;

    public ExerciseLifecycle(ExerciseRepository exercises, AssessmentRepository assessments,
                             ExerciseParticipantRepository participants, SshLabRepository labRepository, SshLabService labs) {
        this.exercises = exercises;
        this.assessments = assessments;
        this.participants = participants;
        this.labRepository = labRepository;
        this.labs = labs;
    }

    @Transactional(readOnly = true)
    public View get(long id) {
        validateId(id);
        var exercise = exercises.findById(id).orElseThrow(() -> new ResourceNotFoundException("Exercise not found"));
        return view(exercise, labStatus(exercise), false);
    }

    @Transactional
    public View change(long id, Action action, String expectedStatus, boolean environmentConfirmed) {
        validateId(id);
        if (action == null || expectedStatus == null || !legalActions(expectedStatus).contains(action)) {
            throw new IllegalArgumentException("Thao tác hoặc trạng thái Exercise không hợp lệ.");
        }
        // Match the lock order of Lab start and stop, then serialize Exercise mutations.
        labRepository.lockLifecycle();
        var exercise = exercises.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Exercise not found"));
        var lab = labStatus(exercise);
        if (action.target.equals(exercise.getStatus())) {
            return view(exercise, lab, false);
        }
        if (!expectedStatus.equals(exercise.getStatus())) {
            throw new BusinessException("Trạng thái Exercise đã thay đổi. Tải lại trước khi tiếp tục.");
        }
        Option option = option(exercise, lab, action);
        if (option.blockedReason() != null) {
            throw new BusinessException(option.blockedReason());
        }
        if (option.confirmationRequired() && !environmentConfirmed) {
            throw new BusinessException("Xác nhận môi trường thực hành đã sẵn sàng trước khi tiếp tục.");
        }
        if (action.endsOperation() && lab != null && !"STOPPED".equals(lab.status())) {
            return view(exercise, labs.stop(id), true);
        }
        exercise.transitionTo(action.target, Instant.now());
        return view(exercise, lab, false);
    }

    private View view(Exercise exercise, SshLabService.Status lab, boolean waiting) {
        var options = legalActions(exercise.getStatus()).stream().map(action -> option(exercise, lab, action)).toList();
        return new View(ExerciseResponse.fromEntity(exercise), options, lab, waiting);
    }

    private Option option(Exercise exercise, SshLabService.Status lab, Action action) {
        boolean needsEnvironment = action == Action.PREPARE || action == Action.START || action == Action.RESUME;
        String reason = null;
        if (needsEnvironment) {
            if (!"PUBLISHED".equals(exercise.getScenario().getStatus()) || !"ACTIVE".equals(exercise.getCyberRange().getStatus())) {
                reason = "Cần Scenario đã xuất bản và Cyber Range đang hoạt động.";
            } else if (isSsh(exercise) && !readyForBlue(exercise, lab)) {
                reason = "Khởi động Lab của Assessment đội BLUE trong Lab Workflow và đợi Lab READY.";
            }
        } else if (action == Action.COMPLETE) {
            var assigned = assessments.findByExercise_IdOrderByTeam_IdAsc(exercise.getId());
            if (assigned.isEmpty() || assigned.stream().anyMatch(item -> !"REVIEWED".equals(item.getReviewStatus())
                    || !"COMPLETED".equals(item.getStatus()))) {
                reason = "Hoàn tất và duyệt các Assessment của Exercise trước khi kết thúc.";
            }
        }
        return new Option(action, reason, needsEnvironment && !isSsh(exercise));
    }

    private boolean readyForBlue(Exercise exercise, SshLabService.Status lab) {
        return lab != null && lab.ready() && assessments.findWithExerciseAndTeamById(lab.assessmentId())
                .filter(item -> exercise.getId().equals(item.getExercise().getId()))
                .map(item -> participants.existsByExercise_IdAndTeam_IdAndRole(exercise.getId(), item.getTeam().getId(), "BLUE"))
                .orElse(false);
    }

    private SshLabService.Status labStatus(Exercise exercise) {
        return isSsh(exercise) ? labs.status(exercise.getId()) : null;
    }

    private static boolean isSsh(Exercise exercise) {
        return SshAssessmentEvaluator.SCENARIO_NAME.equals(exercise.getScenario().getName());
    }

    private static List<Action> legalActions(String status) {
        return switch (status) {
            case "CREATED" -> List.of(Action.PREPARE, Action.CANCEL);
            case "READY" -> List.of(Action.START, Action.CANCEL);
            case "RUNNING" -> List.of(Action.PAUSE, Action.STOP, Action.COMPLETE);
            case "PAUSED" -> List.of(Action.RESUME, Action.STOP);
            case "STOPPED" -> List.of(Action.COMPLETE, Action.CANCEL);
            default -> List.of();
        };
    }

    private static void validateId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Invalid Exercise ID");
        }
    }
}
