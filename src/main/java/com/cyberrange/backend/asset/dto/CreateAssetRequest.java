package com.cyberrange.backend.asset.dto;

public record CreateAssetRequest(
        Long cyberRangeId,
        String name,
        String assetType,
        String hostname,
        String ipAddress,
        String os
) {
}
