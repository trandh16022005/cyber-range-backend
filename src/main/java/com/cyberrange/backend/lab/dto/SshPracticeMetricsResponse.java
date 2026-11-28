package com.cyberrange.backend.lab.dto;


public record SshPracticeMetricsResponse(Long systemDetectionMillis, Long studentDetectionSeconds, Long studentResponseSeconds,
                          Long simulatedResponseSeconds) {}
