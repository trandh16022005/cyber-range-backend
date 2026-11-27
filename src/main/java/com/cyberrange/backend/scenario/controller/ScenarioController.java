package com.cyberrange.backend.scenario.controller;

import com.cyberrange.backend.common.response.ApiResponse;
import com.cyberrange.backend.common.response.PageResponse;
import com.cyberrange.backend.scenario.dto.CreateScenarioRequest;
import com.cyberrange.backend.scenario.dto.DefensiveTaskResponse;
import com.cyberrange.backend.scenario.dto.ScenarioResponse;
import com.cyberrange.backend.scenario.dto.UpdateScenarioRequest;
import com.cyberrange.backend.scenario.service.ScenarioService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/scenarios")
@PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
public class ScenarioController {

    private final ScenarioService scenarioService;

    public ScenarioController(ScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    // 1. Get scenarios with pagination and filtering
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ScenarioResponse>>> getScenarios(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type
    ) {
        PageResponse<ScenarioResponse> scenarios =
                scenarioService.getScenarios(page, size, status, type);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Scenarios retrieved successfully",
                        scenarios
                )
        );
    }

    // 2. Get scenario details
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ScenarioResponse>> getScenarioById(
            @PathVariable Long id
    ) {
        ScenarioResponse scenario = scenarioService.getScenarioById(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Scenario retrieved successfully",
                        scenario
                )
        );
    }

    // 3. Get defensive tasks of a scenario
    @GetMapping("/{id}/defensive-tasks")
    public ResponseEntity<ApiResponse<List<DefensiveTaskResponse>>> getDefensiveTasks(
            @PathVariable Long id
    ) {
        List<DefensiveTaskResponse> tasks =
                scenarioService.getDefensiveTasks(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Defensive tasks retrieved successfully",
                        tasks
                )
        );
    }

    // 4. Create scenario
    @PostMapping
    public ResponseEntity<ApiResponse<ScenarioResponse>> createScenario(
            @RequestBody CreateScenarioRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ScenarioResponse scenario = scenarioService.createScenario(
                request,
                jwt.getSubject()
        );

        return ResponseEntity.status(201).body(
                ApiResponse.success(
                        "Scenario created successfully",
                        scenario
                )
        );
    }

    // 5. Update scenario
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ScenarioResponse>> updateScenario(
            @PathVariable Long id,
            @RequestBody UpdateScenarioRequest request
    ) {
        ScenarioResponse scenario =
                scenarioService.updateScenario(id, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Scenario updated successfully",
                        scenario
                )
        );
    }

    // 6. Publish scenario
    @PatchMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<ScenarioResponse>> publishScenario(
            @PathVariable Long id
    ) {
        ScenarioResponse scenario = scenarioService.publishScenario(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Scenario published successfully",
                        scenario
                )
        );
    }

    // 7. Archive scenario
    @PatchMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<ScenarioResponse>> archiveScenario(
            @PathVariable Long id
    ) {
        ScenarioResponse scenario = scenarioService.archiveScenario(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Scenario archived successfully",
                        scenario
                )
        );
    }

    // 8. Delete scenario (ADMIN only)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteScenario(
            @PathVariable Long id
    ) {
        scenarioService.deleteScenario(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Scenario deleted successfully",
                        null
                )
        );
    }
}
