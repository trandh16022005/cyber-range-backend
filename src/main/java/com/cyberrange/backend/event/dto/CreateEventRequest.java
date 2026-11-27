package com.cyberrange.backend.event.dto;

import tools.jackson.databind.JsonNode;

import java.time.Instant;

public record CreateEventRequest(
        Long exerciseId,
        Long assetId,
        Long serviceId,
        String eventType,
        String severity,
        Instant eventTimestamp,
        String source,
        JsonNode data
) {
}