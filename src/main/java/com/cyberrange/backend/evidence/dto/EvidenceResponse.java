package com.cyberrange.backend.evidence.dto;

import com.cyberrange.backend.evidence.entity.Evidence;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

public record EvidenceResponse(
        Long id,
        Long exerciseId,
        Long eventId,
        String evidenceType,
        JsonNode data,
        Instant createdAt
) {

    public static EvidenceResponse fromEntity(Evidence evidence) {
        return new EvidenceResponse(
                evidence.getId(),
                evidence.getExercise().getId(),
                evidence.getEvent() != null
                        ? evidence.getEvent().getId()
                        : null,
                evidence.getEvidenceType(),
                evidence.getData(),
                evidence.getCreatedAt()
        );
    }
}
