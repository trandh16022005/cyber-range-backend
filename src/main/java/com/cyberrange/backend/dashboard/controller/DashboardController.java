
package com.cyberrange.backend.dashboard.controller;

import com.cyberrange.backend.common.response.ApiResponse;
import com.cyberrange.backend.dashboard.dto.DashboardStatisticsResponse;
import com.cyberrange.backend.dashboard.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<DashboardStatisticsResponse>> getStatistics() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Dashboard statistics retrieved successfully",
                        dashboardService.getStatistics()
                )
        );
    }
}
