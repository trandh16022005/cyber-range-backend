import api from '../../services/api'

import type {
    ApiResponse,
    CyberRangeOption,
    CreateExerciseRequest,
    Exercise,
    PageResponse,
    ScenarioOption,
    AddExerciseParticipantRequest,
    ExerciseParticipant,
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

// Get participants of an exercise
export const getExerciseParticipants = async (
    exerciseId: number,
): Promise<ExerciseParticipant[]> => {
    const response = await api.get<
        ApiResponse<ExerciseParticipant[]>
    >(`/exercises/${exerciseId}/participants`)

    return response.data.data
}

// Add participant to an exercise
export const addExerciseParticipant = async (
    exerciseId: number,
    request: AddExerciseParticipantRequest,
): Promise<ExerciseParticipant> => {
    const response = await api.post<
        ApiResponse<ExerciseParticipant>
    >(`/exercises/${exerciseId}/participants`, request)

    return response.data.data
}

// Remove participant from an exercise
export const removeExerciseParticipant = async (
    exerciseId: number,
    userId: number,
): Promise<void> => {
    await api.delete<ApiResponse<void>>(
        `/exercises/${exerciseId}/participants/${userId}`,
    )
}