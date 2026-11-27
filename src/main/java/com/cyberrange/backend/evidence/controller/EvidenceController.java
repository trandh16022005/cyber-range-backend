package com.cyberrange.backend.evidence.controller;

import com.cyberrange.backend.common.response.ApiResponse;
import com.cyberrange.backend.evidence.dto.CreateEvidenceRequest;
import com.cyberrange.backend.evidence.dto.EvidenceResponse;
import com.cyberrange.backend.evidence.dto.VerifyEvidenceRequest;
import com.cyberrange.backend.evidence.service.EvidenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/evidence")
public class EvidenceController {

    private final EvidenceService evidenceService;

    public EvidenceController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<List<EvidenceResponse>>> getEvidenceByExercise(
            @RequestParam Long exerciseId
    ) {
        List<EvidenceResponse> evidence =
                evidenceService.getEvidenceByExercise(exerciseId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Evidence retrieved successfully",
                        evidence
                )
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<EvidenceResponse>> getEvidenceById(
            @PathVariable Long id
    ) {
        EvidenceResponse evidence = evidenceService.getEvidenceById(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Evidence retrieved successfully",
                        evidence
                )
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<EvidenceResponse>> createEvidence(
            @RequestBody CreateEvidenceRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Evidence request must not be null"
            );
        }

        EvidenceResponse evidence =
                evidenceService.createEvidence(request);

        return ResponseEntity.status(201).body(
                ApiResponse.success(
                        "Evidence created successfully",
                        evidence
                )
        );
    }

    @PatchMapping("/{id}/verification")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<EvidenceResponse>> verifyEvidence(
            @PathVariable Long id,
            @RequestBody VerifyEvidenceRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        EvidenceResponse evidence = evidenceService.verifyEvidence(
                id,
                jwt.getSubject(),
                request
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Evidence verification completed successfully",
                        evidence
                )
        );
    }
}
