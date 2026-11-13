package com.cyberrange.backend.auth.service;

import com.cyberrange.backend.user.entity.User;
import com.cyberrange.backend.user.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public String login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid username or password"));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new DisabledException("Account is not active");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        return jwtService.generateToken(
                user.getId().toString(),
                user.getUsername(),
                user.getRole().getName()
        );
    }
}

