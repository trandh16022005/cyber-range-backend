package com.cyberrange.backend.assessment.service;

import com.cyberrange.backend.assessment.dto.AssessmentResponse;
import com.cyberrange.backend.assessment.dto.AssessmentResultResponse;
import com.cyberrange.backend.assessment.dto.AssessmentScoreResponse;
import com.cyberrange.backend.assessment.entity.Assessment;
import com.cyberrange.backend.assessment.entity.AssessmentCriterion;
import com.cyberrange.backend.assessment.entity.AssessmentResult;
import com.cyberrange.backend.assessment.entity.AssessmentScore;
import com.cyberrange.backend.assessment.repository.AssessmentCriterionRepository;
import com.cyberrange.backend.assessment.repository.AssessmentRepository;
import com.cyberrange.backend.assessment.repository.AssessmentResultRepository;
import com.cyberrange.backend.assessment.repository.AssessmentScoreRepository;
import com.cyberrange.backend.common.exception.BusinessException;
import com.cyberrange.backend.common.exception.ResourceNotFoundException;
import com.cyberrange.backend.exercise.entity.Exercise;
import com.cyberrange.backend.exercise.entity.ExerciseParticipant;
import com.cyberrange.backend.exercise.repository.ExerciseParticipantRepository;
import com.cyberrange.backend.exercise.repository.ExerciseRepository;
import com.cyberrange.backend.scenario.entity.DefensiveTask;
import com.cyberrange.backend.scenario.repository.DefensiveTaskRepository;
import com.cyberrange.backend.team.entity.Team;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AssessmentService {

    private final ExerciseRepository exerciseRepository;
    private final ExerciseParticipantRepository participantRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentResultRepository resultRepository;
    private final AssessmentCriterionRepository criterionRepository;
    private final AssessmentScoreRepository scoreRepository;
    private final DefensiveTaskRepository defensiveTaskRepository;

    public AssessmentService(
            ExerciseRepository exerciseRepository,
            ExerciseParticipantRepository participantRepository,
            AssessmentRepository assessmentRepository,
            AssessmentResultRepository resultRepository,
            AssessmentCriterionRepository criterionRepository,
            AssessmentScoreRepository scoreRepository,
            DefensiveTaskRepository defensiveTaskRepository
    ) {
        this.exerciseRepository = exerciseRepository;
        this.participantRepository = participantRepository;
        this.assessmentRepository = assessmentRepository;
        this.resultRepository = resultRepository;
        this.criterionRepository = criterionRepository;
        this.scoreRepository = scoreRepository;
        this.defensiveTaskRepository = defensiveTaskRepository;
    }

    /**
     * Initialize assessments for all BLUE teams in an exercise.
     *
     * Each eligible team receives one assessment.
     * Each defensive task creates one result.
     * Each criterion creates one score.
     *
     * Existing assessments are skipped.
     */
    @Transactional
    public List<AssessmentResponse> initializeAssessments(
            Long exerciseId
    ) {

        validateId(exerciseId, "Exercise ID");

        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Exercise not found with ID: " + exerciseId
                ));

        Map<Long, Team> blueTeams = new LinkedHashMap<>();

        List<ExerciseParticipant> participants =
                participantRepository
                        .findByExercise_IdOrderByJoinedAtAsc(exerciseId);

        for (ExerciseParticipant participant : participants) {

            if (!"BLUE".equalsIgnoreCase(participant.getRole())) {
                continue;
            }

            Team team = participant.getTeam();

            if (team == null) {
                continue;
            }

            blueTeams.putIfAbsent(team.getId(), team);
        }

        if (blueTeams.isEmpty()) {
            return List.of();
        }

        List<Team> teamsToAssess = blueTeams.values()
                .stream()
                .filter(team -> !assessmentRepository
                        .existsByExercise_IdAndTeam_Id(
                                exerciseId,
                                team.getId()
                        ))
                .toList();

        if (teamsToAssess.isEmpty()) {
            return List.of();
        }

        if (exercise.getScenario() == null) {
            throw new BusinessException(
                    "Exercise does not have an assigned scenario"
            );
        }

        List<DefensiveTask> defensiveTasks =
                defensiveTaskRepository
                        .findByScenario_IdOrderBySequenceOrderAsc(
                                exercise.getScenario().getId()
                        );

        if (defensiveTasks.isEmpty()) {
            throw new BusinessException(
                    "Cannot initialize assessments: "
                            + "the scenario has no defensive tasks"
            );
        }

        Map<Long, List<AssessmentCriterion>> criteriaByTask =
                new LinkedHashMap<>();

        for (DefensiveTask task : defensiveTasks) {

            List<AssessmentCriterion> criteria =
                    criterionRepository
                            .findByDefensiveTask_IdOrderByIdAsc(
                                    task.getId()
                            );

            if (criteria.isEmpty()) {
                throw new BusinessException(
                        "Defensive task has no assessment criteria. Task ID: "
                                + task.getId()
                );
            }

            criteriaByTask.put(task.getId(), criteria);
        }

        List<AssessmentResponse> responses = new ArrayList<>();

        for (Team team : teamsToAssess) {

            Assessment assessment = assessmentRepository.save(
                    new Assessment(exercise, team)
            );

            for (DefensiveTask task : defensiveTasks) {

                AssessmentResult result = resultRepository.save(
                        new AssessmentResult(assessment, task)
                );

                List<AssessmentScore> scores = new ArrayList<>();

                for (AssessmentCriterion criterion :
                        criteriaByTask.get(task.getId())) {

                    scores.add(
                            new AssessmentScore(
                                    result,
                                    criterion,
                                    BigDecimal.ZERO,
                                    criterion.getMaxScore(),
                                    null
                            )
                    );
                }

                scoreRepository.saveAll(scores);
            }

            responses.add(toAssessmentResponse(assessment));
        }

        return responses;
    }

    /**
     * Get all assessments belonging to an exercise.
     *
     * ADMIN and INSTRUCTOR can view all.
     * STUDENT can view only if assigned as WHITE.
     */
    public List<AssessmentResponse> getAssessments(
            Long exerciseId,
            String currentUserId,
            String systemRole
    ) {

        validateId(exerciseId, "Exercise ID");

        if (!exerciseRepository.existsById(exerciseId)) {
            throw new ResourceNotFoundException(
                    "Exercise not found with ID: " + exerciseId
            );
        }

        authorizeExerciseAccess(
                exerciseId,
                currentUserId,
                systemRole
        );

        return assessmentRepository
                .findByExercise_IdOrderByTeam_IdAsc(exerciseId)
                .stream()
                .map(this::toAssessmentResponse)
                .toList();
    }

    /**
     * Get assessments belonging to exercises
     * where the current STUDENT is assigned as WHITE.
     */
    public List<AssessmentResponse> getMyAssessments(
            String currentUserId,
            String systemRole
    ) {

        if (!"STUDENT".equalsIgnoreCase(
                systemRole == null ? "" : systemRole.trim()
        )) {
            throw new AccessDeniedException("Access denied");
        }

        if (currentUserId == null || currentUserId.isBlank()) {
            throw new AccessDeniedException("Access denied");
        }

        Long userId;

        try {
            userId = Long.parseLong(currentUserId);
        } catch (NumberFormatException exception) {
            throw new AccessDeniedException("Access denied");
        }

        List<Long> exerciseIds = participantRepository
                .findByUser_IdAndRoleOrderByJoinedAtDesc(
                        userId,
                        "WHITE"
                )
                .stream()
                .map(participant -> participant.getExercise().getId())
                .distinct()
                .toList();

        if (exerciseIds.isEmpty()) {
            return List.of();
        }

        return assessmentRepository
                .findByExercise_IdInOrderByCreatedAtDesc(exerciseIds)
                .stream()
                .map(this::toAssessmentResponse)
                .toList();
    }

    /**
     * Get assessment details.
     */
    public AssessmentResponse getAssessment(
            Long assessmentId,
            String currentUserId,
            String systemRole
    ) {

        validateId(assessmentId, "Assessment ID");

        Assessment assessment = findAssessment(assessmentId);

        authorizeExerciseAccess(
                assessment.getExercise().getId(),
                currentUserId,
                systemRole
        );

        return toAssessmentResponse(assessment);
    }

    /**
     * Get defensive task results and detailed scores.
     */
    public List<AssessmentResultResponse> getAssessmentResults(
            Long assessmentId,
            String currentUserId,
            String systemRole
    ) {

        validateId(assessmentId, "Assessment ID");

        Assessment assessment = findAssessment(assessmentId);

        authorizeExerciseAccess(
                assessment.getExercise().getId(),
                currentUserId,
                systemRole
        );

        return resultRepository
                .findByAssessment_IdOrderByDefensiveTask_SequenceOrderAsc(
                        assessmentId
                )
                .stream()
                .map(this::toAssessmentResultResponse)
                .toList();
    }

    /**
     * Delete an assessment and its dependent results and scores.
     *
     * Controller must restrict this operation to ADMIN only.
     */
    @Transactional
    public void deleteAssessment(Long assessmentId) {

        validateId(assessmentId, "Assessment ID");

        Assessment assessment = findAssessment(assessmentId);

        assessmentRepository.delete(assessment);
    }

    /**
     * Start a defensive task assessment.
     *
     * Only ADMIN and INSTRUCTOR are allowed to perform this operation.
     */
    @Transactional
    public AssessmentResultResponse startTask(Long resultId) {

        validateId(resultId, "Assessment Result ID");

        AssessmentResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assessment result not found with ID: " + resultId
                ));

        // Only PENDING tasks can be started.
        // The entity method records startedAt using server time.
        result.start();

        AssessmentResult savedResult = resultRepository.save(result);

        return toAssessmentResultResponse(savedResult);
    }

    /**
     * Record detection of a defensive task.
     *
     * Only ADMIN and INSTRUCTOR are allowed to perform this operation.
     * Detection timestamp is generated by the backend.
     */
    @Transactional
    public AssessmentResultResponse recordDetection(Long resultId) {

        validateId(resultId, "Assessment Result ID");

        AssessmentResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assessment result not found with ID: " + resultId
                ));

        // Record detection using server time.
        result.recordDetection(java.time.Instant.now());

        AssessmentResult savedResult = resultRepository.save(result);

        return toAssessmentResultResponse(savedResult);
    }

    /**
     * Record response to a detected incident.
     *
     * Only ADMIN and INSTRUCTOR are allowed to perform this operation.
     * Response timestamp is generated by the backend.
     */
    @Transactional
    public AssessmentResultResponse recordResponse(Long resultId) {

        validateId(resultId, "Assessment Result ID");

        AssessmentResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assessment result not found with ID: " + resultId
                ));

        // Record response using server time.
        result.recordResponse(java.time.Instant.now());

        AssessmentResult savedResult = resultRepository.save(result);

        return toAssessmentResultResponse(savedResult);
    }

    /**
     * Complete a defensive task.
     *
     * Detection is required, but Response is optional.
     * Completion timestamp is generated by the backend.
     */
    @Transactional
    public AssessmentResultResponse completeTask(
            Long resultId,
            String reason
    ) {

        validateId(resultId, "Assessment Result ID");

        AssessmentResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assessment result not found with ID: " + resultId
                ));

        // Complete using server time.
        result.complete(java.time.Instant.now(), reason);

        AssessmentResult savedResult = resultRepository.save(result);

        return toAssessmentResultResponse(savedResult);
    }

    /**
     * Convert Assessment entity to response DTO.
     */
    private AssessmentResponse toAssessmentResponse(
            Assessment assessment
    ) {

        return new AssessmentResponse(
                assessment.getId(),
                assessment.getExercise().getId(),
                assessment.getTeam().getId(),
                assessment.getTeam().getName(),
                assessment.getStatus(),
                assessment.getTotalScore(),
                assessment.getStartedAt(),
                assessment.getCompletedAt(),
                assessment.getReviewStatus(),
                assessment.getReviewedBy() == null
                        ? null
                        : assessment.getReviewedBy().getId(),
                assessment.getReviewedAt(),
                assessment.getReviewComment(),
                assessment.getCreatedAt()
        );
    }

    /**
     * Convert AssessmentResult entity to response DTO.
     */
    private AssessmentResultResponse toAssessmentResultResponse(
            AssessmentResult result
    ) {

        List<AssessmentScoreResponse> scores =
                scoreRepository
                        .findByAssessmentResult_IdOrderByIdAsc(
                                result.getId()
                        )
                        .stream()
                        .map(this::toAssessmentScoreResponse)
                        .toList();

        BigDecimal awardedScore = scores.stream()
                .map(AssessmentScoreResponse::awardedScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal maxScore = scores.stream()
                .map(AssessmentScoreResponse::maxScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new AssessmentResultResponse(
                result.getId(),
                result.getAssessment().getId(),
                result.getDefensiveTask().getId(),
                result.getDefensiveTask().getName(),
                result.getDefensiveTask().getSequenceOrder(),
                result.getStatus(),
                result.getStartedAt(),
                result.getDetectedAt(),
                result.getRespondedAt(),
                result.getCompletedAt(),
                durationToSeconds(result.getDetectionTime()),
                durationToSeconds(result.getResponseTime()),
                result.getReason(),
                awardedScore,
                maxScore,
                scores
        );
    }

    /**
     * Convert AssessmentScore entity to response DTO.
     */
    private AssessmentScoreResponse toAssessmentScoreResponse(
            AssessmentScore score
    ) {

        AssessmentCriterion criterion = score.getCriterion();

        return new AssessmentScoreResponse(
                score.getId(),
                criterion.getId(),
                criterion.getName(),
                criterion.getCriterionType(),
                score.getAwardedScore(),
                score.getMaxScore(),
                score.getReason()
        );
    }

    /**
     * Convert Duration to seconds.
     */
    private Long durationToSeconds(Duration duration) {

        return duration == null
                ? null
                : duration.getSeconds();
    }

    /**
     * Authorize access to an exercise.
     *
     * ADMIN and INSTRUCTOR can access all exercises.
     * STUDENT must be assigned as WHITE.
     */
    private void authorizeExerciseAccess(
            Long exerciseId,
            String currentUserId,
            String systemRole
    ) {

        String normalizedRole = systemRole == null
                ? ""
                : systemRole.trim().toUpperCase(Locale.ROOT);

        if ("ADMIN".equals(normalizedRole)
                || "INSTRUCTOR".equals(normalizedRole)) {
            return;
        }

        if (!"STUDENT".equals(normalizedRole)
                || currentUserId == null
                || currentUserId.isBlank()) {
            throw new AccessDeniedException("Access denied");
        }

        Long userId;

        try {
            userId = Long.parseLong(currentUserId);
        } catch (NumberFormatException exception) {
            throw new AccessDeniedException("Access denied");
        }

        boolean isWhiteParticipant =
                participantRepository
                        .existsByExercise_IdAndUser_IdAndRole(
                                exerciseId,
                                userId,
                                "WHITE"
                        );

        if (!isWhiteParticipant) {
            throw new AccessDeniedException("Access denied");
        }
    }

    /**
     * Find assessment or throw 404.
     */
    private Assessment findAssessment(Long assessmentId) {

        return assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assessment not found with ID: " + assessmentId
                ));
    }

    /**
     * Validate positive database IDs.
     */
    private void validateId(Long id, String fieldName) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be a positive number"
            );
        }
    }
}
