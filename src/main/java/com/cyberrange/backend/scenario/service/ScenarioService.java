package com.cyberrange.backend.scenario.service;

import com.cyberrange.backend.common.exception.BusinessException;
import com.cyberrange.backend.common.exception.ResourceNotFoundException;
import com.cyberrange.backend.common.response.PageResponse;
import com.cyberrange.backend.scenario.dto.CreateScenarioRequest;
import com.cyberrange.backend.scenario.dto.DefensiveTaskResponse;
import com.cyberrange.backend.scenario.dto.ScenarioResponse;
import com.cyberrange.backend.scenario.dto.UpdateScenarioRequest;
import com.cyberrange.backend.scenario.entity.DefensiveTask;
import com.cyberrange.backend.scenario.entity.Scenario;
import com.cyberrange.backend.scenario.repository.DefensiveTaskRepository;
import com.cyberrange.backend.scenario.repository.ScenarioRepository;
import com.cyberrange.backend.user.entity.User;
import com.cyberrange.backend.user.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ScenarioService {

    private static final Set<String> VALID_STATUSES = Set.of(
            "DRAFT",
            "PUBLISHED",
            "ARCHIVED"
    );

    private final ScenarioRepository scenarioRepository;
    private final UserRepository userRepository;
    private final DefensiveTaskRepository defensiveTaskRepository;

    public ScenarioService(
            ScenarioRepository scenarioRepository,
            UserRepository userRepository,
            DefensiveTaskRepository defensiveTaskRepository
    ) {
        this.scenarioRepository = scenarioRepository;
        this.userRepository = userRepository;
        this.defensiveTaskRepository = defensiveTaskRepository;
    }

    // 1. Get scenarios with pagination and filtering
    public PageResponse<ScenarioResponse> getScenarios(
            int page,
            int size,
            String status,
            String type
    ) {
        validatePagination(page, size);

        String normalizedStatus = normalizeStatus(status);
        String normalizedType = normalizeOptional(type);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Scenario> scenarios;

        if (normalizedStatus != null && normalizedType != null) {
            scenarios = scenarioRepository.findByStatusAndType(
                    normalizedStatus,
                    normalizedType,
                    pageable
            );
        } else if (normalizedStatus != null) {
            scenarios = scenarioRepository.findByStatus(
                    normalizedStatus,
                    pageable
            );
        } else if (normalizedType != null) {
            scenarios = scenarioRepository.findByType(
                    normalizedType,
                    pageable
            );
        } else {
            scenarios = scenarioRepository.findAll(pageable);
        }

        Page<ScenarioResponse> responsePage =
                scenarios.map(ScenarioResponse::fromEntity);

        return PageResponse.from(responsePage);
    }

    // 2. Get scenario details
    public ScenarioResponse getScenarioById(Long id) {
        Scenario scenario = findScenarioById(id);

        return ScenarioResponse.fromEntity(scenario);
    }

    // 3. Get defensive tasks of a scenario
    public List<DefensiveTaskResponse> getDefensiveTasks(
            Long scenarioId
    ) {
        findScenarioById(scenarioId);

        List<DefensiveTask> tasks =
                defensiveTaskRepository
                        .findByScenario_IdOrderBySequenceOrderAsc(
                                scenarioId
                        );

        return tasks.stream()
                .map(DefensiveTaskResponse::fromEntity)
                .toList();
    }

    // 4. Create scenario
    @Transactional
    public ScenarioResponse createScenario(
            CreateScenarioRequest request,
            String userIdFromJwt
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Scenario request must not be null"
            );
        }

        Long userId;

        try {
            userId = Long.valueOf(userIdFromJwt);
        } catch (NumberFormatException | NullPointerException exception) {
            throw new IllegalArgumentException(
                    "Invalid authenticated user ID"
            );
        }

        User creator = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        String name = validateName(request.name());
        String type = validateType(request.type());
        String description = normalizeOptional(request.description());
        String difficulty = validateDifficulty(request.difficulty());

        if (scenarioRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException(
                    "Scenario name already exists"
            );
        }

        Scenario scenario = new Scenario(
                name,
                description,
                type,
                difficulty,
                creator
        );

        Scenario savedScenario = scenarioRepository.save(scenario);

        return ScenarioResponse.fromEntity(savedScenario);
    }

    // 5. Update scenario (DRAFT only)
    @Transactional
    public ScenarioResponse updateScenario(
            Long id,
            UpdateScenarioRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Update request must not be null"
            );
        }

        Scenario scenario = findScenarioById(id);

        if (!"DRAFT".equals(scenario.getStatus())) {
            throw new BusinessException(
                    "Only DRAFT scenarios can be updated"
            );
        }

        if (request.name() == null
                && request.description() == null
                && request.type() == null
                && request.difficulty() == null) {
            throw new IllegalArgumentException(
                    "At least one field must be provided"
            );
        }

        String name = scenario.getName();
        String description = scenario.getDescription();
        String type = scenario.getType();
        String difficulty = scenario.getDifficulty();

        if (request.name() != null) {
            name = validateName(request.name());

            if (scenarioRepository.existsByNameIgnoreCaseAndIdNot(
                    name,
                    id
            )) {
                throw new BusinessException(
                        "Scenario name already exists"
                );
            }
        }

        if (request.description() != null) {
            description = normalizeOptional(request.description());
        }

        if (request.type() != null) {
            type = validateType(request.type());
        }

        if (request.difficulty() != null) {
            difficulty = validateDifficulty(request.difficulty());
        }

        scenario.update(
                name,
                description,
                type,
                difficulty
        );

        return ScenarioResponse.fromEntity(scenario);
    }

    // 6. Publish scenario (DRAFT -> PUBLISHED)
    @Transactional
    public ScenarioResponse publishScenario(Long id) {
        Scenario scenario = findScenarioById(id);

        if (!"DRAFT".equals(scenario.getStatus())) {
            throw new BusinessException(
                    "Only DRAFT scenarios can be published"
            );
        }

        scenario.updateStatus("PUBLISHED");

        return ScenarioResponse.fromEntity(scenario);
    }

    // 7. Archive scenario (DRAFT/PUBLISHED -> ARCHIVED)
    @Transactional
    public ScenarioResponse archiveScenario(Long id) {
        Scenario scenario = findScenarioById(id);

        if (!"DRAFT".equals(scenario.getStatus())
                && !"PUBLISHED".equals(scenario.getStatus())) {
            throw new BusinessException(
                    "Only DRAFT or PUBLISHED scenarios can be archived"
            );
        }

        scenario.updateStatus("ARCHIVED");

        return ScenarioResponse.fromEntity(scenario);
    }

    // 8. Delete scenario (ADMIN only - permission handled by Controller)
    @Transactional
    public void deleteScenario(Long id) {
        Scenario scenario = findScenarioById(id);

        if (scenarioRepository.existsInExercises(id)) {
            throw new BusinessException(
                    "Cannot delete a scenario that is referenced by an exercise"
            );
        }

        scenarioRepository.delete(scenario);
    }

    // Find scenario or throw 404
    private Scenario findScenarioById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Invalid scenario ID"
            );
        }

        return scenarioRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Scenario not found"
                        )
                );
    }

    // Validate pagination
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

    // Validate scenario name
    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Scenario name is required"
            );
        }

        String normalized = name.trim();

        if (normalized.length() > 150) {
            throw new IllegalArgumentException(
                    "Scenario name must not exceed 150 characters"
            );
        }

        return normalized;
    }

    // Validate scenario type
    private String validateType(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException(
                    "Scenario type is required"
            );
        }

        String normalized = type.trim();

        if (normalized.length() > 50) {
            throw new IllegalArgumentException(
                    "Scenario type must not exceed 50 characters"
            );
        }

        return normalized;
    }

    // Validate difficulty
    private String validateDifficulty(String difficulty) {
        String normalized = normalizeOptional(difficulty);

        if (normalized != null && normalized.length() > 30) {
            throw new IllegalArgumentException(
                    "Difficulty must not exceed 30 characters"
            );
        }

        return normalized;
    }

    // Normalize optional string
    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty() ? null : normalized;
    }

    // Validate and normalize status filter
    private String normalizeStatus(String status) {
        String normalized = normalizeOptional(status);

        if (normalized == null) {
            return null;
        }

        normalized = normalized.toUpperCase();

        if (!VALID_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException(
                    "Invalid scenario status"
            );
        }

        return normalized;
    }
}