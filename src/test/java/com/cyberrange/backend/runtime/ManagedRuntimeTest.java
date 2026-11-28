package com.cyberrange.backend.runtime;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagedRuntimeTest {
    @Test
    void reportsMissingCredentialsAndInvalidRuntimeSettingsTogether() {
        var environment = mainEnvironment()
                .withProperty("server.address", "0.0.0.0")
                .withProperty("server.port", "9090")
                .withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/other_db");

        String message = assertThrows(IllegalStateException.class,
                () -> ManagedRuntime.validateConfiguration(environment)).getMessage();
        for (String name : new String[]{"DATABASE_PASSWORD", "JWT_SECRET", "SERVER_ADDRESS", "SERVER_PORT", "DATABASE_URL"}) {
            assertTrue(message.contains(name), name);
        }
    }

    @Test
    void validatesJwtWithoutEchoingTheValue() {
        String invalid = "invalid-base64-value!";
        var environment = mainEnvironment().withProperty("JWT_SECRET", invalid);
        String message = assertThrows(IllegalStateException.class,
                () -> ManagedRuntime.validateConfiguration(environment)).getMessage();
        assertTrue(message.contains("DATABASE_PASSWORD is missing"));
        assertTrue(message.contains("JWT_SECRET must be Base64"));
        assertFalse(message.contains(invalid));
    }

    @Test
    void acceptsResolvedCredentials() {
        var key = new byte[32];
        new SecureRandom().nextBytes(key);
        var environment = mainEnvironment()
                .withProperty("DATABASE_PASSWORD", "test-only")
                .withProperty("JWT_SECRET", Base64.getEncoder().encodeToString(key));
        assertDoesNotThrow(() -> ManagedRuntime.validateConfiguration(environment));
    }

    private MockEnvironment mainEnvironment() {
        return new MockEnvironment()
                .withProperty("server.address", "127.0.0.1")
                .withProperty("server.port", "8080")
                .withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/cyber_range_db")
                .withProperty("spring.datasource.username", "postgres")
                .withProperty("spring.datasource.password", "${DATABASE_PASSWORD}")
                .withProperty("app.jwt.secret", "${JWT_SECRET}");
    }
}
