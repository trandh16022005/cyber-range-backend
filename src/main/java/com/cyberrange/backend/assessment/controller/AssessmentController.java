package com.cyberrange.backend.assessment.controller;

import com.cyberrange.backend.assessment.dto.AssessmentResponse;
import com.cyberrange.backend.assessment.dto.AssessmentResultResponse;
import com.cyberrange.backend.assessment.service.AssessmentService;
import com.cyberrange.backend.common.response.ApiResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AssessmentController {

    private final AssessmentService assessmentService;

    public AssessmentController(
            AssessmentService assessmentService
    ) {
        this.assessmentService = assessmentService;
    }

    /**
     * Initialize assessments for all eligible BLUE teams.
     *
     * Only ADMIN and INSTRUCTOR are allowed.
     */
    @PostMapping(
            "/api/v1/exercises/{exerciseId}/assessments/initialize"
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<List<AssessmentResponse>>>
    initializeAssessments(
            @PathVariable Long exerciseId
    ) {

        List<AssessmentResponse> assessments =
                assessmentService.initializeAssessments(exerciseId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Assessments initialized successfully",
                        assessments
                )
        );
    }

    /**
     * Retrieve assessments belonging to an exercise.
     *
     * ADMIN and INSTRUCTOR can view all.
     * STUDENT must be assigned as WHITE.
     */
    @GetMapping(
            "/api/v1/exercises/{exerciseId}/assessments"
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<AssessmentResponse>>>
    getAssessments(
            @PathVariable Long exerciseId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        List<AssessmentResponse> assessments =
                assessmentService.getAssessments(
                        exerciseId,
                        jwt.getSubject(),
                        jwt.getClaimAsString("role")
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Assessments retrieved successfully",
                        assessments
                )
        );
    }

    /**
     * Retrieve assessment details.
     *
     * STUDENT must be assigned as WHITE
     * in the assessment's exercise.
     */
    @GetMapping("/api/v1/assessments/{assessmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR', 'STUDENT')")
    public ResponseEntity<ApiResponse<AssessmentResponse>>
    getAssessment(
            @PathVariable Long assessmentId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        AssessmentResponse assessment =
                assessmentService.getAssessment(
                        assessmentId,
                        jwt.getSubject(),
                        jwt.getClaimAsString("role")
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Assessment retrieved successfully",
                        assessment
                )
        );
    }

    /**
     * Retrieve assessment results and detailed scores.
     *
     * STUDENT must be assigned as WHITE
     * in the assessment's exercise.
     */
    @GetMapping(
            "/api/v1/assessments/{assessmentId}/results"
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<AssessmentResultResponse>>>
    getAssessmentResults(
            @PathVariable Long assessmentId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        List<AssessmentResultResponse> results =
                assessmentService.getAssessmentResults(
                        assessmentId,
                        jwt.getSubject(),
                        jwt.getClaimAsString("role")
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Assessment results retrieved successfully",
                        results
                )
        );
    }

    /**
     * Delete an assessment.
     *
     * Only ADMIN is allowed.
     */
    @DeleteMapping("/api/v1/assessments/{assessmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAssessment(
            @PathVariable Long assessmentId
    ) {

        assessmentService.deleteAssessment(assessmentId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Assessment deleted successfully"
                )
        );
    }
}
