package com.cyberrange.backend.assessment.controller;

import com.cyberrange.backend.assessment.dto.AssessmentResponse;
import com.cyberrange.backend.assessment.dto.AssessmentResultResponse;
import com.cyberrange.backend.assessment.dto.AssessmentScoreEvidenceResponse;
import com.cyberrange.backend.assessment.dto.CompleteAssessmentResultRequest;
import com.cyberrange.backend.assessment.dto.LinkEvidenceRequest;
import com.cyberrange.backend.assessment.entity.AssessmentScoreEvidence;
import com.cyberrange.backend.assessment.service.AssessmentScoreEvidenceService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final AssessmentScoreEvidenceService scoreEvidenceService;

    public AssessmentController(
            AssessmentService assessmentService,
            AssessmentScoreEvidenceService scoreEvidenceService
    ) {
        this.assessmentService = assessmentService;
        this.scoreEvidenceService = scoreEvidenceService;
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
     * Start a defensive task assessment.
     *
     * Only ADMIN and INSTRUCTOR are allowed.
     */
    @PostMapping("/api/v1/assessment-results/{resultId}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<AssessmentResultResponse>> startTask(
            @PathVariable Long resultId
    ) {

        AssessmentResultResponse result =
                assessmentService.startTask(resultId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Assessment task started successfully",
                        result
                )
        );
    }

    /**
     * Record detection of a defensive task.
     *
     * Only ADMIN and INSTRUCTOR are allowed.
     */
    @PostMapping("/api/v1/assessment-results/{resultId}/detection")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<AssessmentResultResponse>>
    recordDetection(
            @PathVariable Long resultId
    ) {

        AssessmentResultResponse result =
                assessmentService.recordDetection(resultId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Assessment detection recorded successfully",
                        result
                )
        );
    }

    /**
     * Record response to a defensive task.
     *
     * Only ADMIN and INSTRUCTOR are allowed.
     */
    @PostMapping("/api/v1/assessment-results/{resultId}/response")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<AssessmentResultResponse>>
    recordResponse(
            @PathVariable Long resultId
    ) {

        AssessmentResultResponse result =
                assessmentService.recordResponse(resultId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Assessment response recorded successfully",
                        result
                )
        );
    }

    /**
     * Complete a defensive task assessment.
     *
     * Detection is required, but Response is optional.
     * Only ADMIN and INSTRUCTOR are allowed.
     */
    @PostMapping(
            "/api/v1/assessment-results/{resultId}/complete"
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<AssessmentResultResponse>>
    completeTask(
            @PathVariable Long resultId,
            @RequestBody(required = false)
            CompleteAssessmentResultRequest request
    ) {

        String reason = request == null
                ? null
                : request.reason();

        AssessmentResultResponse result =
                assessmentService.completeTask(resultId, reason);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Assessment task completed successfully",
                        result
                )
        );
    }

    /**
     * Link verified Evidence to an Assessment Score.
     *
     * Only ADMIN and INSTRUCTOR are allowed.
     */
    @PostMapping(
            "/api/v1/assessment-scores/{scoreId}/evidence"
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<AssessmentScoreEvidenceResponse>>
    linkEvidence(
            @PathVariable Long scoreId,
            @RequestBody LinkEvidenceRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Request body must not be null"
            );
        }

        AssessmentScoreEvidence link =
                scoreEvidenceService.linkEvidence(
                        scoreId,
                        request.evidenceId()
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Evidence linked to Assessment Score successfully",
                        AssessmentScoreEvidenceResponse.fromEntity(link)
                )
        );
    }

    /**
     * Retrieve Evidence linked to an Assessment Score.
     *
     * Only ADMIN and INSTRUCTOR are allowed.
     */
    @GetMapping(
            "/api/v1/assessment-scores/{scoreId}/evidence"
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<List<AssessmentScoreEvidenceResponse>>>
    getEvidenceByScore(
            @PathVariable Long scoreId
    ) {

        List<AssessmentScoreEvidenceResponse> evidence =
                scoreEvidenceService.getEvidenceByScore(scoreId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Assessment Score Evidence retrieved successfully",
                        evidence
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
     * Retrieve all assessments belonging to exercises
     * where the current STUDENT is assigned as WHITE.
     *
     * Only STUDENT is allowed.
     */
    @GetMapping("/api/v1/assessments/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<AssessmentResponse>>>
    getMyAssessments(
            @AuthenticationPrincipal Jwt jwt
    ) {

        List<AssessmentResponse> assessments =
                assessmentService.getMyAssessments(
                        jwt.getSubject(),
                        jwt.getClaimAsString("role")
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "My assessments retrieved successfully",
                        assessments
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
