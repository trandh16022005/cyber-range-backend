package com.cyberrange.backend.exercise.service;

import com.cyberrange.backend.common.exception.BusinessException;
import com.cyberrange.backend.common.exception.ResourceNotFoundException;
import com.cyberrange.backend.common.response.PageResponse;
import com.cyberrange.backend.exercise.dto.ExerciseResponse;
import com.cyberrange.backend.exercise.entity.Exercise;
import com.cyberrange.backend.exercise.repository.ExerciseRepository;
import com.cyberrange.backend.lab.entity.CyberRange;
import com.cyberrange.backend.lab.repository.CyberRangeRepository;
import com.cyberrange.backend.scenario.entity.Scenario;
import com.cyberrange.backend.scenario.repository.ScenarioRepository;
import com.cyberrange.backend.user.entity.User;
import com.cyberrange.backend.user.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final ScenarioRepository scenarioRepository;
    private final CyberRangeRepository cyberRangeRepository;
    private final UserRepository userRepository;

    public ExerciseService(
            ExerciseRepository exerciseRepository,
            ScenarioRepository scenarioRepository,
            CyberRangeRepository cyberRangeRepository,
            UserRepository userRepository
    ) {
        this.exerciseRepository = exerciseRepository;
        this.scenarioRepository = scenarioRepository;
        this.cyberRangeRepository = cyberRangeRepository;
        this.userRepository = userRepository;
    }

    // 1. Get exercises with pagination and optional filters
    public PageResponse<ExerciseResponse> getExercises(
            int page,
            int size,
            String status,
            Long scenarioId,
            Long cyberRangeId
    ) {
        validatePagination(page, size);

        String normalizedStatus = normalizeOptional(status);

        if (normalizedStatus != null) {
            normalizedStatus = normalizedStatus.toUpperCase();

            if (!isValidStatus(normalizedStatus)) {
                throw new IllegalArgumentException(
                        "Invalid exercise status"
                );
            }
        }

        validateOptionalId(scenarioId, "Scenario ID");
        validateOptionalId(cyberRangeId, "Cyber Range ID");

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Exercise> exercises;

        if (normalizedStatus != null
                && scenarioId != null
                && cyberRangeId != null) {
            exercises = exerciseRepository
                    .findByStatusAndScenarioIdAndCyberRangeId(
                            normalizedStatus,
                            scenarioId,
                            cyberRangeId,
                            pageable
                    );
        } else if (normalizedStatus != null && scenarioId != null) {
            exercises = exerciseRepository
                    .findByStatusAndScenarioId(
                            normalizedStatus,
                            scenarioId,
                            pageable
                    );
        } else if (normalizedStatus != null && cyberRangeId != null) {
            exercises = exerciseRepository
                    .findByStatusAndCyberRangeId(
                            normalizedStatus,
                            cyberRangeId,
                            pageable
                    );
        } else if (scenarioId != null && cyberRangeId != null) {
            exercises = exerciseRepository
                    .findByScenarioIdAndCyberRangeId(
                            scenarioId,
                            cyberRangeId,
                            pageable
                    );
        } else if (normalizedStatus != null) {
            exercises = exerciseRepository.findByStatus(
                    normalizedStatus,
                    pageable
            );
        } else if (scenarioId != null) {
            exercises = exerciseRepository.findByScenarioId(
                    scenarioId,
                    pageable
            );
        } else if (cyberRangeId != null) {
            exercises = exerciseRepository.findByCyberRangeId(
                    cyberRangeId,
                    pageable
            );
        } else {
            exercises = exerciseRepository.findAll(pageable);
        }

        Page<ExerciseResponse> responsePage =
                exercises.map(ExerciseResponse::fromEntity);

        return PageResponse.from(responsePage);
    }

    // 2. Get exercise details
    public ExerciseResponse getExerciseById(Long id) {
        Exercise exercise = findExerciseById(id);

        return ExerciseResponse.fromEntity(exercise);
    }

    // 3. Create exercise
    @Transactional
    public ExerciseResponse createExercise(
            Long scenarioId,
            Long cyberRangeId,
            String name,
            String userIdFromJwt
    ) {
        validateRequiredId(scenarioId, "Scenario ID");
        validateRequiredId(cyberRangeId, "Cyber Range ID");

        String normalizedName = validateName(name);

        Long userId;

        try {
            userId = Long.valueOf(userIdFromJwt);
        } catch (NumberFormatException | NullPointerException exception) {
            throw new IllegalArgumentException(
                    "Invalid authenticated user ID"
            );
        }

        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Scenario not found")
                );

        if (!"PUBLISHED".equals(scenario.getStatus())) {
            throw new BusinessException(
                    "Only PUBLISHED scenarios can be used to create exercises"
            );
        }

        CyberRange cyberRange = cyberRangeRepository.findById(cyberRangeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Cyber Range not found")
                );

        if (!"ACTIVE".equals(cyberRange.getStatus())) {
            throw new BusinessException(
                    "Only ACTIVE Cyber Ranges can be used to create exercises"
            );
        }

        User creator = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        Exercise exercise = new Exercise(
                scenario,
                cyberRange,
                normalizedName,
                creator
        );

        Exercise savedExercise = exerciseRepository.save(exercise);

        return ExerciseResponse.fromEntity(savedExercise);
    }

    // Find exercise or throw 404
    private Exercise findExerciseById(Long id) {
        validateRequiredId(id, "Exercise ID");

        return exerciseRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Exercise not found")
                );
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page must not be negative"
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100"
            );
        }
    }

    private void validateRequiredId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Invalid " + fieldName
            );
        }
    }

    private void validateOptionalId(Long id, String fieldName) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException(
                    "Invalid " + fieldName
            );
        }
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Exercise name is required"
            );
        }

        String normalized = name.trim();

        if (normalized.length() > 150) {
            throw new IllegalArgumentException(
                    "Exercise name must not exceed 150 characters"
            );
        }

        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty() ? null : normalized;
    }

    private boolean isValidStatus(String status) {
        return switch (status) {
            case "CREATED", "READY", "RUNNING", "PAUSED",
                 "STOPPED", "COMPLETED", "CANCELLED" -> true;
            default -> false;
        };
    }
}
