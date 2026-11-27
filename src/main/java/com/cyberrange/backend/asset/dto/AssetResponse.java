package com.cyberrange.backend.asset.dto;

import com.cyberrange.backend.asset.entity.Asset;

public record AssetResponse(
        Long id,
        Long cyberRangeId,
        String cyberRangeName,
        String name,
        String assetType,
        String hostname,
        String ipAddress,
        String os,
        String status
) {

    public static AssetResponse fromEntity(Asset asset) {
        return new AssetResponse(
                asset.getId(),
                asset.getCyberRange().getId(),
                asset.getCyberRange().getName(),
                asset.getName(),
                asset.getAssetType(),
                asset.getHostname(),
                asset.getIpAddress(),
                asset.getOs(),
                asset.getStatus()
        );
    }
}
