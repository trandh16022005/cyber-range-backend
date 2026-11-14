package com.cyberrange.backend.user.service;

import com.cyberrange.backend.common.exception.ResourceNotFoundException;
import com.cyberrange.backend.common.response.PageResponse;
import com.cyberrange.backend.user.dto.UserResponse;
import com.cyberrange.backend.user.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public PageResponse<UserResponse> getUsers(
            String keyword,
            String role,
            String status,
            int page,
            int size
    ) {

        // Normalize optional filters
        keyword = normalize(keyword);

        if (keyword == null) {
            keyword = "";
        }

        role = normalize(role);
        status = normalize(status);

        // Validate pagination
        if (page < 0) {
            throw new IllegalArgumentException("Page must not be negative");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100"
            );
        }

        // Sort by newest users first
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // Query database
        Page<UserResponse> users = userRepository.searchUsers(
                keyword,
                role,
                status,
                pageable
        ).map(UserResponse::fromEntity);

        // Return paginated response
        return PageResponse.from(users);
    }

    // Get user details by ID
    public UserResponse getUserById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::fromEntity)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with ID: " + id
                        )
                );
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}

