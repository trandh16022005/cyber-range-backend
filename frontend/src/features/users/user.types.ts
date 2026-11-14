export type UserRole = 'ADMIN' | 'INSTRUCTOR' | 'STUDENT'

export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'LOCKED'

export interface CreateUserRequest {
    username: string
    email: string
    password: string
    fullName: string
    role: UserRole
}

export interface User {
    id: number
    username: string
    email: string
    fullName: string
    role: UserRole
    status: UserStatus
    createdAt: string
    updatedAt: string
}

export interface PageResponse<T> {
    content: T[]
    page: number
    size: number
    totalElements: number
    totalPages: number
    first: boolean
    last: boolean
    hasNext: boolean
    hasPrevious: boolean
}

export interface ApiResponse<T> {
    success: boolean
    message: string
    data: T
    timestamp: string
}