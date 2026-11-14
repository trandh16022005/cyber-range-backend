import api from '../../services/api'

import type {
    ApiResponse,
    CreateUserRequest,
    PageResponse,
    User,
    UserRole,
    UserStatus,
} from './user.types'

export interface GetUsersParams {
    keyword?: string
    role?: UserRole
    status?: UserStatus
    page?: number
    size?: number
}

export const userService = {
    async getUsers(
        params: GetUsersParams = {},
    ): Promise<PageResponse<User>> {
        const response = await api.get<ApiResponse<PageResponse<User>>>(
            '/users',
            {
                params,
            },
        )

        return response.data.data
    },

    async createUser(data: CreateUserRequest): Promise<User> {
        const response = await api.post<ApiResponse<User>>(
            '/users',
            data,
        )

        return response.data.data
    },
}
