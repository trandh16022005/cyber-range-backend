import { useEffect, useMemo, useState } from 'react'
import { UserPlus, UserMinus, X, Users } from 'lucide-react'

import { teamService } from '../teamService'
import { userService } from '../../users/userService'

import type { Team, TeamMember } from '../team.types'
import type { User } from '../../users/user.types'

interface TeamMembersModalProps {
    open: boolean
    team: Team | null
    onClose: () => void
}

function TeamMembersModal({
                              open,
                              team,
                              onClose,
                          }: TeamMembersModalProps) {
    const [members, setMembers] = useState<TeamMember[]>([])
    const [availableUsers, setAvailableUsers] = useState<User[]>([])
    const [selectedUserId, setSelectedUserId] = useState('')

    const [loading, setLoading] = useState(false)
    const [adding, setAdding] = useState(false)
    const [removingUserId, setRemovingUserId] = useState<number | null>(null)

    const [error, setError] = useState<string | null>(null)
    const [success, setSuccess] = useState<string | null>(null)

    const availableStudents = useMemo(() => {
        const memberIds = new Set(members.map((member) => member.userId))

        return availableUsers.filter(
            (user) => !memberIds.has(user.id),
        )
    }, [members, availableUsers])
    useEffect(() => {
        console.log('Team Members Debug:', {
            loading,
            adding,
            selectedUserId,
            availableUsers: availableUsers.length,
            availableStudents: availableStudents.length,
            isAddDisabled: loading || adding || !selectedUserId,
        })
    }, [
        loading,
        adding,
        selectedUserId,
        availableUsers,
        availableStudents,
    ])

    const loadData = async (teamId: number) => {
        setLoading(true)
        setError(null)
        setSuccess(null)

        try {
            const [memberData, userData] = await Promise.all([
                teamService.getTeamMembers(teamId),
                userService.getUsers({
                    role: 'STUDENT',
                    status: 'ACTIVE',
                    page: 0,
                    size: 100,
                }),
            ])

            setMembers(memberData)
            setAvailableUsers(userData.content)
            setSelectedUserId('')
        } catch {
            setError('Không thể tải dữ liệu thành viên. Vui lòng thử lại.')
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        if (open && team) {
            void loadData(team.id)
        } else {
            setMembers([])
            setAvailableUsers([])
            setSelectedUserId('')
            setError(null)
            setSuccess(null)
        }
    }, [open, team?.id])

    if (!open || !team) {
        return null
    }

    const handleAddMember = async () => {
        if (!selectedUserId) {
            setError('Vui lòng chọn Student cần thêm.')
            return
        }

        setAdding(true)
        setError(null)
        setSuccess(null)

        try {
            const newMember = await teamService.addTeamMember(
                team.id,
                { userId: Number(selectedUserId) },
            )

            setMembers((current) => [...current, newMember])
            setSelectedUserId('')
            setSuccess('Đã thêm thành viên vào Team thành công.')
        } catch {
            setError('Không thể thêm thành viên. Vui lòng kiểm tra lại.')
        } finally {
            setAdding(false)
        }
    }

    const handleRemoveMember = async (userId: number) => {
        const confirmed = window.confirm(
            'Bạn có chắc chắn muốn xóa thành viên này khỏi Team?',
        )

        if (!confirmed) {
            return
        }

        setRemovingUserId(userId)
        setError(null)
        setSuccess(null)

        try {
            await teamService.removeTeamMember(team.id, userId)

            setMembers((current) =>
                current.filter((member) => member.userId !== userId),
            )

            setSuccess('Đã xóa thành viên khỏi Team thành công.')
        } catch {
            setError('Không thể xóa thành viên. Vui lòng thử lại.')
        } finally {
            setRemovingUserId(null)
        }
    }

    return (
        <div
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
            onMouseDown={(event) => {
                if (event.target === event.currentTarget) {
                    onClose()
                }
            }}
        >
            <div className="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-xl bg-white shadow-xl dark:bg-gray-900">
                {/* Header */}
                <div className="flex items-center justify-between border-b border-gray-200 px-6 py-4 dark:border-gray-700">
                    <div>
                        <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                            Quản lý thành viên
                        </h2>

                        <p className="mt-1 text-sm text-gray-500">
                            Team: {team.name}
                        </p>
                    </div>

                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-lg p-2 text-gray-500 transition hover:bg-gray-100 dark:hover:bg-gray-800"
                        aria-label="Đóng"
                    >
                        <X className="h-5 w-5" />
                    </button>
                </div>

                <div className="space-y-6 px-6 py-5">
                    {/* Notifications */}
                    {error && (
                        <div
                            role="alert"
                            className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600 dark:border-red-900 dark:bg-red-950/30 dark:text-red-400"
                        >
                            {error}
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

                    {/* Add member */}
                    <section>
                        <h3 className="mb-3 text-sm font-semibold text-gray-900 dark:text-white">
                            Thêm thành viên
                        </h3>

                        <div className="flex flex-col gap-3 sm:flex-row">
                            <select
                                value={selectedUserId}
                                onChange={(event) =>
                                    setSelectedUserId(event.target.value)
                                }
                                disabled={
                                    loading ||
                                    adding ||
                                    availableStudents.length === 0
                                }
                                className="min-w-0 flex-1 rounded-lg border border-gray-300 bg-white px-4 py-2.5 text-sm text-gray-900 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 disabled:bg-gray-100 dark:border-gray-600 dark:bg-gray-800 dark:text-white dark:disabled:bg-gray-700"
                            >
                                <option value="">
                                    {loading
                                        ? 'Đang tải danh sách Student...'
                                        : availableStudents.length === 0
                                            ? 'Không có Student khả dụng'
                                            : 'Chọn Student để thêm'}
                                </option>

                                {availableStudents.map((user) => (
                                    <option
                                        key={user.id}
                                        value={user.id}
                                    >
                                        {user.fullName} ({user.username})
                                    </option>
                                ))}
                            </select>

                            <button
                                type="button"
                                onClick={handleAddMember}
                                disabled={
                                    loading ||
                                    adding ||
                                    !selectedUserId
                                }
                                className="inline-flex items-center justify-center gap-2 rounded-lg bg-blue-600 px-5 py-2.5 text-sm font-medium text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                            >
                                <UserPlus className="h-4 w-4" />
                                {adding ? 'Đang thêm...' : 'Thêm'}
                            </button>
                        </div>
                    </section>

                    {/* Member list */}
                    <section>
                        <div className="mb-3 flex items-center justify-between">
                            <h3 className="text-sm font-semibold text-gray-900 dark:text-white">
                                Danh sách thành viên
                            </h3>

                            <span className="rounded-full bg-blue-100 px-3 py-1 text-xs font-medium text-blue-700 dark:bg-blue-900/40 dark:text-blue-300">
                                {members.length} thành viên
                            </span>
                        </div>

                        {loading ? (
                            <div className="rounded-lg border border-gray-200 p-8 text-center dark:border-gray-700">
                                <p className="text-sm text-gray-500">
                                    Đang tải danh sách thành viên...
                                </p>
                            </div>
                        ) : members.length === 0 ? (
                            <div className="rounded-lg border border-dashed border-gray-300 p-8 text-center dark:border-gray-700">
                                <Users className="mx-auto mb-2 h-8 w-8 text-gray-400" />

                                <p className="text-sm text-gray-500">
                                    Team chưa có thành viên nào.
                                </p>
                            </div>
                        ) : (
                            <div className="overflow-hidden rounded-lg border border-gray-200 dark:border-gray-700">
                                <div className="overflow-x-auto">
                                    <table className="w-full text-left text-sm">
                                        <thead className="bg-gray-50 text-xs uppercase text-gray-500 dark:bg-gray-800 dark:text-gray-400">
                                        <tr>
                                            <th className="px-4 py-3">
                                                Thành viên
                                            </th>
                                            <th className="px-4 py-3">
                                                Email
                                            </th>
                                            <th className="px-4 py-3">
                                                Vai trò
                                            </th>
                                            <th className="px-4 py-3 text-right">
                                                Thao tác
                                            </th>
                                        </tr>
                                        </thead>

                                        <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                                        {members.map((member) => (
                                            <tr key={member.userId}>
                                                <td className="px-4 py-3">
                                                    <div className="font-medium text-gray-900 dark:text-white">
                                                        {member.fullName}
                                                    </div>

                                                    <div className="mt-1 text-xs text-gray-500">
                                                        {member.username}
                                                    </div>
                                                </td>

                                                <td className="px-4 py-3 text-gray-600 dark:text-gray-300">
                                                    {member.email}
                                                </td>

                                                <td className="px-4 py-3">
                                                        <span className="rounded-full bg-gray-100 px-2.5 py-1 text-xs text-gray-700 dark:bg-gray-800 dark:text-gray-300">
                                                            {member.role}
                                                        </span>
                                                </td>

                                                <td className="px-4 py-3 text-right">
                                                    <button
                                                        type="button"
                                                        onClick={() =>
                                                            void handleRemoveMember(
                                                                member.userId,
                                                            )
                                                        }
                                                        disabled={
                                                            removingUserId === member.userId
                                                        }
                                                        className="inline-flex items-center gap-1 rounded-lg px-3 py-2 text-xs font-medium text-red-600 transition hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50 dark:text-red-400 dark:hover:bg-red-950/30"
                                                    >
                                                        <UserMinus className="h-4 w-4" />
                                                        {removingUserId === member.userId
                                                            ? 'Đang xóa...'
                                                            : 'Xóa'}
                                                    </button>
                                                </td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        )}
                    </section>
                </div>

                {/* Footer */}
                <div className="flex justify-end border-t border-gray-200 px-6 py-4 dark:border-gray-700">
                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-lg border border-gray-300 px-5 py-2 text-sm font-medium text-gray-700 transition hover:bg-gray-100 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-800"
                    >
                        Đóng
                    </button>
                </div>
            </div>
        </div>
    )
}

export default TeamMembersModal
