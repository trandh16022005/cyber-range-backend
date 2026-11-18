import { useEffect, useState } from 'react'
import { Plus, Users, RefreshCw } from 'lucide-react'

import { useAuth } from '../auth/AuthContext'
import { teamService } from './teamService'

import TeamTable from './components/TeamTable'
import TeamFormModal from './components/TeamFormModal'
import TeamMembersModal from './components/TeamMembersModal'

import type {
    CreateTeamRequest,
    PageResponse,
    Team,
    UpdateTeamRequest,
} from './team.types'

function TeamManagementPage() {
    const { role } = useAuth()
    const canDelete = role === 'ADMIN'

    const [teams, setTeams] = useState<Team[]>([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)

    // Pagination
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(0)
    const [totalElements, setTotalElements] = useState(0)

    const pageSize = 10

    // Create / Edit Team modal
    const [isFormModalOpen, setIsFormModalOpen] = useState(false)
    const [selectedTeam, setSelectedTeam] = useState<Team | null>(null)
    const [saving, setSaving] = useState(false)
    const [formError, setFormError] = useState<string | null>(null)

    // Team members modal
    const [isMembersModalOpen, setIsMembersModalOpen] = useState(false)
    const [membersTeam, setMembersTeam] = useState<Team | null>(null)

    // Delete Team
    const [deletingTeamId, setDeletingTeamId] = useState<number | null>(null)
    const [deleteError, setDeleteError] = useState<string | null>(null)

    // Refresh list after create / update / delete
    const [refreshKey, setRefreshKey] = useState(0)

    // Fetch teams
    useEffect(() => {
        const fetchTeams = async () => {
            try {
                setLoading(true)
                setError(null)

                const data: PageResponse<Team> =
                    await teamService.getTeams({
                        page,
                        size: pageSize,
                    })

                setTeams(data.content)
                setTotalPages(data.totalPages)
                setTotalElements(data.totalElements)
            } catch (err) {
                console.error('Failed to fetch teams:', err)
                setError('Không thể tải danh sách Team. Vui lòng thử lại.')
            } finally {
                setLoading(false)
            }
        }

        void fetchTeams()
    }, [page, refreshKey])

    // Extract API error message
    const getApiErrorMessage = (err: unknown): string => {
        if (
            typeof err === 'object' &&
            err !== null &&
            'response' in err
        ) {
            const response = (
                err as {
                    response?: {
                        data?: {
                            message?: string
                        }
                    }
                }
            ).response

            if (response?.data?.message) {
                return response.data.message
            }
        }

        return 'Không thể thực hiện thao tác. Vui lòng thử lại.'
    }

    // Open create modal
    const openCreateModal = () => {
        setSelectedTeam(null)
        setFormError(null)
        setIsFormModalOpen(true)
    }

    // Open edit modal
    const openEditModal = (team: Team) => {
        setSelectedTeam(team)
        setFormError(null)
        setIsFormModalOpen(true)
    }

    // Close create / edit modal
    const closeFormModal = () => {
        if (saving) return

        setIsFormModalOpen(false)
        setSelectedTeam(null)
        setFormError(null)
    }

    // Create or update Team
    const handleSaveTeam = async (
        data: CreateTeamRequest | UpdateTeamRequest,
    ) => {
        try {
            setSaving(true)
            setFormError(null)

            if (selectedTeam) {
                await teamService.updateTeam(
                    selectedTeam.id,
                    data as UpdateTeamRequest,
                )
            } else {
                await teamService.createTeam(
                    data as CreateTeamRequest,
                )
            }

            setIsFormModalOpen(false)
            setSelectedTeam(null)

            // Return to first page and refresh
            setPage(0)
            setRefreshKey((previous) => previous + 1)
        } catch (err) {
            console.error('Failed to save team:', err)
            setFormError(getApiErrorMessage(err))
        } finally {
            setSaving(false)
        }
    }

    // Delete Team (ADMIN only)
    const handleDeleteTeam = async (team: Team) => {
        if (!canDelete || deletingTeamId !== null) return

        const confirmed = window.confirm(
            `Bạn có chắc chắn muốn xóa Team "${team.name}" không?\n\nThành viên thuộc Team này sẽ bị xóa khỏi danh sách thành viên của Team.`,
        )

        if (!confirmed) return

        try {
            setDeletingTeamId(team.id)
            setDeleteError(null)

            await teamService.deleteTeam(team.id)

            // If this was the last Team on the current page,
            // move back one page.
            if (teams.length === 1 && page > 0) {
                setPage((previous) => previous - 1)
            } else {
                setRefreshKey((previous) => previous + 1)
            }
        } catch (err) {
            console.error('Failed to delete team:', err)
            setDeleteError(getApiErrorMessage(err))
        } finally {
            setDeletingTeamId(null)
        }
    }

    // Open Team members modal
    const openMembersModal = (team: Team) => {
        setMembersTeam(team)
        setIsMembersModalOpen(true)
    }

    // Close Team members modal
    const closeMembersModal = () => {
        setIsMembersModalOpen(false)
        setMembersTeam(null)
    }

    // Refresh Team list manually
    const refreshTeams = () => {
        setDeleteError(null)
        setRefreshKey((previous) => previous + 1)
    }

    return (
        <div>
            {/* Page heading */}
            <div className="mb-8">
                <h2 className="text-2xl font-bold text-slate-900">
                    Team Management
                </h2>

                <p className="mt-1 text-sm text-slate-500">
                    Create teams and manage team members.
                </p>
            </div>

            {/* Total teams */}
            <div className="mb-6 flex items-center gap-3">
                <div className="rounded-lg bg-cyan-100 p-3">
                    <Users className="h-6 w-6 text-cyan-600" />
                </div>

                <div>
                    <p className="text-sm text-slate-500">
                        Total Teams
                    </p>

                    <p className="text-2xl font-bold text-slate-900">
                        {loading ? '...' : totalElements}
                    </p>
                </div>
            </div>

            {/* Teams section */}
            <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
                {/* Section heading */}
                <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                    <div>
                        <h3 className="text-lg font-semibold text-slate-900">
                            Teams
                        </h3>

                        <p className="mt-1 text-sm text-slate-500">
                            View and manage teams in the Cyber Range.
                        </p>
                    </div>

                    <div className="flex flex-wrap gap-2">
                        <button
                            type="button"
                            onClick={refreshTeams}
                            disabled={loading || deletingTeamId !== null}
                            className="inline-flex items-center justify-center gap-2 rounded-lg border border-slate-300 px-4 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                        >
                            <RefreshCw className="h-4 w-4" />
                            Refresh
                        </button>

                        <button
                            type="button"
                            onClick={openCreateModal}
                            className="inline-flex items-center justify-center gap-2 rounded-lg bg-cyan-600 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-cyan-700"
                        >
                            <Plus className="h-4 w-4" />
                            Create Team
                        </button>
                    </div>
                </div>

                {/* Fetch error */}
                {error && (
                    <div
                        role="alert"
                        className="mb-5 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600"
                    >
                        {error}
                    </div>
                )}

                {/* Delete error */}
                {deleteError && (
                    <div
                        role="alert"
                        className="mb-5 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600"
                    >
                        {deleteError}
                    </div>
                )}

                {/* Teams table */}
                <TeamTable
                    teams={teams}
                    loading={loading}
                    error={null}
                    canDelete={canDelete}
                    onEdit={openEditModal}
                    onViewMembers={openMembersModal}
                    onDelete={handleDeleteTeam}
                />

                {/* Pagination */}
                {!loading && !error && (
                    <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                        <p className="text-sm text-slate-500">
                            Showing{' '}
                            {teams.length > 0
                                ? page * pageSize + 1
                                : 0}
                            {' '}to{' '}
                            {page * pageSize + teams.length}
                            {' '}of {totalElements} teams
                        </p>

                        <div className="flex items-center gap-2">
                            <button
                                type="button"
                                onClick={() =>
                                    setPage((previous) => previous - 1)
                                }
                                disabled={page === 0}
                                className="rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-600 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
                            >
                                Previous
                            </button>

                            <span className="px-2 text-sm text-slate-600">
                                Page {totalPages === 0 ? 0 : page + 1} of{' '}
                                {totalPages}
                            </span>

                            <button
                                type="button"
                                onClick={() =>
                                    setPage((previous) => previous + 1)
                                }
                                disabled={page + 1 >= totalPages}
                                className="rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-600 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
                            >
                                Next
                            </button>
                        </div>
                    </div>
                )}
            </div>

            {/* Create / Edit Team Modal */}
            <TeamFormModal
                open={isFormModalOpen}
                team={selectedTeam}
                saving={saving}
                error={formError}
                onClose={closeFormModal}
                onSubmit={handleSaveTeam}
            />

            {/* Team Members Modal */}
            <TeamMembersModal
                open={isMembersModalOpen}
                team={membersTeam}
                onClose={closeMembersModal}
            />
        </div>
    )
}

export default TeamManagementPage
