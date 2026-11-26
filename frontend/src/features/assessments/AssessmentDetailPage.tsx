import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import {
    ArrowLeft,
    ClipboardList,
    RefreshCw,
    Play,
    Search,
    ShieldCheck,
    CheckCircle,
} from 'lucide-react'

import { useAuth } from '../auth/AuthContext'

import {
    getAssessmentById,
    getAssessmentResults,
    startAssessmentTask,
    recordAssessmentDetection,
    recordAssessmentResponse,
    completeAssessmentTask,
} from './assessmentService'

import type {
    Assessment,
    AssessmentResult,
} from './assessment.types'

type TaskAction =
    | 'start'
    | 'detection'
    | 'response'
    | 'complete'

function AssessmentDetailPage() {
    const { id } = useParams<{ id: string }>()

    const assessmentId = Number(id)

    const { role } = useAuth()

    const canManage =
        role === 'ADMIN' ||
        role === 'INSTRUCTOR'

    const [assessment, setAssessment] =
        useState<Assessment | null>(null)

    const [results, setResults] =
        useState<AssessmentResult[]>([])

    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [actionError, setActionError] = useState<string | null>(null)

    const [refreshKey, setRefreshKey] = useState(0)

    const [actionLoadingId, setActionLoadingId] =
        useState<number | null>(null)

    // Load assessment and task results
    const loadAssessment = useCallback(async () => {
        if (!Number.isSafeInteger(assessmentId) || assessmentId <= 0) {
            setError('Invalid assessment ID.')
            setLoading(false)
            return
        }

        setLoading(true)
        setError(null)

        try {
            const [assessmentData, resultData] = await Promise.all([
                getAssessmentById(assessmentId),
                getAssessmentResults(assessmentId),
            ])

            setAssessment(assessmentData)
            setResults(resultData)
        } catch (err: unknown) {
            const message =
                typeof err === 'object' &&
                err !== null &&
                'response' in err
                    ? (
                        err as {
                            response?: {
                                data?: {
                                    message?: string
                                }
                            }
                        }
                    ).response?.data?.message
                    : undefined

            setError(message ?? 'Failed to load assessment details.')
        } finally {
            setLoading(false)
        }
    }, [assessmentId])

    useEffect(() => {
        void loadAssessment()
    }, [loadAssessment, refreshKey])

    // Execute lifecycle action
    const performTaskAction = async (
        resultId: number,
        action: TaskAction,
    ) => {
        if (!canManage || actionLoadingId !== null) {
            return
        }

        setActionLoadingId(resultId)
        setActionError(null)

        try {
            switch (action) {
                case 'start':
                    await startAssessmentTask(resultId)
                    break

                case 'detection':
                    await recordAssessmentDetection(resultId)
                    break

                case 'response':
                    await recordAssessmentResponse(resultId)
                    break

                case 'complete':
                    await completeAssessmentTask(resultId)
                    break
            }

            await loadAssessment()
        } catch (err: unknown) {
            const message =
                typeof err === 'object' &&
                err !== null &&
                'response' in err
                    ? (
                        err as {
                            response?: {
                                data?: {
                                    message?: string
                                }
                            }
                        }
                    ).response?.data?.message
                    : undefined

            setActionError(
                message ?? 'Failed to update defensive task.',
            )
        } finally {
            setActionLoadingId(null)
        }
    }

    // Format date
    const formatDate = (value: string | null) => {
        if (!value) return 'Not recorded'

        return new Date(value).toLocaleString()
    }

    // Format duration
    const formatDuration = (seconds: number | null) => {
        if (seconds === null) return 'Not recorded'

        const hours = Math.floor(seconds / 3600)
        const minutes = Math.floor((seconds % 3600) / 60)
        const remainingSeconds = seconds % 60

        if (hours > 0) {
            return `${hours}h ${minutes}m ${remainingSeconds}s`
        }

        if (minutes > 0) {
            return `${minutes}m ${remainingSeconds}s`
        }

        return `${remainingSeconds}s`
    }

    // Status badge style
    const getStatusStyle = (status: string) => {
        switch (status) {
            case 'COMPLETED':
                return 'bg-green-100 text-green-700'

            case 'IN_PROGRESS':
                return 'bg-blue-100 text-blue-700'

            case 'FAILED':
                return 'bg-red-100 text-red-700'

            case 'NOT_APPLICABLE':
                return 'bg-slate-200 text-slate-600'

            default:
                return 'bg-amber-100 text-amber-700'
        }
    }

    // Loading state
    if (loading) {
        return (
            <div className="rounded-xl border border-slate-200 bg-white p-8 text-center text-sm text-slate-500">
                Loading assessment details...
            </div>
        )
    }

    // Error state
    if (error || !assessment) {
        return (
            <div className="space-y-4">
                <Link
                    to="/assessments"
                    className="inline-flex items-center gap-2 text-sm text-slate-600 hover:text-blue-600"
                >
                    <ArrowLeft size={16} />
                    Back to Assessments
                </Link>

                <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
                    {error ?? 'Assessment not found.'}
                </div>
            </div>
        )
    }

    return (
        <div className="space-y-6">
            {/* Page header */}
            <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
                <div>
                    <Link
                        to="/assessments"
                        className="mb-3 inline-flex items-center gap-2 text-sm text-slate-500 hover:text-blue-600"
                    >
                        <ArrowLeft size={16} />
                        Back to Assessments
                    </Link>

                    <div className="flex items-center gap-3">
                        <ClipboardList className="h-7 w-7 text-blue-600" />

                        <h1 className="text-2xl font-bold text-slate-900">
                            Assessment #{assessment.id}
                        </h1>
                    </div>

                    <p className="mt-1 text-sm text-slate-500">
                        Defensive skills assessment details and task progress.
                    </p>
                </div>

                <button
                    type="button"
                    onClick={() => setRefreshKey((current) => current + 1)}
                    disabled={actionLoadingId !== null}
                    className="inline-flex items-center gap-2 self-start rounded-lg border border-slate-300 bg-white px-4 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
                >
                    <RefreshCw size={16} />
                    Refresh
                </button>
            </div>

            {/* Lifecycle action error */}
            {actionError && (
                <div className="flex items-start justify-between gap-3 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                    <p>{actionError}</p>

                    <button
                        type="button"
                        onClick={() => setActionError(null)}
                        className="font-medium hover:text-red-900"
                    >
                        Dismiss
                    </button>
                </div>
            )}

            {/* Assessment overview */}
            <div className="rounded-xl border border-slate-200 bg-white p-5">
                <h2 className="mb-4 font-semibold text-slate-900">
                    Assessment Overview
                </h2>

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
                    <div>
                        <p className="text-sm text-slate-500">
                            Assessment ID
                        </p>
                        <p className="mt-1 font-semibold text-slate-900">
                            #{assessment.id}
                        </p>
                    </div>

                    <div>
                        <p className="text-sm text-slate-500">
                            Team
                        </p>
                        <p className="mt-1 font-semibold text-slate-900">
                            {assessment.teamName}
                        </p>
                    </div>

                    <div>
                        <p className="text-sm text-slate-500">
                            Status
                        </p>
                        <span className={`mt-1 inline-block rounded-full px-3 py-1 text-xs font-medium ${getStatusStyle(assessment.status)}`}>
                            {assessment.status}
                        </span>
                    </div>

                    <div>
                        <p className="text-sm text-slate-500">
                            Total Score
                        </p>
                        <p className="mt-1 font-semibold text-slate-900">
                            {assessment.totalScore}
                        </p>
                    </div>

                    <div>
                        <p className="text-sm text-slate-500">
                            Started At
                        </p>
                        <p className="mt-1 text-sm text-slate-700">
                            {formatDate(assessment.startedAt)}
                        </p>
                    </div>

                    <div>
                        <p className="text-sm text-slate-500">
                            Completed At
                        </p>
                        <p className="mt-1 text-sm text-slate-700">
                            {formatDate(assessment.completedAt)}
                        </p>
                    </div>

                    <div>
                        <p className="text-sm text-slate-500">
                            Review Status
                        </p>
                        <p className="mt-1 font-medium text-slate-900">
                            {assessment.reviewStatus}
                        </p>
                    </div>

                    <div>
                        <p className="text-sm text-slate-500">
                            Created At
                        </p>
                        <p className="mt-1 text-sm text-slate-700">
                            {formatDate(assessment.createdAt)}
                        </p>
                    </div>
                </div>
            </div>

            {/* Defensive task results */}
            <div className="overflow-hidden rounded-xl border border-slate-200 bg-white">
                <div className="border-b border-slate-200 px-5 py-4">
                    <h2 className="font-semibold text-slate-900">
                        Defensive Task Results
                    </h2>

                    <p className="mt-1 text-sm text-slate-500">
                        Progress and evaluation details for each defensive task.
                    </p>
                </div>

                {results.length === 0 ? (
                    <div className="p-8 text-center text-sm text-slate-500">
                        No defensive task results found.
                    </div>
                ) : (
                    <div className="divide-y divide-slate-200">
                        {results.map((result) => (
                            <div
                                key={result.id}
                                className="space-y-4 p-5"
                            >
                                {/* Task header */}
                                <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
                                    <div>
                                        <p className="text-xs font-medium text-slate-400">
                                            TASK {result.sequenceOrder}
                                        </p>

                                        <h3 className="mt-1 font-semibold text-slate-900">
                                            {result.defensiveTaskName}
                                        </h3>

                                        <p className="mt-1 text-xs text-slate-500">
                                            Result ID: #{result.id}
                                        </p>
                                    </div>

                                    <span className={`self-start rounded-full px-3 py-1 text-xs font-medium ${getStatusStyle(result.status)}`}>
                                        {result.status}
                                    </span>
                                </div>

                                {/* Lifecycle actions: ADMIN / INSTRUCTOR only */}
                                {canManage && result.status !== 'COMPLETED' && (
                                    <div className="flex flex-wrap items-center gap-2 border-b border-slate-100 pb-4">
                                        {result.status === 'PENDING' && (
                                            <button
                                                type="button"
                                                onClick={() =>
                                                    void performTaskAction(result.id, 'start')
                                                }
                                                disabled={actionLoadingId !== null}
                                                className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                                            >
                                                <Play size={16} />
                                                {actionLoadingId === result.id
                                                    ? 'Processing...'
                                                    : 'Start Task'}
                                            </button>
                                        )}

                                        {result.status === 'IN_PROGRESS' &&
                                            !result.detectedAt && (
                                                <button
                                                    type="button"
                                                    onClick={() =>
                                                        void performTaskAction(result.id, 'detection')
                                                    }
                                                    disabled={actionLoadingId !== null}
                                                    className="inline-flex items-center gap-2 rounded-lg bg-amber-500 px-3 py-2 text-sm font-medium text-white hover:bg-amber-600 disabled:cursor-not-allowed disabled:opacity-50"
                                                >
                                                    <Search size={16} />
                                                    {actionLoadingId === result.id
                                                        ? 'Processing...'
                                                        : 'Record Detection'}
                                                </button>
                                            )}

                                        {result.status === 'IN_PROGRESS' &&
                                            result.detectedAt &&
                                            !result.respondedAt && (
                                                <button
                                                    type="button"
                                                    onClick={() =>
                                                        void performTaskAction(result.id, 'response')
                                                    }
                                                    disabled={actionLoadingId !== null}
                                                    className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-3 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-50"
                                                >
                                                    <ShieldCheck size={16} />
                                                    {actionLoadingId === result.id
                                                        ? 'Processing...'
                                                        : 'Record Response'}
                                                </button>
                                            )}

                                        {result.status === 'IN_PROGRESS' &&
                                            result.detectedAt && (
                                                <button
                                                    type="button"
                                                    onClick={() =>
                                                        void performTaskAction(result.id, 'complete')
                                                    }
                                                    disabled={actionLoadingId !== null}
                                                    className="inline-flex items-center gap-2 rounded-lg bg-green-600 px-3 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:cursor-not-allowed disabled:opacity-50"
                                                >
                                                    <CheckCircle size={16} />
                                                    {actionLoadingId === result.id
                                                        ? 'Processing...'
                                                        : 'Complete Task'}
                                                </button>
                                            )}
                                    </div>
                                )}

                                {/* Task timing */}
                                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                                    <div>
                                        <p className="text-xs text-slate-500">
                                            Started At
                                        </p>
                                        <p className="mt-1 text-sm text-slate-700">
                                            {formatDate(result.startedAt)}
                                        </p>
                                    </div>

                                    <div>
                                        <p className="text-xs text-slate-500">
                                            Detected At
                                        </p>
                                        <p className="mt-1 text-sm text-slate-700">
                                            {formatDate(result.detectedAt)}
                                        </p>
                                    </div>

                                    <div>
                                        <p className="text-xs text-slate-500">
                                            Responded At
                                        </p>
                                        <p className="mt-1 text-sm text-slate-700">
                                            {formatDate(result.respondedAt)}
                                        </p>
                                    </div>

                                    <div>
                                        <p className="text-xs text-slate-500">
                                            Completed At
                                        </p>
                                        <p className="mt-1 text-sm text-slate-700">
                                            {formatDate(result.completedAt)}
                                        </p>
                                    </div>
                                </div>

                                {/* Duration and score */}
                                <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
                                    <div className="rounded-lg bg-slate-50 p-3">
                                        <p className="text-xs text-slate-500">
                                            Detection Time
                                        </p>
                                        <p className="mt-1 font-semibold text-slate-900">
                                            {formatDuration(result.detectionTimeSeconds)}
                                        </p>
                                    </div>

                                    <div className="rounded-lg bg-slate-50 p-3">
                                        <p className="text-xs text-slate-500">
                                            Response Time
                                        </p>
                                        <p className="mt-1 font-semibold text-slate-900">
                                            {formatDuration(result.responseTimeSeconds)}
                                        </p>
                                    </div>

                                    <div className="rounded-lg bg-slate-50 p-3">
                                        <p className="text-xs text-slate-500">
                                            Task Score
                                        </p>
                                        <p className="mt-1 font-semibold text-slate-900">
                                            {result.awardedScore} / {result.maxScore}
                                        </p>
                                    </div>
                                </div>

                                {/* Task reason */}
                                {result.reason && (
                                    <div>
                                        <p className="text-xs font-medium text-slate-500">
                                            Evaluation Note
                                        </p>

                                        <p className="mt-1 text-sm text-slate-700">
                                            {result.reason}
                                        </p>
                                    </div>
                                )}

                                {/* Score criteria */}
                                {result.scores.length > 0 && (
                                    <div>
                                        <h4 className="mb-2 text-sm font-medium text-slate-700">
                                            Scoring Criteria
                                        </h4>

                                        <div className="overflow-x-auto rounded-lg border border-slate-200">
                                            <table className="w-full text-left text-sm">
                                                <thead className="bg-slate-50 text-xs uppercase text-slate-500">
                                                <tr>
                                                    <th className="px-4 py-3">
                                                        Criterion
                                                    </th>

                                                    <th className="px-4 py-3">
                                                        Type
                                                    </th>

                                                    <th className="px-4 py-3">
                                                        Score
                                                    </th>

                                                    <th className="px-4 py-3">
                                                        Note
                                                    </th>
                                                </tr>
                                                </thead>

                                                <tbody className="divide-y divide-slate-100">
                                                {result.scores.map((score) => (
                                                    <tr key={score.id}>
                                                        <td className="px-4 py-3 font-medium text-slate-800">
                                                            {score.criterionName}
                                                        </td>

                                                        <td className="px-4 py-3 text-slate-600">
                                                            {score.criterionType}
                                                        </td>

                                                        <td className="px-4 py-3 text-slate-700">
                                                            {score.awardedScore} / {score.maxScore}
                                                        </td>

                                                        <td className="px-4 py-3 text-slate-600">
                                                            {score.reason ?? '—'}
                                                        </td>
                                                    </tr>
                                                ))}
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                )}
                            </div>
                        ))}
                    </div>
                )}
            </div>

            {/* Permission note */}
            {!canManage && (
                <p className="text-xs text-slate-500">
                    You have read-only access to this assessment.
                </p>
            )}
        </div>
    )
}

export default AssessmentDetailPage
