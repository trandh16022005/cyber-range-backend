import api from '../../services/api'

import type {
    Assessment,
    AssessmentResult,
    CompleteAssessmentResultRequest,
} from './assessment.types'

import type { ApiResponse } from '../exercises/exercise.types'

// Initialize assessments for eligible BLUE teams
export const initializeAssessments = async (
    exerciseId: number,
): Promise<Assessment[]> => {
    const response = await api.post<
        ApiResponse<Assessment[]>
    >(`/exercises/${exerciseId}/assessments/initialize`)

    return response.data.data
}

// Get assessments of an exercise
export const getAssessments = async (
    exerciseId: number,
): Promise<Assessment[]> => {
    const response = await api.get<
        ApiResponse<Assessment[]>
    >(`/exercises/${exerciseId}/assessments`)

    return response.data.data
}

// Get assessments assigned to the current STUDENT as WHITE
export const getMyAssessments = async (): Promise<Assessment[]> => {
    const response = await api.get<
        ApiResponse<Assessment[]>
    >('/assessments/my')

    return response.data.data
}

// Get assessment details
export const getAssessmentById = async (
    assessmentId: number,
): Promise<Assessment> => {
    const response = await api.get<
        ApiResponse<Assessment>
    >(`/assessments/${assessmentId}`)

    return response.data.data
}

// Get defensive task results of an assessment
export const getAssessmentResults = async (
    assessmentId: number,
): Promise<AssessmentResult[]> => {
    const response = await api.get<
        ApiResponse<AssessmentResult[]>
    >(`/assessments/${assessmentId}/results`)

    return response.data.data
}

// Start a defensive task
export const startAssessmentTask = async (
    resultId: number,
): Promise<AssessmentResult> => {
    const response = await api.post<
        ApiResponse<AssessmentResult>
    >(`/assessment-results/${resultId}/start`)

    return response.data.data
}

// Record detection event
export const recordAssessmentDetection = async (
    resultId: number,
): Promise<AssessmentResult> => {
    const response = await api.post<
        ApiResponse<AssessmentResult>
    >(`/assessment-results/${resultId}/detection`)

    return response.data.data
}

// Record response event
export const recordAssessmentResponse = async (
    resultId: number,
): Promise<AssessmentResult> => {
    const response = await api.post<
        ApiResponse<AssessmentResult>
    >(`/assessment-results/${resultId}/response`)

    return response.data.data
}

// Complete a defensive task
export const completeAssessmentTask = async (
    resultId: number,
    request?: CompleteAssessmentResultRequest,
): Promise<AssessmentResult> => {
    const response = await api.post<
        ApiResponse<AssessmentResult>
    >(
        `/assessment-results/${resultId}/complete`,
        request,
    )

    return response.data.data
}

// Delete an assessment (ADMIN only)
export const deleteAssessment = async (
    assessmentId: number,
): Promise<void> => {
    await api.delete<ApiResponse<void>>(
        `/assessments/${assessmentId}`,
    )
}