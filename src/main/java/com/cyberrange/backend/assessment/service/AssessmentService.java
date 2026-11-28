package com.cyberrange.backend.assessment.service;

import com.cyberrange.backend.scoring.engine.ScoringEngine;

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
import java.util.Comparator;
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
    public List<AssessmentResponse> initializeAssessments(Long exerciseId) {
        return initializeAssessments(exerciseId, null);
    }

    @Transactional
    public List<AssessmentResponse> initializeAssessments(Long exerciseId, Long teamId) {
        validateId(exerciseId, "Exercise ID");
        if (teamId != null) {
            validateId(teamId, "Team ID");
        }

        Exercise exercise = exerciseRepository.findByIdForUpdate(exerciseId)
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

        Assessment existingSelectedAssessment = null;
        if (teamId != null) {
            Team selectedTeam = blueTeams.get(teamId);
            if (selectedTeam == null) {
                throw new BusinessException("Team is not a BLUE participant in this Exercise");
            }
            existingSelectedAssessment = assessmentRepository.findByExercise_IdAndTeam_Id(exerciseId, teamId).orElse(null);
            if (existingSelectedAssessment == null && !List.of("CREATED", "READY").contains(exercise.getStatus())) {
                throw new BusinessException("Exercise must be CREATED or READY to initialize a Team Assessment");
            }

            blueTeams.clear();
            blueTeams.put(teamId, selectedTeam);
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

        if (teamsToAssess.isEmpty() && existingSelectedAssessment == null) {
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

            if (teamId != null && !describeCriteria(task, criteria).configured()) {
                throw new BusinessException("Assessment criteria or scoring rule is invalid. Task ID: " + task.getId());
            }

            criteriaByTask.put(task.getId(), criteria);
        }

        if (existingSelectedAssessment != null) {
            validateExistingResults(existingSelectedAssessment, criteriaByTask);
            return List.of(toAssessmentResponse(existingSelectedAssessment));
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

    private void validateExistingResults(Assessment assessment, Map<Long, List<AssessmentCriterion>> criteriaByTask) {
        var results = resultRepository.findByAssessment_IdOrderByDefensiveTask_SequenceOrderAsc(assessment.getId());
        if (results.size() != criteriaByTask.size()) {
            throw new BusinessException("Existing Assessment task results do not match the current Scenario");
        }
        for (AssessmentResult result : results) {
            var expected = criteriaByTask.get(result.getDefensiveTask().getId());
            var recorded = scoreRepository.findByAssessmentResult_IdOrderByIdAsc(result.getId());
            if (expected == null || expected.size() != recorded.size() || recorded.stream()
                    .anyMatch(score -> expected.stream().noneMatch(criterion ->
                            criterion.getId().equals(score.getCriterion().getId())
                                    && criterion.getMaxScore().compareTo(score.getMaxScore()) == 0))) {
                throw new BusinessException("Existing Assessment Scores do not match the current Criteria");
            }
        }
    }

    public record TaskCriteriaStatus(Long taskId, int count, BigDecimal maximum, boolean configured) {}

    public List<TaskCriteriaStatus> getTaskCriteriaStatus(Long exerciseId) {
        validateId(exerciseId, "Exercise ID");
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise not found with ID: " + exerciseId));
        return defensiveTaskRepository.findByScenario_IdOrderBySequenceOrderAsc(exercise.getScenario().getId())
                .stream()
                .map(task -> describeCriteria(task, criterionRepository.findByDefensiveTask_IdOrderByIdAsc(task.getId())))
                .toList();
    }

    private TaskCriteriaStatus describeCriteria(DefensiveTask task, List<AssessmentCriterion> criteria) {
        BigDecimal maximum = criteria.stream()
                .map(AssessmentCriterion::getMaxScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean configured = !criteria.isEmpty() && maximum.compareTo(task.getMaxScore()) == 0
                && criteria.stream().allMatch(this::isConfiguredCriterion);
        return new TaskCriteriaStatus(task.getId(), criteria.size(), maximum, configured);
    }

    private boolean isConfiguredCriterion(AssessmentCriterion criterion) {
        var rule = criterion.getScoringRule();
        if ("MANUAL".equalsIgnoreCase(criterion.getCriterionType())) {
            return rule == null || rule.isEmpty();
        }
        if (rule == null || rule.isEmpty()) {
            return false;
        }
        try {
            ScoringEngine.validateRule(rule);
            return ScoringEngine.toBigDecimal(rule.get("score")).compareTo(criterion.getMaxScore()) <= 0;
        } catch (BusinessException exception) {
            return false;
        }
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

        Map<Long, Assessment> visible = new LinkedHashMap<>();
        if (!exerciseIds.isEmpty()) {
            assessmentRepository.findByExercise_IdInOrderByCreatedAtDesc(exerciseIds)
                    .forEach(assessment -> visible.put(assessment.getId(), assessment));
        }
        participantRepository.findByUser_IdAndRoleOrderByJoinedAtDesc(userId, "BLUE").stream()
                .filter(member -> member.getTeam() != null)
                .flatMap(member -> assessmentRepository.findByExercise_IdAndTeam_Id(
                        member.getExercise().getId(), member.getTeam().getId()).stream())
                .filter(BlueSshAccess::isSsh)
                .forEach(assessment -> visible.put(assessment.getId(), assessment));
        return visible.values().stream()
                .sorted(Comparator.comparing(Assessment::getCreatedAt).reversed())
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

        authorizeAssessmentAccess(assessment, currentUserId, systemRole);

        return toAssessmentResponse(assessment);
    }

    public void authorizeScoreAccess(Long scoreId, String userId, String systemRole) {
        validateId(scoreId, "Assessment Score ID");
        Assessment assessment = scoreRepository.findById(scoreId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment Score not found"))
                .getAssessmentResult().getAssessment();
        authorizeAssessmentAccess(assessment, userId, systemRole);
    }

    private void authorizeAssessmentAccess(Assessment assessment, String userId, String systemRole) {
        if (BlueSshAccess.isStudent(systemRole) && BlueSshAccess.isSsh(assessment) && userId != null) {
            Long studentId;
            try {
                studentId = Long.valueOf(userId);
            } catch (NumberFormatException exception) {
                throw new AccessDeniedException("Access denied");
            }
            boolean ownTeam = participantRepository.findByExercise_IdAndUser_Id(assessment.getExercise().getId(), studentId)
                    .filter(member -> "BLUE".equals(member.getRole()) && member.getTeam() != null)
                    .filter(member -> member.getTeam().getId().equals(assessment.getTeam().getId()))
                    .isPresent();
            if (ownTeam) {
                return;
            }
        }
        authorizeExerciseAccess(assessment.getExercise().getId(), userId, systemRole);
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

        authorizeAssessmentAccess(assessment, currentUserId, systemRole);

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

        requireManualTaskWorkflow(result);
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

        requireManualTaskWorkflow(result);
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

        requireManualTaskWorkflow(result);
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

        requireManualTaskWorkflow(result);
        // Complete using server time.
        result.complete(java.time.Instant.now(), reason);

        AssessmentResult savedResult = resultRepository.save(result);

        return toAssessmentResultResponse(savedResult);
    }

    private void requireManualTaskWorkflow(AssessmentResult result) {
        if (usesSshWorkflow(result.getAssessment())) {
            throw new BusinessException("SSH task status and timestamps are recorded from verified workflow evidence");
        }
    }

    private boolean usesSshWorkflow(Assessment assessment) {
        return SshAssessmentEvaluator.SCENARIO_NAME.equals(assessment.getExercise().getScenario().getName());
    }

    /**
     * Convert Assessment entity to response DTO.
     */
    private AssessmentResponse toAssessmentResponse(
            Assessment assessment
    ) {

        boolean sshWorkflow = usesSshWorkflow(assessment);
        BigDecimal finalScore = null;
        if (sshWorkflow && "REVIEWED".equals(assessment.getReviewStatus())) {
            BigDecimal maximum = SshAssessmentEvaluator.applicableMaximum(
                    resultRepository.findByAssessment_IdOrderByDefensiveTask_SequenceOrderAsc(assessment.getId()));
            finalScore = SshAssessmentEvaluator.normalizedScore(assessment.getTotalScore(), maximum);
        }
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
                assessment.getCreatedAt(),
                sshWorkflow,
                finalScore
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
                score.getReason(),
                score.getScoreSource()
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
