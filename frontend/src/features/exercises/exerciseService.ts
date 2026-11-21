import api from '../../services/api'

import type {
    ApiResponse,
    CyberRangeOption,
    CreateExerciseRequest,
    Exercise,
    PageResponse,
    ScenarioOption,
} from './exercise.types'

export interface ExerciseQueryParams {
    page?: number
    size?: number
    status?: string
    scenarioId?: number
    cyberRangeId?: number
}

// Get paginated exercises
export const getExercises = async (
    params: ExerciseQueryParams = {}
): Promise<PageResponse<Exercise>> => {
    const response = await api.get<ApiResponse<PageResponse<Exercise>>>(
        '/exercises',
        { params }
    )

    return response.data.data
}

// Get exercise details
export const getExerciseById = async (
    id: number
): Promise<Exercise> => {
    const response = await api.get<ApiResponse<Exercise>>(
        `/exercises/${id}`
    )

    return response.data.data
}

// Create exercise
export const createExercise = async (
    request: CreateExerciseRequest
): Promise<Exercise> => {
    const response = await api.post<ApiResponse<Exercise>>(
        '/exercises',
        request
    )

    return response.data.data
}

// Get published scenarios for exercise creation
export const getPublishedScenarios = async (): Promise<
    ScenarioOption[]
> => {
    const response = await api.get<
        ApiResponse<PageResponse<ScenarioOption>>
    >('/scenarios', {
        params: {
            page: 0,
            size: 100,
            status: 'PUBLISHED',
        },
    })

    return response.data.data.content
}

// Get active cyber ranges for exercise creation
export const getActiveCyberRanges = async (): Promise<
    CyberRangeOption[]
> => {
    const response = await api.get<
        ApiResponse<CyberRangeOption[]>
    >('/cyber-ranges', {
        params: {
            status: 'ACTIVE',
        },
    })

    return response.data.data
}
