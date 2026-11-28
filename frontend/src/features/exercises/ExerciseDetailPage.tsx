import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import {
    ArrowLeft,
    Calendar,
    ClipboardList,
    Server,
} from 'lucide-react'

import { getExerciseById } from './exerciseService'
import type { Exercise } from './exercise.types'

import ParticipantList from './components/ParticipantList'
import ExerciseLifecycleControls from './components/ExerciseLifecycleControls'
import { useAuth } from '../auth/AuthContext'

const statusStyles: Record<string, string> = {
    CREATED: 'bg-slate-100 text-slate-700',
    READY: 'bg-blue-100 text-blue-700',
    RUNNING: 'bg-green-100 text-green-700',
    PAUSED: 'bg-amber-100 text-amber-700',
    STOPPED: 'bg-orange-100 text-orange-700',
    COMPLETED: 'bg-emerald-100 text-emerald-700',
    CANCELLED: 'bg-red-100 text-red-700',
}

function formatDate(value: string | null) {
    if (!value) return 'Not available'

    return new Date(value).toLocaleString()
}

function ExerciseDetailPage() {
    const { role } = useAuth()
    const { id } = useParams()
    const navigate = useNavigate()

    const [exercise, setExercise] = useState<Exercise | null>(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        const loadExercise = async () => {
            if (!id || !/^\d+$/.test(id) || Number(id) <= 0) {
                setError('Invalid exercise ID.')
                setLoading(false)
                return
            }

            try {
                const result = await getExerciseById(Number(id))
                setExercise(result)
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

                setError(message ?? 'Failed to load exercise details.')
            } finally {
                setLoading(false)
            }
        }

        void loadExercise()
    }, [id])

    if (loading) {
        return (
            <div className="py-12 text-center text-sm text-slate-500">
                Loading exercise details...
            </div>
        )
    }

    if (error || !exercise) {
        return (
            <div className="space-y-4">
                <button
                    type="button"
                    onClick={() => navigate('/exercises')}
                    className="inline-flex items-center gap-2 text-sm font-medium text-slate-600 hover:text-slate-900"
                >
                    <ArrowLeft className="h-4 w-4" />
                    Back to Exercises
                </button>

                <div className="rounded-xl border border-red-200 bg-red-50 p-5">
                    <h2 className="font-semibold text-red-800">
                        Unable to load exercise
                    </h2>

                    <p className="mt-1 text-sm text-red-700">
                        {error ?? 'Exercise not found.'}
                    </p>
                </div>
            </div>
        )
    }

    return (
        <div className="space-y-6">
            <button
                type="button"
                onClick={() => navigate('/exercises')}
                className="inline-flex items-center gap-2 text-sm font-medium text-slate-600 hover:text-slate-900"
            >
                <ArrowLeft className="h-4 w-4" />
                Back to Exercises
            </button>

            <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
                <div>
                    <div className="flex items-center gap-3">
                        <ClipboardList className="h-7 w-7 text-blue-600" />

                        <h1 className="text-2xl font-bold text-slate-900">
                            {exercise.name}
                        </h1>
                    </div>

                    <p className="mt-2 text-sm text-slate-500">
                        Exercise ID: #{exercise.id}
                    </p>
                </div>

                <span
                    className={`inline-flex w-fit rounded-full px-3 py-1.5 text-sm font-medium ${
                        statusStyles[exercise.status] ??
                        'bg-slate-100 text-slate-700'
                    }`}
                >
                    {exercise.status}
                </span>
            </div>

            <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
                <div className="rounded-xl border border-slate-200 bg-white p-5">
                    <div className="mb-4 flex items-center gap-2">
                        <ClipboardList className="h-5 w-5 text-blue-600" />

                        <h2 className="font-semibold text-slate-900">
                            Scenario Information
                        </h2>
                    </div>

                    <dl className="space-y-3 text-sm">
                        <div>
                            <dt className="text-slate-500">Scenario ID</dt>
                            <dd className="mt-1 font-medium text-slate-900">
                                #{exercise.scenarioId}
                            </dd>
                        </div>

                        <div>
                            <dt className="text-slate-500">Scenario Name</dt>
                            <dd className="mt-1 font-medium text-slate-900">
                                {exercise.scenarioName}
                            </dd>
                        </div>
                    </dl>
                </div>

                <div className="rounded-xl border border-slate-200 bg-white p-5">
                    <div className="mb-4 flex items-center gap-2">
                        <Server className="h-5 w-5 text-blue-600" />

                        <h2 className="font-semibold text-slate-900">
                            Cyber Range Information
                        </h2>
                    </div>

                    <dl className="space-y-3 text-sm">
                        <div>
                            <dt className="text-slate-500">
                                Cyber Range ID
                            </dt>
                            <dd className="mt-1 font-medium text-slate-900">
                                #{exercise.cyberRangeId}
                            </dd>
                        </div>

                        <div>
                            <dt className="text-slate-500">
                                Cyber Range Name
                            </dt>
                            <dd className="mt-1 font-medium text-slate-900">
                                {exercise.cyberRangeName}
                            </dd>
                        </div>
                    </dl>
                </div>

                <div className="rounded-xl border border-slate-200 bg-white p-5 lg:col-span-2">
                    <div className="mb-4 flex items-center gap-2">
                        <Calendar className="h-5 w-5 text-blue-600" />

                        <h2 className="font-semibold text-slate-900">
                            Exercise Timeline
                        </h2>
                    </div>

                    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                        <div>
                            <p className="text-xs text-slate-500">
                                Created At
                            </p>
                            <p className="mt-1 text-sm font-medium text-slate-900">
                                {formatDate(exercise.createdAt)}
                            </p>
                        </div>

                        <div>
                            <p className="text-xs text-slate-500">
                                Started At
                            </p>
                            <p className="mt-1 text-sm font-medium text-slate-900">
                                {formatDate(exercise.startedAt)}
                            </p>
                        </div>

                        <div>
                            <p className="text-xs text-slate-500">
                                Ended At
                            </p>
                            <p className="mt-1 text-sm font-medium text-slate-900">
                                {formatDate(exercise.endedAt)}
                            </p>
                        </div>

                        <div>
                            <p className="text-xs text-slate-500">
                                Created By
                            </p>
                            <p className="mt-1 text-sm font-medium text-slate-900">
                                {exercise.createdBy
                                    ? `User #${exercise.createdBy}`
                                    : 'Not available'}
                            </p>
                        </div>
                    </div>
                </div>
            </div>

            {/* Participant Management */}
            {(role === 'ADMIN' || role === 'INSTRUCTOR') && (
                <ExerciseLifecycleControls key={exercise.id} exercise={exercise} onUpdate={setExercise} />
            )}
            <ParticipantList exerciseId={exercise.id} />
        </div>
    )
}

export default ExerciseDetailPage
