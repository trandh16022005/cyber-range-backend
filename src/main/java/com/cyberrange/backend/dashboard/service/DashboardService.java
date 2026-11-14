package com.cyberrange.backend.dashboard.service;

import com.cyberrange.backend.dashboard.dto.DashboardStatisticsResponse;
import com.cyberrange.backend.dashboard.repository.DashboardRepository;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final DashboardRepository dashboardRepository;

    public DashboardService(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    public DashboardStatisticsResponse getStatistics() {
        return new DashboardStatisticsResponse(
                dashboardRepository.countTotalUsers(),
                dashboardRepository.countActiveExercises(),
                dashboardRepository.countCompletedAssessments()
        );
    }
}