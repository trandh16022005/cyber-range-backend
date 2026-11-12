
package com.cyberrange.backend.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final long expirationSeconds;
    private final String issuer;

    public JwtService(
            JwtEncoder jwtEncoder,
            JwtDecoder jwtDecoder,
            @Value("${app.jwt.expiration-seconds}") long expirationSeconds,
            @Value("${app.jwt.issuer}") String issuer
    ) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.expirationSeconds = expirationSeconds;
        this.issuer = issuer;
    }

    /**
     * Generate and sign a JWT for an authenticated user.
     */
    public String generateToken(
            String userId,
            String username,
            String role
    ) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(userId)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expirationSeconds))
                .claim("username", username)
                .claim("role", role)
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

    /**
     * Verify a JWT and return its decoded claims.
     * Invalid or expired tokens cause JwtException.
     */
    public Jwt verifyToken(String token) {
        return jwtDecoder.decode(token);
    }
}

