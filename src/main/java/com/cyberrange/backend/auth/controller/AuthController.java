package com.cyberrange.backend.auth.controller;

import com.cyberrange.backend.auth.dto.LoginRequest;
import com.cyberrange.backend.auth.dto.LoginResponse;
import com.cyberrange.backend.auth.service.AuthService;
import com.cyberrange.backend.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody LoginRequest request
    ) {
        String token = authService.login(
                request.username(),
                request.password()
        );

        LoginResponse response = new LoginResponse(token);

        return ResponseEntity.ok(
                ApiResponse.success("Login successful", response)
        );
    }
}

