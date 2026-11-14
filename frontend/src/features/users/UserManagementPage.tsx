import { useEffect, useState } from 'react'
import { Users } from 'lucide-react'

import { userService } from './userService'

import type {
    User,
    UserRole,
    UserStatus,
} from './user.types'

function UserManagementPage() {
    const [users, setUsers] = useState<User[]>([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)

    const [keyword, setKeyword] = useState('')
    const [role, setRole] = useState<UserRole | ''>('')
    const [status, setStatus] = useState<UserStatus | ''>('')

    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(0)
    const [totalElements, setTotalElements] = useState(0)

    const pageSize = 10

    useEffect(() => {
        const fetchUsers = async () => {
            try {
                setLoading(true)
                setError(null)

                const data = await userService.getUsers({
                    keyword,
                    role: role || undefined,
                    status: status || undefined,
                    page,
                    size: pageSize,
                })

                setUsers(data.content)
                setTotalPages(data.totalPages)
                setTotalElements(data.totalElements)
            } catch (err) {
                console.error('Failed to fetch users:', err)
                setError('Unable to load users.')
            } finally {
                setLoading(false)
            }
        }

        fetchUsers()
    }, [keyword, role, status, page])

    return (
        <div>
            <div className="mb-8">
                <h2 className="text-2xl font-bold text-slate-900">
                    User Management
                </h2>

                <p className="mt-1 text-sm text-slate-500">
                    Manage users and their system access.
                </p>
            </div>

            <div className="mb-6 flex items-center gap-3">
                <div className="rounded-lg bg-cyan-100 p-3">
                    <Users className="h-6 w-6 text-cyan-600" />
                </div>

                <div>
                    <p className="text-sm text-slate-500">
                        Total Users
                    </p>

                    <p className="text-2xl font-bold text-slate-900">
                        {loading ? '...' : totalElements}
                    </p>
                </div>
            </div>

            <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
                <div className="mb-6">
                    <h3 className="text-lg font-semibold text-slate-900">
                        Users
                    </h3>
                </div>

                {error && (
                    <div className="mb-4 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-600">
                        {error}
                    </div>
                )}

                <div className="mb-6 flex flex-col gap-3 md:flex-row">
                    <input
                        type="text"
                        placeholder="Search username, email or full name..."
                        value={keyword}
                        onChange={(event) => {
                            setKeyword(event.target.value)
                            setPage(0)
                        }}
                        className="min-w-0 flex-1 rounded-lg border border-slate-300 px-4 py-2 text-sm outline-none focus:border-cyan-500"
                    />

                    <select
                        value={role}
                        onChange={(event) => {
                            setRole(event.target.value as UserRole | '')
                            setPage(0)
                        }}
                        className="rounded-lg border border-slate-300 px-4 py-2 text-sm outline-none focus:border-cyan-500"
                    >
                        <option value="">All Roles</option>
                        <option value="ADMIN">Admin</option>
                        <option value="INSTRUCTOR">Instructor</option>
                        <option value="STUDENT">Student</option>
                    </select>

                    <select
                        value={status}
                        onChange={(event) => {
                            setStatus(event.target.value as UserStatus | '')
                            setPage(0)
                        }}
                        className="rounded-lg border border-slate-300 px-4 py-2 text-sm outline-none focus:border-cyan-500"
                    >
                        <option value="">All Statuses</option>
                        <option value="ACTIVE">Active</option>
                        <option value="INACTIVE">Inactive</option>
                        <option value="LOCKED">Locked</option>
                    </select>
                </div>

                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                        <thead className="border-b border-slate-200 bg-slate-50 text-slate-500">
                        <tr>
                            <th className="px-4 py-3 font-medium">ID</th>
                            <th className="px-4 py-3 font-medium">User</th>
                            <th className="px-4 py-3 font-medium">Email</th>
                            <th className="px-4 py-3 font-medium">Role</th>
                            <th className="px-4 py-3 font-medium">Status</th>
                        </tr>
                        </thead>

                        <tbody className="divide-y divide-slate-100">
                        {loading ? (
                            <tr>
                                <td
                                    colSpan={5}
                                    className="px-4 py-8 text-center text-slate-500"
                                >
                                    Loading users...
                                </td>
                            </tr>
                        ) : users.length === 0 ? (
                            <tr>
                                <td
                                    colSpan={5}
                                    className="px-4 py-8 text-center text-slate-500"
                                >
                                    No users found.
                                </td>
                            </tr>
                        ) : (
                            users.map((user) => (
                                <tr
                                    key={user.id}
                                    className="hover:bg-slate-50"
                                >
                                    <td className="px-4 py-4 text-slate-500">
                                        {user.id}
                                    </td>

                                    <td className="px-4 py-4">
                                        <p className="font-medium text-slate-900">
                                            {user.fullName}
                                        </p>
                                        <p className="text-xs text-slate-500">
                                            @{user.username}
                                        </p>
                                    </td>

                                    <td className="px-4 py-4 text-slate-600">
                                        {user.email}
                                    </td>

                                    <td className="px-4 py-4 text-slate-600">
                                        {user.role}
                                    </td>

                                    <td className="px-4 py-4">
                                            <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-700">
                                                {user.status}
                                            </span>
                                    </td>
                                </tr>
                            ))
                        )}
                        </tbody>
                    </table>
                </div>

                <div className="mt-6 flex flex-col items-center justify-between gap-4 border-t border-slate-200 pt-4 sm:flex-row">
                    <p className="text-sm text-slate-500">
                        Page {page + 1} of {Math.max(totalPages, 1)}
                        {' · '}
                        {totalElements} users
                    </p>

                    <div className="flex gap-2">
                        <button
                            type="button"
                            disabled={page === 0 || loading}
                            onClick={() => setPage((current) => current - 1)}
                            className="rounded-lg border border-slate-300 px-4 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-40"
                        >
                            Previous
                        </button>

                        <button
                            type="button"
                            disabled={page + 1 >= totalPages || loading}
                            onClick={() => setPage((current) => current + 1)}
                            className="rounded-lg border border-slate-300 px-4 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-40"
                        >
                            Next
                        </button>
                    </div>
                </div>
            </div>
        </div>
    )
}

export default UserManagementPage
