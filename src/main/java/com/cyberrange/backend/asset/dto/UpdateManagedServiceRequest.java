package com.cyberrange.backend.asset.dto;

public record UpdateManagedServiceRequest(
        String name,
        String serviceType,
        Integer port,
        String protocol
) {
}