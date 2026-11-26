import { useCallback, useEffect, useState } from 'react'
import { ClipboardList, RefreshCw } from 'lucide-react'

import { useAuth } from '../auth/AuthContext'

import { getExercises } from '../exercises/exerciseService'
import type { Exercise } from '../exercises/exercise.types'

function AssessmentManagementPage() {
    const { role } = useAuth()

    const [exercises, setExercises] = useState<Exercise[]>([])
    const [selectedExerciseId, setSelectedExerciseId] = useState<number | ''>('')

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
            const message =
                typeof err === 'object' &&
                err !== null &&
                'response' in err
                    ? (
                        err as {
                            response?: {
                                data?: { message?: string }
                            }
                        }
                    ).response?.data?.message
                    : undefined

            setError(message ?? 'Failed to load exercises.')
        } finally {
            setLoading(false)
        }
    }, [])

    useEffect(() => {
        if (canAccess) {
            void loadExercises()
        }
    }, [canAccess, loadExercises, refreshKey])

    const handleRefresh = () => {
        setRefreshKey((current) => current + 1)
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

            {error && (
                <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                    {error}
                </div>
            )}

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
                    onChange={(event) => {
                        const value = event.target.value

                        setSelectedExerciseId(
                            value === '' ? '' : Number(value)
                        )
                    }}
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

                {selectedExerciseId !== '' && (
                    <p className="mt-3 text-sm text-slate-500">
                        Selected Exercise ID: {selectedExerciseId}
                    </p>
                )}
            </div>

            {selectedExerciseId !== '' && (
                <div className="rounded-xl border border-blue-200 bg-blue-50 p-5">
                    <h2 className="font-semibold text-blue-900">
                        Exercise Selected
                    </h2>

                    <p className="mt-1 text-sm text-blue-800">
                        Assessment data for this exercise will be displayed here.
                    </p>
                </div>
            )}

            {canManage && (
                <p className="text-xs text-slate-500">
                    ADMIN and INSTRUCTOR can initialize and manage assessments.
                </p>
            )}
        </div>
    )
}

export default AssessmentManagementPage