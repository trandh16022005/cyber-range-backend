package com.cyberrange.backend.asset.dto;

import com.cyberrange.backend.asset.entity.ManagedService;

public record ManagedServiceResponse(
        Long id,
        Long assetId,
        String assetName,
        String name,
        String serviceType,
        Integer port,
        String protocol,
        String status
) {
    public static ManagedServiceResponse fromEntity(
            ManagedService service
    ) {
        return new ManagedServiceResponse(
                service.getId(),
                service.getAsset().getId(),
                service.getAsset().getName(),
                service.getName(),
                service.getServiceType(),
                service.getPort(),
                service.getProtocol(),
                service.getStatus()
        );
    }
}

