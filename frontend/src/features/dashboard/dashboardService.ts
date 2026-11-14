import api from '../../services/api'

export interface DashboardStatistics {
    totalUsers: number
    activeExercises: number
    completedAssessments: number
}

interface ApiResponse<T> {
    success: boolean
    message: string
    data: T
    timestamp: string
}

export const dashboardService = {
    async getStatistics(): Promise<DashboardStatistics> {
        const response = await api.get<ApiResponse<DashboardStatistics>>(
            '/dashboard/statistics'
        )

        return response.data.data
    },
}
