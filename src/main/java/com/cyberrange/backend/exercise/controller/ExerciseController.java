package com.cyberrange.backend.exercise.controller;

import com.cyberrange.backend.common.response.ApiResponse;
import com.cyberrange.backend.common.response.PageResponse;
import com.cyberrange.backend.exercise.dto.CreateExerciseRequest;
import com.cyberrange.backend.exercise.dto.ExerciseResponse;
import com.cyberrange.backend.exercise.service.ExerciseService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/exercises")
@PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
public class ExerciseController {

    private final ExerciseService exerciseService;

    public ExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    // 1. Get exercises with pagination and filtering
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ExerciseResponse>>> getExercises(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long scenarioId,
            @RequestParam(required = false) Long cyberRangeId
    ) {
        PageResponse<ExerciseResponse> exercises =
                exerciseService.getExercises(
                        page,
                        size,
                        status,
                        scenarioId,
                        cyberRangeId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Exercises retrieved successfully",
                        exercises
                )
        );
    }

    // 2. Get exercise details
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExerciseResponse>> getExerciseById(
            @PathVariable Long id
    ) {
        ExerciseResponse exercise =
                exerciseService.getExerciseById(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Exercise retrieved successfully",
                        exercise
                )
        );
    }

    // 3. Create exercise
    @PostMapping
    public ResponseEntity<ApiResponse<ExerciseResponse>> createExercise(
            @RequestBody CreateExerciseRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Exercise request must not be null"
            );
        }

        ExerciseResponse exercise = exerciseService.createExercise(
                request.scenarioId(),
                request.cyberRangeId(),
                request.name(),
                jwt.getSubject()
        );

        return ResponseEntity.status(201).body(
                ApiResponse.success(
                        "Exercise created successfully",
                        exercise
                )
        );
    }
}

