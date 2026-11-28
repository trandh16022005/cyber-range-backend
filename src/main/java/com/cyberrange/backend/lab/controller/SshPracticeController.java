package com.cyberrange.backend.lab.controller;

import com.cyberrange.backend.common.response.ApiResponse;
import com.cyberrange.backend.lab.service.SshPracticeService;
import com.cyberrange.backend.lab.service.SshPracticeService.Action;
import com.cyberrange.backend.lab.service.SshPracticeService.View;
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

    public record Approve(long redTeamId) {}
    public record ConnectionResult(boolean success) {}

    private final SshPracticeService practice;
    private final SshRunnerCredential credentials;

    public SshPracticeController(SshPracticeService practice, SshRunnerCredential credentials) {
        this.practice = practice;
        this.credentials = credentials;
    }

    @GetMapping("/api/v1/ssh-practice")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<View>> assigned(@AuthenticationPrincipal Jwt jwt) {
        practice.requireSchema();
        return ApiResponse.success("Bài được phân công", practice.assigned(Long.parseLong(jwt.getSubject())));
    }

    @GetMapping("/api/v1/assessments/{id}/ssh-practice")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<List<View>> list(@PathVariable long id) {
        practice.requireSchema();
        return ApiResponse.success("Lượt thực hành", practice.forAssessment(id));
    }

    @PostMapping("/api/v1/assessments/{id}/ssh-practice")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<View> approve(@PathVariable long id, @RequestBody Approve body, @AuthenticationPrincipal Jwt jwt) {
        practice.requireSchema();
        return ApiResponse.success("Đã duyệt lượt", practice.approve(id, body.redTeamId(), Long.parseLong(jwt.getSubject())));
    }

    @PostMapping("/api/v1/ssh-practice/{id}/actions")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR', 'STUDENT')")
    public ApiResponse<View> action(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt, @RequestBody Action action) {
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
                                    @RequestHeader("X-Lab-Credential") String token, @RequestBody ConnectionResult body) {
        credentials.requireRunner(token);
        practice.requireSchema();
        practice.finishConnection(session, id, body.success());
        return ApiResponse.success("Đã ghi trạng thái kết nối");
    }
}
