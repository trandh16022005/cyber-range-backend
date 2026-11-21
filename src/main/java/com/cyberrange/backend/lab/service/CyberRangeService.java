package com.cyberrange.backend.lab.service;

import com.cyberrange.backend.lab.dto.CyberRangeResponse;
import com.cyberrange.backend.lab.entity.CyberRange;
import com.cyberrange.backend.lab.repository.CyberRangeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class CyberRangeService {

    private final CyberRangeRepository cyberRangeRepository;

    public CyberRangeService(
            CyberRangeRepository cyberRangeRepository
    ) {
        this.cyberRangeRepository = cyberRangeRepository;
    }

    public List<CyberRangeResponse> getCyberRanges(String status) {

        List<CyberRange> cyberRanges;

        if (status == null || status.isBlank()) {
            cyberRanges = cyberRangeRepository.findAll();
        } else {
            String normalizedStatus = status
                    .trim()
                    .toUpperCase(Locale.ROOT);

            if (!List.of("ACTIVE", "INACTIVE", "MAINTENANCE")
                    .contains(normalizedStatus)) {
                throw new IllegalArgumentException(
                        "Invalid Cyber Range status"
                );
            }

            cyberRanges = cyberRangeRepository.findByStatus(
                    normalizedStatus
            );
        }

        return cyberRanges.stream()
                .map(CyberRangeResponse::fromEntity)
                .toList();
    }
}
