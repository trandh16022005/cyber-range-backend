package com.cyberrange.backend.dashboard.dto;

public record DashboardStatisticsResponse(
        long totalUsers,
        long activeExercises,
        long completedAssessments
) {
}

