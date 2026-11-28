import { useCallback, useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import {
    ClipboardList,
    Eye,
    RefreshCw,
} from 'lucide-react'

import { useAuth } from '../auth/AuthContext'

import {
    getExercises,
} from '../exercises/exerciseService'

import type {
    Exercise,
} from '../exercises/exercise.types'

import {
    getAssessments,
    getMyAssessments,
} from './assessmentService'

import type {
    Assessment,
} from './assessment.types'

function AssessmentManagementPage() {
    const { role } = useAuth()
    const [searchParams] = useSearchParams()
    const exerciseParam = Number(searchParams.get('exerciseId'))

    const [exercises, setExercises] = useState<Exercise[]>([])
    const [assessments, setAssessments] = useState<Assessment[]>([])

    const [selectedExerciseId, setSelectedExerciseId] =
        useState<number | ''>(Number.isSafeInteger(exerciseParam) && exerciseParam > 0 ? exerciseParam : '')

    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [refreshKey, setRefreshKey] = useState(0)

    const canAccess =
        role === 'ADMIN' ||
        role === 'INSTRUCTOR' ||
        role === 'STUDENT'

    const canManage =
        role === 'ADMIN' ||
        role === 'INSTRUCTOR'

    const getErrorMessage = (err: unknown): string => {
        if (
            typeof err === 'object' &&
            err !== null &&
            'response' in err
        ) {
            const message = (
                err as {
                    response?: {
                        data?: {
                            message?: string
                        }
                    }
                }
            ).response?.data?.message

            if (message) {
                return message
            }
        }

        return 'Failed to load assessment data.'
    }

    // Load exercises for ADMIN and INSTRUCTOR
    const loadExercises = useCallback(async () => {
        setLoading(true)
        setError(null)

        try {
            const result = await getExercises({
                page: 0,
                size: 100,
            })

            setExercises(result.content)
        } catch (err: unknown) {
            setError(getErrorMessage(err))
        } finally {
            setLoading(false)
        }
    }, [])

    // Load assessments assigned to STUDENT as WHITE
    const loadMyAssessments = useCallback(async () => {
        setLoading(true)
        setError(null)

        try {
            const result = await getMyAssessments()
            setAssessments(result)
        } catch (err: unknown) {
            setError(getErrorMessage(err))
        } finally {
            setLoading(false)
        }
    }, [])

    // Load data according to system role
    useEffect(() => {
        if (canManage) {
            void loadExercises()
        } else if (role === 'STUDENT') {
            void loadMyAssessments()
        } else {
            setLoading(false)
        }
    }, [
        role,
        canManage,
        loadExercises,
        loadMyAssessments,
        refreshKey,
    ])

    // Load assessments after ADMIN or INSTRUCTOR selects an exercise
    useEffect(() => {
        if (!canManage || selectedExerciseId === '') {
            return
        }

        const loadSelectedAssessments = async () => {
            setLoading(true)
            setError(null)

            try {
                const result = await getAssessments(
                    selectedExerciseId,
                )

                setAssessments(result)
            } catch (err: unknown) {
                setAssessments([])
                setError(getErrorMessage(err))
            } finally {
                setLoading(false)
            }
        }

        void loadSelectedAssessments()
    }, [
        canManage,
        selectedExerciseId,
        refreshKey,
    ])

    const handleRefresh = () => {
        setRefreshKey((current) => current + 1)
    }

    const handleExerciseChange = (value: string) => {
        setSelectedExerciseId(
            value === '' ? '' : Number(value),
        )

        setAssessments([])
        setError(null)
    }

    const getStatusStyle = (status: string) => {
        switch (status) {
            case 'COMPLETED':
                return 'bg-green-100 text-green-700'

            case 'IN_PROGRESS':
                return 'bg-blue-100 text-blue-700'

            case 'PENDING':
                return 'bg-yellow-100 text-yellow-700'

            default:
                return 'bg-slate-100 text-slate-700'
        }
    }

    if (!canAccess) {
        return (
            <div className="rounded-xl border border-red-200 bg-red-50 p-6">
                <h2 className="font-semibold text-red-800">
                    Access Denied
                </h2>

                <p className="mt-1 text-sm text-red-700">
                    You do not have permission to view assessments.
                </p>
            </div>
        )
    }

    return (
        <div className="space-y-6">
            {/* Page header */}
            <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
                <div>
                    <div className="flex items-center gap-3">
                        <ClipboardList className="h-7 w-7 text-blue-600" />

                        <h1 className="text-2xl font-bold text-slate-900">
                            Assessment Management
                        </h1>
                    </div>

                    <p className="mt-1 text-sm text-slate-500">
                        Monitor and manage defensive skills assessments.
                    </p>
                </div>

                <button
                    type="button"
                    onClick={handleRefresh}
                    disabled={loading}
                    className="inline-flex items-center gap-2 self-start rounded-lg border border-slate-300 bg-white px-4 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
                >
                    <RefreshCw className="h-4 w-4" />
                    Refresh
                </button>
            </div>

            {/* Error */}
            {error && (
                <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                    {error}
                </div>
            )}

            {/* Exercise selector */}
            {canManage && (
                <div className="rounded-xl border border-slate-200 bg-white p-5">
                    <div className="mb-3">
                        <h2 className="font-semibold text-slate-900">
                            Select Exercise
                        </h2>

                        <p className="mt-1 text-sm text-slate-500">
                            Select an exercise to view its team assessments.
                        </p>
                    </div>

                    <label
                        htmlFor="assessment-exercise"
                        className="mb-2 block text-sm font-medium text-slate-700"
                    >
                        Exercise
                    </label>

                    <select
                        id="assessment-exercise"
                        value={selectedExerciseId}
                        onChange={(event) =>
                            handleExerciseChange(event.target.value)
                        }
                        disabled={loading || exercises.length === 0}
                        className="w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm outline-none focus:border-blue-500 disabled:bg-slate-50 sm:max-w-xl"
                    >
                        <option value="">
                            {loading
                                ? 'Loading exercises...'
                                : exercises.length === 0
                                    ? 'No exercises available'
                                    : 'Select an exercise'}
                        </option>

                        {exercises.map((exercise) => (
                            <option
                                key={exercise.id}
                                value={exercise.id}
                            >
                                {exercise.name} ({exercise.status})
                            </option>
                        ))}
                    </select>
                </div>
            )}

            {/* Assessment list */}
            {(role === 'STUDENT' || selectedExerciseId !== '') && (
                <div className="overflow-hidden rounded-xl border border-slate-200 bg-white">
                    <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
                        <div>
                            <h2 className="font-semibold text-slate-900">
                                {role === 'STUDENT'
                                    ? 'My Assessments'
                                    : 'Team Assessments'}
                            </h2>

                            <p className="mt-1 text-sm text-slate-500">
                                {role === 'STUDENT'
                                    ? 'Assessments assigned to you or your BLUE Team.'
                                    : 'Assessments associated with the selected exercise.'}
                            </p>
                        </div>

                        <span className="rounded-full bg-blue-50 px-3 py-1 text-sm font-medium text-blue-700">
                            {assessments.length} assessments
                        </span>
                    </div>

                    {loading ? (
                        <div className="p-8 text-center text-sm text-slate-500">
                            Loading assessments...
                        </div>
                    ) : assessments.length === 0 ? (
                        <div className="p-8 text-center text-sm text-slate-500">
                            No assessments found.
                        </div>
                    ) : (
                        <div className="overflow-x-auto">
                            <table className="w-full text-left text-sm">
                                <thead className="bg-slate-50 text-xs uppercase text-slate-500">
                                <tr>
                                    <th className="px-5 py-3">ID</th>
                                    <th className="px-5 py-3">Team</th>
                                    <th className="px-5 py-3">Status</th>
                                    <th className="px-5 py-3">Total Score</th>
                                    <th className="px-5 py-3">Created At</th>
                                    <th className="px-5 py-3 text-right">Actions</th>
                                </tr>
                                </thead>

                                <tbody className="divide-y divide-slate-100">
                                {assessments.map((assessment) => (
                                    <tr
                                        key={assessment.id}
                                        className="hover:bg-slate-50"
                                    >
                                        <td className="px-5 py-4 font-medium">
                                            <Link
                                                to={`/assessments/${assessment.id}`}
                                                className="text-blue-600 hover:text-blue-800 hover:underline"
                                            >
                                                #{assessment.id}
                                            </Link>
                                        </td>

                                        <td className="px-5 py-4 text-slate-700">
                                            {assessment.teamName}
                                        </td>

                                        <td className="px-5 py-4">
                                                <span
                                                    className={`rounded-full px-2.5 py-1 text-xs font-medium ${getStatusStyle(assessment.status)}`}
                                                >
                                                    {assessment.status}
                                                </span>
                                        </td>

                                        <td className="px-5 py-4 text-slate-700">
                                            {assessment.totalScore}
                                        </td>

                                        <td className="px-5 py-4 text-slate-500">
                                            {new Date(
                                                assessment.createdAt,
                                            ).toLocaleString()}
                                        </td>

                                        <td className="px-5 py-4 text-right">
                                            <Link
                                                to={`/assessments/${assessment.id}`}
                                                className="inline-flex items-center gap-2 rounded-lg border border-blue-200 bg-blue-50 px-3 py-2 text-xs font-medium text-blue-700 hover:bg-blue-100"
                                            >
                                                <Eye className="h-4 w-4" />
                                                View Details
                                            </Link>
                                            {role === 'STUDENT' && assessment.sshWorkflow && (
                                                <Link className="ml-3 text-xs font-semibold text-cyan-800 underline"
                                                    to={`/assessments/${assessment.id}/ssh-lab`}>Mở bài SSH</Link>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            )}

            {canManage && (
                <p className="text-xs text-slate-500">
                    ADMIN and INSTRUCTOR can manage assessment records.
                    STUDENT can only view assigned assessments.
                </p>
            )}
        </div>
    )
}

export default AssessmentManagementPage
