package com.cyberrange.backend.lab.controller;

import com.cyberrange.backend.lab.dto.SshPracticeApproveRequest;
import com.cyberrange.backend.lab.dto.SshPracticeConnectionResultRequest;
import com.cyberrange.backend.common.response.ApiResponse;
import com.cyberrange.backend.lab.service.SshPracticeService;
import com.cyberrange.backend.lab.dto.SshPracticeActionRequest;
import jakarta.validation.Valid;
import com.cyberrange.backend.lab.dto.SshPracticeResponse;
import com.cyberrange.backend.lab.service.SshRunnerCredential;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@ConditionalOnProperty(name = "ssh.practice.enabled", havingValue = "true")
public class SshPracticeController {

    private final SshPracticeService practice;
    private final SshRunnerCredential credentials;

    public SshPracticeController(SshPracticeService practice, SshRunnerCredential credentials) {
        this.practice = practice;
        this.credentials = credentials;
    }

    @GetMapping("/api/v1/ssh-practice")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<SshPracticeResponse>> assigned(@AuthenticationPrincipal Jwt jwt) {
        practice.requireSchema();
        return ApiResponse.success("Bài được phân công", practice.assigned(Long.parseLong(jwt.getSubject())));
    }

    @GetMapping("/api/v1/assessments/{id}/ssh-practice")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<List<SshPracticeResponse>> list(@PathVariable long id) {
        practice.requireSchema();
        return ApiResponse.success("Lượt thực hành", practice.forAssessment(id));
    }

    @PostMapping("/api/v1/assessments/{id}/ssh-practice")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<SshPracticeResponse> approve(@PathVariable long id, @RequestBody SshPracticeApproveRequest body, @AuthenticationPrincipal Jwt jwt) {
        practice.requireSchema();
        return ApiResponse.success("Đã duyệt lượt", practice.approve(id, body.redTeamId(), Long.parseLong(jwt.getSubject()), body.labSessionId()));
    }

    @GetMapping("/api/v1/assessments/{id}/ssh-practice/approval")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<UUID> approval(@PathVariable long id, @RequestParam long redTeamId) {
        practice.requireSchema();
        return ApiResponse.success("Có thể duyệt lượt", practice.approvalSession(id, redTeamId));
    }

    @PostMapping("/api/v1/ssh-practice/{id}/actions")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR', 'STUDENT')")
    public ApiResponse<SshPracticeResponse> action(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SshPracticeActionRequest action) {
        practice.requireSchema();
        var role = jwt.getClaimAsString("role");
        boolean instructor = "ADMIN".equals(role) || "INSTRUCTOR".equals(role);
        return ApiResponse.success("Đã ghi nhận", practice.act(id, Long.parseLong(jwt.getSubject()), instructor, action));
    }

    @PostMapping("/api/v1/lab-agent/{session}/practice-connection")
    public ApiResponse<UUID> claim(@PathVariable UUID session, @RequestHeader("X-Lab-Credential") String token) {
        credentials.requireRunner(token);
        practice.requireSchema();
        return ApiResponse.success("Kiểm tra kết nối", practice.claimConnection(session));
    }

    @PostMapping("/api/v1/lab-agent/{session}/practice-connection/{id}")
    public ApiResponse<Void> finish(@PathVariable UUID session, @PathVariable UUID id,
                                    @RequestHeader("X-Lab-Credential") String token, @RequestBody SshPracticeConnectionResultRequest body) {
        credentials.requireRunner(token);
        practice.requireSchema();
        practice.finishConnection(session, id, body.success());
        return ApiResponse.success("Đã ghi trạng thái kết nối");
    }
}
