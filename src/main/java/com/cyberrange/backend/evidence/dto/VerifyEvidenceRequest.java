package com.cyberrange.backend.evidence.dto;

public record VerifyEvidenceRequest(
        String verificationStatus,
        String verificationComment
) {
}
