import { useCallback, useEffect, useState } from 'react'
import {
    AlertCircle,
    Plus,
    RefreshCw,
    Trash2,
    Users,
} from 'lucide-react'

import { useAuth } from '../../auth/AuthContext'
import {
    getExerciseParticipants,
    removeExerciseParticipant,
} from '../exerciseService'

import type { ExerciseParticipant } from '../exercise.types'

import AddParticipantModal from './AddParticipantModal'

interface ParticipantListProps {
    exerciseId: number
}

function getErrorMessage(error: unknown): string {
    if (
        typeof error === 'object' &&
        error !== null &&
        'response' in error
    ) {
        const response = (
            error as {
                response?: {
                    data?: {
                        message?: string
                    }
                }
            }
        ).response

        return response?.data?.message ?? 'Request failed.'
    }

    return 'An unexpected error occurred.'
}

function formatDate(value: string) {
    return new Date(value).toLocaleString()
}

const roleStyles: Record<string, string> = {
    RED: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
    BLUE: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
    WHITE: 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300',
}

function ParticipantList({ exerciseId }: ParticipantListProps) {
    const { role } = useAuth()

    const canManage = role === 'ADMIN' || role === 'INSTRUCTOR'

    const [participants, setParticipants] = useState<
        ExerciseParticipant[]
    >([])

    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [success, setSuccess] = useState<string | null>(null)

    const [showAddModal, setShowAddModal] = useState(false)
    const [removingUserId, setRemovingUserId] = useState<number | null>(
        null,
    )

    const loadParticipants = useCallback(async () => {
        setLoading(true)
        setError(null)

        try {
            const result = await getExerciseParticipants(exerciseId)
            setParticipants(result)
        } catch (err: unknown) {
            setError(getErrorMessage(err))
        } finally {
            setLoading(false)
        }
    }, [exerciseId])

    useEffect(() => {
        void loadParticipants()
    }, [loadParticipants])

    const handleRemove = async (participant: ExerciseParticipant) => {
        const confirmed = window.confirm(
            `Remove ${participant.username} from this Exercise?`,
        )

        if (!confirmed) {
            return
        }

        setRemovingUserId(participant.userId)
        setError(null)
        setSuccess(null)

        try {
            await removeExerciseParticipant(
                exerciseId,
                participant.userId,
            )

            setParticipants((current) =>
                current.filter(
                    (item) => item.userId !== participant.userId,
                ),
            )

            setSuccess('Participant removed successfully.')
        } catch (err: unknown) {
            setError(getErrorMessage(err))
        } finally {
            setRemovingUserId(null)
        }
    }

    const handleCreated = () => {
        setSuccess('Participant added successfully.')
        void loadParticipants()
    }

    return (
        <section className="rounded-xl border border-slate-200 bg-white dark:border-gray-700 dark:bg-gray-900">
            <div className="flex flex-col justify-between gap-3 border-b border-slate-200 px-5 py-4 sm:flex-row sm:items-center dark:border-gray-700">
                <div className="flex items-center gap-2">
                    <Users className="h-5 w-5 text-blue-600" />

                    <div>
                        <h2 className="font-semibold text-slate-900 dark:text-white">
                            Exercise Participants
                        </h2>

                        <p className="mt-1 text-xs text-slate-500">
                            {participants.length} participant(s)
                        </p>
                    </div>
                </div>

                <div className="flex items-center gap-2">
                    <button
                        type="button"
                        onClick={() => void loadParticipants()}
                        disabled={loading}
                        className="inline-flex items-center gap-2 rounded-lg border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-800"
                    >
                        <RefreshCw
                            className={`h-4 w-4 ${
                                loading ? 'animate-spin' : ''
                            }`}
                        />
                        Refresh
                    </button>

                    {canManage && (
                        <button
                            type="button"
                            onClick={() => setShowAddModal(true)}
                            className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
                        >
                            <Plus className="h-4 w-4" />
                            Add Participant
                        </button>
                    )}
                </div>
            </div>

            <div className="space-y-4 p-5">
                {error && (
                    <div
                        role="alert"
                        className="flex items-start gap-2 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-900 dark:bg-red-950/30 dark:text-red-400"
                    >
                        <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
                        <span>{error}</span>
                    </div>
                )}

                {success && (
                    <div
                        role="status"
                        className="rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700 dark:border-green-900 dark:bg-green-950/30 dark:text-green-400"
                    >
                        {success}
                    </div>
                )}

                {loading ? (
                    <div className="py-10 text-center text-sm text-slate-500">
                        Loading participants...
                    </div>
                ) : participants.length === 0 ? (
                    <div className="rounded-lg border border-dashed border-slate-300 py-10 text-center dark:border-gray-700">
                        <Users className="mx-auto mb-2 h-8 w-8 text-slate-400" />

                        <p className="text-sm font-medium text-slate-700 dark:text-gray-300">
                            No participants yet
                        </p>

                        <p className="mt-1 text-xs text-slate-500">
                            Participants assigned to this Exercise will appear here.
                        </p>
                    </div>
                ) : (
                    <div className="overflow-x-auto rounded-lg border border-slate-200 dark:border-gray-700">
                        <table className="w-full text-left text-sm">
                            <thead className="bg-slate-50 text-xs uppercase text-slate-500 dark:bg-gray-800 dark:text-gray-400">
                            <tr>
                                <th className="px-4 py-3">
                                    Participant
                                </th>
                                <th className="px-4 py-3">Email</th>
                                <th className="px-4 py-3">Team</th>
                                <th className="px-4 py-3">Exercise Role</th>
                                <th className="px-4 py-3">Joined At</th>

                                {canManage && (
                                    <th className="px-4 py-3 text-right">
                                        Actions
                                    </th>
                                )}
                            </tr>
                            </thead>

                            <tbody className="divide-y divide-slate-100 dark:divide-gray-800">
                            {participants.map((participant) => (
                                <tr key={participant.id}>
                                    <td className="px-4 py-3">
                                        <div className="font-medium text-slate-900 dark:text-white">
                                            {participant.fullName}
                                        </div>

                                        <div className="mt-1 text-xs text-slate-500">
                                            {participant.username}
                                        </div>
                                    </td>

                                    <td className="px-4 py-3 text-slate-600 dark:text-gray-300">
                                        {participant.email}
                                    </td>

                                    <td className="px-4 py-3 text-slate-600 dark:text-gray-300">
                                        {participant.teamName ?? 'No Team'}
                                    </td>

                                    <td className="px-4 py-3">
                                            <span
                                                className={`rounded-full px-2.5 py-1 text-xs font-medium ${
                                                    roleStyles[participant.role] ??
                                                    roleStyles.WHITE
                                                }`}
                                            >
                                                {participant.role}
                                            </span>
                                    </td>

                                    <td className="whitespace-nowrap px-4 py-3 text-xs text-slate-500">
                                        {formatDate(participant.joinedAt)}
                                    </td>

                                    {canManage && (
                                        <td className="px-4 py-3 text-right">
                                            <button
                                                type="button"
                                                onClick={() =>
                                                    void handleRemove(
                                                        participant,
                                                    )
                                                }
                                                disabled={
                                                    removingUserId ===
                                                    participant.userId
                                                }
                                                className="inline-flex items-center gap-1 rounded-lg px-3 py-2 text-xs font-medium text-red-600 hover:bg-red-50 disabled:opacity-50 dark:text-red-400 dark:hover:bg-red-950/30"
                                            >
                                                <Trash2 className="h-4 w-4" />
                                                {removingUserId ===
                                                participant.userId
                                                    ? 'Removing...'
                                                    : 'Remove'}
                                            </button>
                                        </td>
                                    )}
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            {canManage && (
                <AddParticipantModal
                    open={showAddModal}
                    exerciseId={exerciseId}
                    existingUserIds={participants.map(
                        (participant) => participant.userId,
                    )}
                    onClose={() => setShowAddModal(false)}
                    onCreated={handleCreated}
                />
            )}
        </section>
    )
}

export default ParticipantList