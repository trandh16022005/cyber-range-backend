package com.cyberrange.backend.asset.dto;

public record UpdateAssetRequest(
        String name,
        String assetType,
        String hostname,
        String ipAddress,
        String os
) {
}
