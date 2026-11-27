package com.cyberrange.backend.event.dto;

import com.cyberrange.backend.event.entity.Event;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

public record EventResponse(
        Long id,
        Long exerciseId,
        Long assetId,
        String assetName,
        Long serviceId,
        String serviceName,
        String eventType,
        String severity,
        Instant eventTimestamp,
        String source,
        JsonNode data,
        Instant createdAt
) {

    public static EventResponse fromEntity(Event event) {
        return new EventResponse(
                event.getId(),
                event.getExercise().getId(),
                event.getAsset() != null
                        ? event.getAsset().getId()
                        : null,
                event.getAsset() != null
                        ? event.getAsset().getName()
                        : null,
                event.getService() != null
                        ? event.getService().getId()
                        : null,
                event.getService() != null
                        ? event.getService().getName()
                        : null,
                event.getEventType(),
                event.getSeverity(),
                event.getEventTimestamp(),
                event.getSource(),
                event.getData(),
                event.getCreatedAt()
        );
    }
}

