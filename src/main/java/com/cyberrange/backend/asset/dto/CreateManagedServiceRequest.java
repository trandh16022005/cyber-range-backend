package com.cyberrange.backend.asset.dto;

public record CreateManagedServiceRequest(
        Long assetId,
        String name,
        String serviceType,
        Integer port,
        String protocol
) {
}

