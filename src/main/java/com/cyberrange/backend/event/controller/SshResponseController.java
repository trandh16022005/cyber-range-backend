package com.cyberrange.backend.event.controller;

import com.cyberrange.backend.assessment.service.BlueSshAccess;
import com.cyberrange.backend.assessment.service.SshAssessmentEvaluator;
import com.cyberrange.backend.assessment.service.SshAssessmentEvaluator.Evaluation;
import com.cyberrange.backend.assessment.service.SshAssessmentEvaluator.EvaluationRequest;
import com.cyberrange.backend.common.response.ApiResponse;
import com.cyberrange.backend.event.repository.SshAlertRepository.Alert;
import com.cyberrange.backend.event.service.SshResponseWorkflow;
import com.cyberrange.backend.event.service.SshResponseWorkflow.*;
import com.cyberrange.backend.lab.service.SshRunnerCredential;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class SshResponseController {

    private final SshResponseWorkflow workflow;
    private final SshRunnerCredential credential;

    private final SshAssessmentEvaluator evaluator;

    public SshResponseController(SshResponseWorkflow workflow, SshRunnerCredential credential,
                                 SshAssessmentEvaluator evaluator) {
        this.workflow = workflow;
        this.credential = credential;
        this.evaluator = evaluator;
    }

    @PostMapping("/api/v1/exercises/{exerciseId}/security-alerts/{alertId}/assessment")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<Evaluation> evaluate(@PathVariable long exerciseId, @PathVariable long alertId,
                                            @AuthenticationPrincipal Jwt jwt, @RequestBody EvaluationRequest request) {
        return ApiResponse.success("SSH Assessment evaluated",
                evaluator.evaluate(exerciseId, alertId, Long.parseLong(jwt.getSubject()), request));
    }

    @GetMapping("/api/v1/exercises/{exerciseId}/security-alerts")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR', 'STUDENT')")
    public ApiResponse<List<Alert>> list(@PathVariable long exerciseId, @RequestParam(required = false) String status,
                                         @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success("Security Alerts", workflow.list(exerciseId, status, studentId(jwt)));
    }

    @GetMapping("/api/v1/exercises/{exerciseId}/security-alerts/{alertId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR', 'STUDENT')")
    public ApiResponse<Details> detail(@PathVariable long exerciseId, @PathVariable long alertId,
                                       @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success("Security Alert", workflow.detail(exerciseId, alertId, studentId(jwt)));
    }

    @PostMapping("/api/v1/exercises/{exerciseId}/security-alerts/{alertId}/actions")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR', 'STUDENT')")
    public ApiResponse<Details> act(@PathVariable long exerciseId, @PathVariable long alertId, @AuthenticationPrincipal Jwt jwt,
                                    @RequestBody Command command) {
        return ApiResponse.success("Response action recorded", workflow.command(exerciseId, alertId,
                Long.parseLong(jwt.getSubject()), command, studentId(jwt) != null));
    }

    private static Long studentId(Jwt jwt) {
        return BlueSshAccess.isStudent(jwt.getClaimAsString("role")) ? Long.valueOf(jwt.getSubject()) : null;
    }

    @GetMapping("/api/v1/lab-agent/{id}/containment")
    public ApiResponse<Containment> next(@PathVariable UUID id,
                                         @RequestHeader(value = "X-Lab-Credential", required = false) String token) {
        credential.requireRunner(token);
        return ApiResponse.success("Pending containment", workflow.nextContainment(id));
    }

    @PostMapping("/api/v1/lab-agent/{id}/containment/{alertId}")
    public ApiResponse<Void> result(@PathVariable UUID id, @PathVariable long alertId,
                                     @RequestHeader(value = "X-Lab-Credential", required = false) String token,
                                     @RequestBody ProbeResult result) {
        credential.requireRunner(token);
        workflow.recordContainment(id, alertId, result);
        return ApiResponse.success("Probe result accepted");
    }
}
