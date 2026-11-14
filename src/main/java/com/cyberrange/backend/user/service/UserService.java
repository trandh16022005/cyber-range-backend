package com.cyberrange.backend.user.service;

import com.cyberrange.backend.common.exception.ResourceNotFoundException;
import com.cyberrange.backend.common.response.PageResponse;
import com.cyberrange.backend.user.dto.CreateUserRequest;
import com.cyberrange.backend.user.dto.UserResponse;
import com.cyberrange.backend.user.entity.Role;
import com.cyberrange.backend.user.entity.User;
import com.cyberrange.backend.user.repository.RoleRepository;
import com.cyberrange.backend.user.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cyberrange.backend.common.exception.BusinessException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
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

    // Create a new user
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "User information is required"
            );
        }

        String username = normalize(request.username());
        String email = normalize(request.email());
        String password = request.password();
        String fullName = normalize(request.fullName());
        String roleName = normalize(request.role());

        // Validate required fields
        if (username == null || email == null
                || password == null || password.isBlank()
                || fullName == null || roleName == null) {
            throw new IllegalArgumentException(
                    "All user fields are required"
            );
        }

        // Validate username
        if (username.length() < 3 || username.length() > 100) {
            throw new IllegalArgumentException(
                    "Username must be between 3 and 100 characters"
            );
        }

        // Validate email
        if (email.length() > 255
                || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Invalid email address");
        }

        // Validate password
        int passwordBytes = password.getBytes(StandardCharsets.UTF_8).length;

        if (password.length() < 8 || passwordBytes > 72) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters "
                            + "and must not exceed 72 UTF-8 bytes"
            );
        }

        // Validate full name
        if (fullName.length() > 255) {
            throw new IllegalArgumentException(
                    "Full name must not exceed 255 characters"
            );
        }

        // Normalize role name
        final String normalizedRoleName = roleName.toUpperCase(Locale.ROOT);

        // Check duplicate username
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(
                    "Username already exists"
            );
        }

        // Check duplicate email
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(
                    "Email already exists"
            );
        }

        // Find role
        Role role = roleRepository.findByName(normalizedRoleName)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid role: " + normalizedRoleName
                        )
                );

        // Encode password and create user
        User user = new User(
                role,
                username,
                email,
                passwordEncoder.encode(password),
                fullName
        );

        // Save user
        User savedUser = userRepository.save(user);

        return UserResponse.fromEntity(savedUser);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
