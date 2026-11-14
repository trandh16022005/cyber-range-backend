package com.cyberrange.backend.user.controller;

import com.cyberrange.backend.common.response.ApiResponse;
import com.cyberrange.backend.common.response.PageResponse;
import com.cyberrange.backend.user.dto.UserResponse;
import com.cyberrange.backend.user.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<UserResponse> users = userService.getUsers(
                keyword,
                role,
                status,
                page,
                size
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Users retrieved successfully",
                        users
                )
        );
    }
}
