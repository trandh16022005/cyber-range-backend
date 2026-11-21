import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import { ClipboardList, Plus, RefreshCw } from 'lucide-react'

import { useAuth } from '../auth/AuthContext'

import {
    getExercises,
} from './exerciseService'

import type {
    Exercise,
    ExerciseStatus,
} from './exercise.types'

import ExerciseTable from './components/ExerciseTable'
import ExerciseFormModal from './components/ExerciseFormModal'

const PAGE_SIZE = 10

const statuses: ExerciseStatus[] = [
    'CREATED',
    'READY',
    'RUNNING',
    'PAUSED',
    'STOPPED',
    'COMPLETED',
    'CANCELLED',
]

function ExerciseManagementPage() {
    const { role } = useAuth()
    const navigate = useNavigate()

    const [exercises, setExercises] = useState<Exercise[]>([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)

    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(0)
    const [totalElements, setTotalElements] = useState(0)

    const [statusFilter, setStatusFilter] = useState('')
    const [showCreateModal, setShowCreateModal] = useState(false)
    const [refreshKey, setRefreshKey] = useState(0)

    const canManage =
        role === 'ADMIN' || role === 'INSTRUCTOR'

    const loadExercises = useCallback(async () => {
        setLoading(true)
        setError(null)

        try {
            const result = await getExercises({
                page,
                size: PAGE_SIZE,
                status: statusFilter || undefined,
            })

            setExercises(result.content)
            setTotalPages(result.totalPages)
            setTotalElements(result.totalElements)
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
    }, [page, statusFilter])

    useEffect(() => {
        if (canManage) {
            void loadExercises()
        }
    }, [canManage, loadExercises, refreshKey])

    const handleStatusChange = (value: string) => {
        setStatusFilter(value)
        setPage(0)
    }

    const handleCreateSuccess = () => {
        setPage(0)
        setRefreshKey((current) => current + 1)
    }

    const handleRefresh = () => {
        setRefreshKey((current) => current + 1)
    }

    if (!canManage) {
        return (
            <div className="rounded-xl border border-red-200 bg-red-50 p-6">
                <h2 className="font-semibold text-red-800">
                    Access Denied
                </h2>

                <p className="mt-1 text-sm text-red-700">
                    You do not have permission to manage exercises.
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
                            Exercise Management
                        </h1>
                    </div>

                    <p className="mt-1 text-sm text-slate-500">
                        Manage Cyber Range exercises and their configurations.
                    </p>
                </div>

                <div className="flex flex-wrap gap-2">
                    <button
                        type="button"
                        onClick={handleRefresh}
                        disabled={loading}
                        className="inline-flex items-center gap-2 rounded-lg border border-slate-300 bg-white px-4 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
                    >
                        <RefreshCw className="h-4 w-4" />
                        Refresh
                    </button>

                    <button
                        type="button"
                        onClick={() => setShowCreateModal(true)}
                        className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-blue-700"
                    >
                        <Plus className="h-4 w-4" />
                        Create Exercise
                    </button>
                </div>
            </div>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div className="rounded-xl border border-slate-200 bg-white p-5">
                    <p className="text-sm text-slate-500">
                        Total Exercises
                    </p>

                    <p className="mt-2 text-2xl font-bold text-slate-900">
                        {totalElements}
                    </p>
                </div>

                <div className="rounded-xl border border-slate-200 bg-white p-5">
                    <p className="text-sm text-slate-500">
                        Current Page
                    </p>

                    <p className="mt-2 text-2xl font-bold text-slate-900">
                        {totalPages === 0 ? 0 : page + 1}
                        <span className="ml-1 text-base font-normal text-slate-400">
                            / {totalPages}
                        </span>
                    </p>
                </div>
            </div>

            <div className="overflow-hidden rounded-xl border border-slate-200 bg-white">
                <div className="flex flex-col justify-between gap-3 border-b border-slate-200 p-4 sm:flex-row sm:items-center">
                    <div>
                        <h2 className="font-semibold text-slate-900">
                            Exercise List
                        </h2>

                        <p className="mt-1 text-xs text-slate-500">
                            View and manage available exercises.
                        </p>
                    </div>

                    <div className="flex items-center gap-2">
                        <label
                            htmlFor="exercise-status"
                            className="text-sm text-slate-600"
                        >
                            Status:
                        </label>

                        <select
                            id="exercise-status"
                            value={statusFilter}
                            onChange={(event) =>
                                handleStatusChange(event.target.value)
                            }
                            className="rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500"
                        >
                            <option value="">All statuses</option>

                            {statuses.map((status) => (
                                <option key={status} value={status}>
                                    {status}
                                </option>
                            ))}
                        </select>
                    </div>
                </div>

                {error && (
                    <div className="m-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                        {error}
                    </div>
                )}

                <ExerciseTable
                    exercises={exercises}
                    loading={loading}
                    onView={(exercise) =>
                        navigate(`/exercises/${exercise.id}`)
                    }
                />

                <div className="flex flex-col justify-between gap-3 border-t border-slate-200 px-4 py-3 sm:flex-row sm:items-center">
                    <p className="text-sm text-slate-500">
                        Showing {exercises.length} of {totalElements} exercises
                    </p>

                    <div className="flex items-center gap-2">
                        <button
                            type="button"
                            disabled={page === 0 || loading}
                            onClick={() => setPage((current) => current - 1)}
                            className="rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
                        >
                            Previous
                        </button>

                        <span className="text-sm text-slate-600">
                            Page {totalPages === 0 ? 0 : page + 1} of {totalPages}
                        </span>

                        <button
                            type="button"
                            disabled={page + 1 >= totalPages || loading}
                            onClick={() => setPage((current) => current + 1)}
                            className="rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
                        >
                            Next
                        </button>
                    </div>
                </div>
            </div>

            <ExerciseFormModal
                isOpen={showCreateModal}
                onClose={() => setShowCreateModal(false)}
                onSuccess={handleCreateSuccess}
            />
        </div>
    )
}

export default ExerciseManagementPage
