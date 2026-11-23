import { useEffect, useState } from 'react'
import { UserPlus, X } from 'lucide-react'

import { addExerciseParticipant } from '../exerciseService'
import { userService } from '../../users/userService'
import { teamService } from '../../teams/teamService'

import type { ExerciseParticipantRole } from '../exercise.types'
import type { User } from '../../users/user.types'
import type { Team } from '../../teams/team.types'

interface AddParticipantModalProps {
    open: boolean
    exerciseId: number
    existingUserIds: number[]
    onClose: () => void
    onCreated: () => void
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

function AddParticipantModal({
                                 open,
                                 exerciseId,
                                 existingUserIds,
                                 onClose,
                                 onCreated,
                             }: AddParticipantModalProps) {
    const [users, setUsers] = useState<User[]>([])
    const [teams, setTeams] = useState<Team[]>([])
    const [eligibleTeams, setEligibleTeams] = useState<Team[]>([])

    const [selectedUserId, setSelectedUserId] = useState('')
    const [selectedTeamId, setSelectedTeamId] = useState('')
    const [selectedRole, setSelectedRole] =
        useState<ExerciseParticipantRole>('BLUE')

    const [loading, setLoading] = useState(false)
    const [adding, setAdding] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const availableUsers = users.filter(
        (user) => !existingUserIds.includes(user.id),
    )

    useEffect(() => {
        if (!open) {
            return
        }

        let active = true

        const loadData = async () => {
            setLoading(true)
            setError(null)

            try {
                const [userPage, teamPage] = await Promise.all([
                    userService.getUsers({
                        role: 'STUDENT',
                        status: 'ACTIVE',
                        page: 0,
                        size: 100,
                    }),
                    teamService.getTeams({
                        page: 0,
                        size: 100,
                    }),
                ])

                if (!active) return

                setUsers(userPage.content)
                setTeams(teamPage.content)
                setSelectedUserId('')
                setSelectedTeamId('')
                setSelectedRole('BLUE')
                setEligibleTeams([])
            } catch (err: unknown) {
                if (active) {
                    setError(getErrorMessage(err))
                }
            } finally {
                if (active) {
                    setLoading(false)
                }
            }
        }

        void loadData()

        return () => {
            active = false
        }
    }, [open])

    useEffect(() => {
        if (!open || !selectedUserId) {
            setEligibleTeams([])
            setSelectedTeamId('')
            return
        }

        let active = true

        const loadEligibleTeams = async () => {
            setSelectedTeamId('')
            setError(null)

            try {
                const userId = Number(selectedUserId)

                const membershipResults = await Promise.all(
                    teams.map(async (team) => {
                        const members =
                            await teamService.getTeamMembers(team.id)

                        return members.some(
                            (member) => member.userId === userId,
                        )
                            ? team
                            : null
                    }),
                )

                if (!active) return

                setEligibleTeams(
                    membershipResults.filter(
                        (team): team is Team => team !== null,
                    ),
                )
            } catch (err: unknown) {
                if (active) {
                    setError(
                        `Unable to load Team memberships: ${getErrorMessage(err)}`,
                    )
                }
            }
        }

        void loadEligibleTeams()

        return () => {
            active = false
        }
    }, [open, selectedUserId, teams])

    if (!open) {
        return null
    }

    const handleAdd = async () => {
        if (!selectedUserId) {
            setError('Please select a Student.')
            return
        }

        setAdding(true)
        setError(null)

        try {
            await addExerciseParticipant(exerciseId, {
                userId: Number(selectedUserId),
                ...(selectedTeamId
                    ? { teamId: Number(selectedTeamId) }
                    : {}),
                role: selectedRole,
            })

            onCreated()
            onClose()
        } catch (err: unknown) {
            setError(getErrorMessage(err))
        } finally {
            setAdding(false)
        }
    }

    return (
        <div
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
            onMouseDown={(event) => {
                if (event.target === event.currentTarget && !adding) {
                    onClose()
                }
            }}
        >
            <div className="w-full max-w-lg rounded-xl bg-white shadow-xl dark:bg-gray-900">
                <div className="flex items-center justify-between border-b border-gray-200 px-6 py-4 dark:border-gray-700">
                    <div>
                        <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                            Add Participant
                        </h2>

                        <p className="mt-1 text-sm text-gray-500">
                            Exercise #{exerciseId}
                        </p>
                    </div>

                    <button
                        type="button"
                        onClick={onClose}
                        disabled={adding}
                        className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 disabled:opacity-50 dark:hover:bg-gray-800"
                        aria-label="Close"
                    >
                        <X className="h-5 w-5" />
                    </button>
                </div>

                <div className="space-y-5 px-6 py-5">
                    {error && (
                        <div
                            role="alert"
                            className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-900 dark:bg-red-950/30 dark:text-red-400"
                        >
                            {error}
                        </div>
                    )}

                    <div>
                        <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
                            Student
                        </label>

                        <select
                            value={selectedUserId}
                            onChange={(event) =>
                                setSelectedUserId(event.target.value)
                            }
                            disabled={loading || adding}
                            className="w-full rounded-lg border border-gray-300 bg-white px-4 py-2.5 text-sm text-gray-900 outline-none focus:border-blue-500 disabled:bg-gray-100 dark:border-gray-600 dark:bg-gray-800 dark:text-white"
                        >
                            <option value="">
                                {loading
                                    ? 'Loading Students...'
                                    : availableUsers.length === 0
                                        ? 'No available Students'
                                        : 'Select a Student'}
                            </option>

                            {availableUsers.map((user) => (
                                <option key={user.id} value={user.id}>
                                    {user.fullName} ({user.username})
                                </option>
                            ))}
                        </select>
                    </div>

                    <div>
                        <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
                            Exercise Role
                        </label>

                        <select
                            value={selectedRole}
                            onChange={(event) =>
                                setSelectedRole(
                                    event.target.value as ExerciseParticipantRole,
                                )
                            }
                            disabled={adding}
                            className="w-full rounded-lg border border-gray-300 bg-white px-4 py-2.5 text-sm text-gray-900 outline-none focus:border-blue-500 dark:border-gray-600 dark:bg-gray-800 dark:text-white"
                        >
                            <option value="RED">RED</option>
                            <option value="BLUE">BLUE</option>
                            <option value="WHITE">WHITE</option>
                        </select>
                    </div>

                    <div>
                        <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
                            Team (Optional)
                        </label>

                        <select
                            value={selectedTeamId}
                            onChange={(event) =>
                                setSelectedTeamId(event.target.value)
                            }
                            disabled={
                                adding ||
                                !selectedUserId ||
                                eligibleTeams.length === 0
                            }
                            className="w-full rounded-lg border border-gray-300 bg-white px-4 py-2.5 text-sm text-gray-900 outline-none focus:border-blue-500 disabled:bg-gray-100 dark:border-gray-600 dark:bg-gray-800 dark:text-white"
                        >
                            <option value="">
                                {!selectedUserId
                                    ? 'Select a Student first'
                                    : eligibleTeams.length === 0
                                        ? 'Student does not belong to any Team'
                                        : 'No Team'}
                            </option>

                            {eligibleTeams.map((team) => (
                                <option key={team.id} value={team.id}>
                                    {team.name}
                                </option>
                            ))}
                        </select>

                        <p className="mt-1.5 text-xs text-gray-500">
                            Only Teams that the selected Student belongs to are shown.
                        </p>
                    </div>
                </div>

                <div className="flex justify-end gap-3 border-t border-gray-200 px-6 py-4 dark:border-gray-700">
                    <button
                        type="button"
                        onClick={onClose}
                        disabled={adding}
                        className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 disabled:opacity-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-800"
                    >
                        Cancel
                    </button>

                    <button
                        type="button"
                        onClick={() => void handleAdd()}
                        disabled={
                            loading ||
                            adding ||
                            !selectedUserId ||
                            availableUsers.length === 0
                        }
                        className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-5 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                        <UserPlus className="h-4 w-4" />
                        {adding ? 'Adding...' : 'Add Participant'}
                    </button>
                </div>
            </div>
        </div>
    )
}

export default AddParticipantModal