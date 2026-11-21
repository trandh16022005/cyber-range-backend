package com.cyberrange.backend.lab.controller;

import com.cyberrange.backend.common.response.ApiResponse;
import com.cyberrange.backend.lab.dto.CyberRangeResponse;
import com.cyberrange.backend.lab.service.CyberRangeService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cyber-ranges")
@PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
public class CyberRangeController {

    private final CyberRangeService cyberRangeService;

    public CyberRangeController(
            CyberRangeService cyberRangeService
    ) {
        this.cyberRangeService = cyberRangeService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CyberRangeResponse>>>
    getCyberRanges(
            @RequestParam(required = false) String status
    ) {
        List<CyberRangeResponse> cyberRanges =
                cyberRangeService.getCyberRanges(status);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Cyber Ranges retrieved successfully",
                        cyberRanges
                )
        );
    }
}

