package com.cyberrange.backend.lab.dto;

import com.cyberrange.backend.lab.entity.CyberRange;

public record CyberRangeResponse(
        Long id,
        String name,
        String description,
        String environmentType,
        String status
) {

    public static CyberRangeResponse fromEntity(CyberRange cyberRange) {
        return new CyberRangeResponse(
                cyberRange.getId(),
                cyberRange.getName(),
                cyberRange.getDescription(),
                cyberRange.getEnvironmentType(),
                cyberRange.getStatus()
        );
    }
}
