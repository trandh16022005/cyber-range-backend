package com.cyberrange.backend.lab.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class SshRunnerCredential {

    private final String runnerToken;
    private final String collectorToken;

    public SshRunnerCredential(
            @Value("${SSH_LAB_RUNNER_TOKEN:}") String runnerToken,
            @Value("${SSH_LAB_COLLECTOR_TOKEN:}") String collectorToken
    ) {
        this.runnerToken = runnerToken;
        this.collectorToken = collectorToken;
    }

    public void requireRunner(String token) {
        verify(runnerToken, token);
    }

    public void requireCollector(String token) {
        verify(collectorToken, token);
    }

    private void verify(String expected, String token) {
        if (expected.length() < 32 || token == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
            throw new AccessDeniedException("Invalid SSH Lab service credential");
        }
    }
}
