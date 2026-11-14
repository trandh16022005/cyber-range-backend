import api from './api'

export interface LoginRequest {
    username: string
    password: string
}

export interface LoginResponse {
    token: string
}

interface ApiResponse<T> {
    success: boolean
    message: string
    data: T
    timestamp: string
}

export const authService = {
    async login(credentials: LoginRequest): Promise<LoginResponse> {
        const response = await api.post<ApiResponse<LoginResponse>>(
            '/auth/login',
            credentials,
        )

        const result = response.data

        if (!result.success || !result.data?.token) {
            throw new Error(result.message || 'Login failed')
        }

        return result.data
    },
}
