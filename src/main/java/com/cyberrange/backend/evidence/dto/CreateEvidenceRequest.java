package com.cyberrange.backend.evidence.dto;

import tools.jackson.databind.JsonNode;

public record CreateEvidenceRequest(
        Long exerciseId,
        Long eventId,
        String evidenceType,
        JsonNode data
) {
}

