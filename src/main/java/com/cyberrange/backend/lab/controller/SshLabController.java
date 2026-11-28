package com.cyberrange.backend.lab.controller;

import com.cyberrange.backend.common.response.ApiResponse;
import com.cyberrange.backend.assessment.service.BlueSshAccess;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.cyberrange.backend.lab.repository.SshLabRepository.Session;
import com.cyberrange.backend.lab.service.SshLabService;
import com.cyberrange.backend.lab.service.SshLabService.Health;
import com.cyberrange.backend.lab.service.SshLabService.Status;
import com.cyberrange.backend.lab.service.SshRunnerCredential;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class SshLabController {

    public record StartRequest(Long assessmentId) {}
    public record Heartbeat(String status, Health health) {}

    private final SshLabService labs;
    private final SshRunnerCredential credentials;

    private final BlueSshAccess blueAccess;

    public SshLabController(SshLabService labs, SshRunnerCredential credentials, BlueSshAccess blueAccess) {
        this.labs = labs;
        this.credentials = credentials;
        this.blueAccess = blueAccess;
    }

    @GetMapping("/api/v1/exercises/{exerciseId}/ssh-lab")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR', 'STUDENT')")
    public ApiResponse<Status> get(@PathVariable long exerciseId, @AuthenticationPrincipal Jwt jwt) {
        boolean student = BlueSshAccess.isStudent(jwt.getClaimAsString("role"));
        long actor = Long.parseLong(jwt.getSubject());
        if (student) {
            blueAccess.requireTeam(exerciseId, actor);
        }
        Status status = labs.status(exerciseId);
        if (student && status != null) {
            try {
                blueAccess.requireAssessment(exerciseId, status.assessmentId(), actor);
            } catch (AccessDeniedException denied) {
                status = null;
            }
        }
        return ApiResponse.success("SSH Lab status", status);
    }

    @PostMapping("/api/v1/exercises/{exerciseId}/ssh-lab/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<Status> start(@PathVariable long exerciseId, @RequestBody StartRequest request) {
        if (request.assessmentId() == null || request.assessmentId() <= 0) {
            throw new IllegalArgumentException("Assessment ID is required");
        }
        return ApiResponse.success("SSH Lab start requested", labs.start(exerciseId, request.assessmentId()));
    }

    @PostMapping("/api/v1/exercises/{exerciseId}/ssh-lab/stop")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<Status> stop(@PathVariable long exerciseId) {
        return ApiResponse.success("SSH Lab stop requested", labs.stop(exerciseId));
    }

    @GetMapping("/api/v1/lab-agent/desired")
    public ApiResponse<Session> desired(@RequestHeader(value = "X-Lab-Credential", required = false) String token) {
        credentials.requireRunner(token);
        return ApiResponse.success("Desired SSH Lab", labs.desired());
    }

    @PostMapping("/api/v1/lab-agent/{id}/heartbeat")
    public ApiResponse<Void> heartbeat(@PathVariable UUID id,
                                       @RequestHeader(value = "X-Lab-Credential", required = false) String token,
                                       @RequestBody Heartbeat request) {
        credentials.requireRunner(token);
        labs.heartbeat(id, request.status(), request.health());
        return ApiResponse.success("Heartbeat accepted");
    }
}
