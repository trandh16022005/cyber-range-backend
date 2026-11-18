import {
    Users,
    Pencil,
    CalendarDays,
    Trash2,
} from 'lucide-react'

import type { Team } from '../team.types'

interface TeamTableProps {
    teams: Team[]
    loading: boolean
    error: string | null
    canDelete: boolean
    onEdit: (team: Team) => void
    onViewMembers: (team: Team) => void
    onDelete: (team: Team) => void
}

function TeamTable({
                       teams,
                       loading,
                       error,
                       canDelete,
                       onEdit,
                       onViewMembers,
                       onDelete,
                   }: TeamTableProps) {
    const formatDate = (date: string) => {
        return new Intl.DateTimeFormat('vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
        }).format(new Date(date))
    }

    if (loading) {
        return (
            <div className="rounded-xl border border-gray-200 bg-white p-10 text-center dark:border-gray-700 dark:bg-gray-900">
                <p className="text-sm text-gray-500">
                    Đang tải danh sách Team...
                </p>
            </div>
        )
    }

    if (error) {
        return (
            <div className="rounded-xl border border-red-200 bg-red-50 p-6 text-center dark:border-red-900 dark:bg-red-950/30">
                <p className="text-sm text-red-600 dark:text-red-400">
                    {error}
                </p>
            </div>
        )
    }

    if (teams.length === 0) {
        return (
            <div className="rounded-xl border border-dashed border-gray-300 bg-white p-12 text-center dark:border-gray-700 dark:bg-gray-900">
                <Users className="mx-auto mb-3 h-10 w-10 text-gray-400" />

                <h3 className="text-base font-semibold text-gray-900 dark:text-white">
                    Chưa có Team nào
                </h3>

                <p className="mt-1 text-sm text-gray-500">
                    Hãy tạo Team mới để bắt đầu quản lý thành viên.
                </p>
            </div>
        )
    }

    return (
        <div className="overflow-hidden rounded-xl border border-gray-200 bg-white dark:border-gray-700 dark:bg-gray-900">
            <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                    <thead className="bg-gray-50 text-xs uppercase text-gray-500 dark:bg-gray-800 dark:text-gray-400">
                    <tr>
                        <th className="px-6 py-4 font-semibold">
                            Team
                        </th>
                        <th className="px-6 py-4 font-semibold">
                            Mô tả
                        </th>
                        <th className="px-6 py-4 font-semibold">
                            Ngày tạo
                        </th>
                        <th className="px-6 py-4 text-right font-semibold">
                            Thao tác
                        </th>
                    </tr>
                    </thead>

                    <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                    {teams.map((team) => (
                        <tr
                            key={team.id}
                            className="transition-colors hover:bg-gray-50 dark:hover:bg-gray-800/50"
                        >
                            <td className="px-6 py-4">
                                <div className="font-semibold text-gray-900 dark:text-white">
                                    {team.name}
                                </div>

                                <div className="mt-1 text-xs text-gray-500">
                                    ID: {team.id}
                                </div>
                            </td>

                            <td className="max-w-xs px-6 py-4 text-gray-600 dark:text-gray-300">
                                    <span className="line-clamp-2">
                                        {team.description || 'Chưa có mô tả'}
                                    </span>
                            </td>

                            <td className="whitespace-nowrap px-6 py-4 text-gray-500 dark:text-gray-400">
                                <div className="flex items-center gap-2">
                                    <CalendarDays className="h-4 w-4" />
                                    {formatDate(team.createdAt)}
                                </div>
                            </td>

                            <td className="px-6 py-4">
                                <div className="flex justify-end gap-2">
                                    <button
                                        type="button"
                                        onClick={() => onViewMembers(team)}
                                        className="inline-flex items-center gap-2 rounded-lg border border-gray-200 px-3 py-2 text-xs font-medium text-gray-700 transition-colors hover:bg-gray-100 dark:border-gray-600 dark:text-gray-200 dark:hover:bg-gray-700"
                                    >
                                        <Users className="h-4 w-4" />
                                        Thành viên
                                    </button>

                                    <button
                                        type="button"
                                        onClick={() => onEdit(team)}
                                        className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-3 py-2 text-xs font-medium text-white transition-colors hover:bg-blue-700"
                                    >
                                        <Pencil className="h-4 w-4" />
                                        Sửa
                                    </button>

                                    {canDelete && (
                                        <button
                                            type="button"
                                            onClick={() => onDelete(team)}
                                            className="inline-flex items-center gap-2 rounded-lg bg-red-600 px-3 py-2 text-xs font-medium text-white transition-colors hover:bg-red-700"
                                        >
                                            <Trash2 className="h-4 w-4" />
                                            Xóa
                                        </button>
                                    )}
                                </div>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    )
}

export default TeamTable
