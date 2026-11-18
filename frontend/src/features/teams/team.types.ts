export interface Team {
    id: number
    name: string
    description: string | null
    createdAt: string
}

export interface CreateTeamRequest {
    name: string
    description: string | null
}

export interface UpdateTeamRequest {
    name?: string
    description?: string
}

export interface TeamMember {
    userId: number
    username: string
    email: string
    fullName: string
    role: 'ADMIN' | 'INSTRUCTOR' | 'STUDENT'
    joinedAt: string
}

export interface AddTeamMemberRequest {
    userId: number
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
