import api from '../../services/api'

import type {
    AddTeamMemberRequest,
    ApiResponse,
    CreateTeamRequest,
    PageResponse,
    Team,
    TeamMember,
    UpdateTeamRequest,
} from './team.types'

export interface GetTeamsParams {
    page?: number
    size?: number
}

export const teamService = {
    // Get paginated teams
    async getTeams(
        params: GetTeamsParams = {},
    ): Promise<PageResponse<Team>> {
        const response = await api.get<ApiResponse<PageResponse<Team>>>(
            '/teams',
            { params },
        )

        return response.data.data
    },

    // Get team by ID
    async getTeamById(id: number): Promise<Team> {
        const response = await api.get<ApiResponse<Team>>(
            `/teams/${id}`,
        )

        return response.data.data
    },

    // Create team
    async createTeam(data: CreateTeamRequest): Promise<Team> {
        const response = await api.post<ApiResponse<Team>>(
            '/teams',
            data,
        )

        return response.data.data
    },

    // Update team
    async updateTeam(
        id: number,
        data: UpdateTeamRequest,
    ): Promise<Team> {
        const response = await api.patch<ApiResponse<Team>>(
            `/teams/${id}`,
            data,
        )

        return response.data.data
    },

    // Delete team (ADMIN only)
    async deleteTeam(id: number): Promise<void> {
        await api.delete<ApiResponse<void>>(
            `/teams/${id}`,
        )
    },

    // Get team members
    async getTeamMembers(id: number): Promise<TeamMember[]> {
        const response = await api.get<ApiResponse<TeamMember[]>>(
            `/teams/${id}/members`,
        )

        return response.data.data
    },

    // Add team member
    async addTeamMember(
        id: number,
        data: AddTeamMemberRequest,
    ): Promise<TeamMember> {
        const response = await api.post<ApiResponse<TeamMember>>(
            `/teams/${id}/members`,
            data,
        )

        return response.data.data
    },

    // Remove team member
    async removeTeamMember(
        teamId: number,
        userId: number,
    ): Promise<void> {
        await api.delete<ApiResponse<void>>(
            `/teams/${teamId}/members/${userId}`,
        )
    },
}
