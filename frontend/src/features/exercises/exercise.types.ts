export type ExerciseStatus =
    | 'CREATED'
    | 'READY'
    | 'RUNNING'
    | 'PAUSED'
    | 'STOPPED'
    | 'COMPLETED'
    | 'CANCELLED'

export interface Exercise {
    id: number
    scenarioId: number
    scenarioName: string
    cyberRangeId: number
    cyberRangeName: string
    name: string
    status: ExerciseStatus
    startedAt: string | null
    endedAt: string | null
    createdBy: number | null
    createdAt: string
}

export interface CreateExerciseRequest {
    scenarioId: number
    cyberRangeId: number
    name: string
}

export interface ScenarioOption {
    id: number
    name: string
    description: string | null
    type: string
    difficulty: string | null
    status: string
    createdBy: number | null
    createdAt: string
    updatedAt: string
}

export interface CyberRangeOption {
    id: number
    name: string
    description: string | null
    environmentType: string | null
    status: string
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
