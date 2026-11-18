import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { X } from 'lucide-react'

import type {
    CreateTeamRequest,
    Team,
    UpdateTeamRequest,
} from '../team.types'

interface TeamFormModalProps {
    open: boolean
    team: Team | null
    saving: boolean
    error: string | null
    onClose: () => void
    onSubmit: (
        data: CreateTeamRequest | UpdateTeamRequest,
    ) => void
}

function TeamFormModal({
                           open,
                           team,
                           saving,
                           error,
                           onClose,
                           onSubmit,
                       }: TeamFormModalProps) {
    const [name, setName] = useState('')
    const [description, setDescription] = useState('')

    const isEditing = team !== null

    // Reset form when opening or switching between teams
    useEffect(() => {
        if (open) {
            setName(team?.name ?? '')
            setDescription(team?.description ?? '')
        }
    }, [open, team])

    if (!open) {
        return null
    }

    const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault()

        const trimmedName = name.trim()
        const trimmedDescription = description.trim()

        if (!trimmedName || trimmedName.length > 100) {
            return
        }

        if (isEditing) {
            onSubmit({
                name: trimmedName,
                description: trimmedDescription,
            })
        } else {
            onSubmit({
                name: trimmedName,
                description: trimmedDescription || null,
            })
        }
    }

    return (
        <div
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
            onMouseDown={(event) => {
                if (
                    event.target === event.currentTarget &&
                    !saving
                ) {
                    onClose()
                }
            }}
        >
            <div className="w-full max-w-lg rounded-xl bg-white shadow-xl dark:bg-gray-900">
                {/* Header */}
                <div className="flex items-center justify-between border-b border-gray-200 px-6 py-4 dark:border-gray-700">
                    <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                        {isEditing ? 'Chỉnh sửa Team' : 'Tạo Team mới'}
                    </h2>

                    <button
                        type="button"
                        onClick={onClose}
                        disabled={saving}
                        className="rounded-lg p-2 text-gray-500 transition hover:bg-gray-100 disabled:opacity-50 dark:hover:bg-gray-800"
                        aria-label="Đóng"
                    >
                        <X className="h-5 w-5" />
                    </button>
                </div>

                {/* Form */}
                <form onSubmit={handleSubmit}>
                    <div className="space-y-5 px-6 py-5">
                        {/* Backend error */}
                        {error && (
                            <div
                                role="alert"
                                className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600 dark:border-red-900 dark:bg-red-950/30 dark:text-red-400"
                            >
                                {error}
                            </div>
                        )}

                        {/* Team name */}
                        <div>
                            <label
                                htmlFor="team-name"
                                className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300"
                            >
                                Tên Team{' '}
                                <span className="text-red-500">*</span>
                            </label>

                            <input
                                id="team-name"
                                type="text"
                                value={name}
                                onChange={(event) =>
                                    setName(event.target.value)
                                }
                                maxLength={100}
                                required
                                disabled={saving}
                                placeholder="Nhập tên Team"
                                className="w-full rounded-lg border border-gray-300 bg-white px-4 py-2.5 text-sm text-gray-900 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100 disabled:bg-gray-100 dark:border-gray-600 dark:bg-gray-800 dark:text-white dark:disabled:bg-gray-700"
                            />

                            <p className="mt-1 text-right text-xs text-gray-400">
                                {name.length}/100 ký tự
                            </p>
                        </div>

                        {/* Team description */}
                        <div>
                            <label
                                htmlFor="team-description"
                                className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300"
                            >
                                Mô tả
                            </label>

                            <textarea
                                id="team-description"
                                value={description}
                                onChange={(event) =>
                                    setDescription(event.target.value)
                                }
                                rows={4}
                                disabled={saving}
                                placeholder="Nhập mô tả cho Team (không bắt buộc)"
                                className="w-full resize-none rounded-lg border border-gray-300 bg-white px-4 py-3 text-sm text-gray-900 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100 disabled:bg-gray-100 dark:border-gray-600 dark:bg-gray-800 dark:text-white dark:disabled:bg-gray-700"
                            />
                        </div>
                    </div>

                    {/* Footer */}
                    <div className="flex justify-end gap-3 border-t border-gray-200 px-6 py-4 dark:border-gray-700">
                        <button
                            type="button"
                            onClick={onClose}
                            disabled={saving}
                            className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 transition hover:bg-gray-100 disabled:opacity-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-800"
                        >
                            Hủy
                        </button>

                        <button
                            type="submit"
                            disabled={
                                saving ||
                                !name.trim() ||
                                name.trim().length > 100
                            }
                            className="rounded-lg bg-blue-600 px-5 py-2 text-sm font-medium text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                        >
                            {saving
                                ? 'Đang lưu...'
                                : isEditing
                                    ? 'Lưu thay đổi'
                                    : 'Tạo Team'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    )
}

export default TeamFormModal
