import { useEffect, useState } from 'react'
import { Plus, Users, X } from 'lucide-react'

import { userService } from './userService'

import type {
    CreateUserRequest,
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

    // Create User modal
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false)
    const [creating, setCreating] = useState(false)
    const [createError, setCreateError] = useState<string | null>(null)
    const [refreshKey, setRefreshKey] = useState(0)

    const [formData, setFormData] = useState<CreateUserRequest>({
        username: '',
        email: '',
        password: '',
        fullName: '',
        role: 'STUDENT',
    })

    // Fetch users
    useEffect(() => {
        const fetchUsers = async () => {
            try {
                setLoading(true)
                setError(null)

                const data = await userService.getUsers({
                    keyword: keyword.trim() || undefined,
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
    }, [keyword, role, status, page, refreshKey])

    // Get API error message
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

        return 'Unable to create user. Please try again.'
    }

    // Handle form input changes
    const handleFormChange = (
        field: keyof CreateUserRequest,
        value: string,
    ) => {
        setFormData((previous) => ({
            ...previous,
            [field]: value,
        }))
    }

    // Reset create form
    const resetCreateForm = () => {
        setFormData({
            username: '',
            email: '',
            password: '',
            fullName: '',
            role: 'STUDENT',
        })
        setCreateError(null)
    }

    // Open create modal
    const openCreateModal = () => {
        resetCreateForm()
        setIsCreateModalOpen(true)
    }

    // Close create modal
    const closeCreateModal = () => {
        if (creating) return

        setIsCreateModalOpen(false)
        resetCreateForm()
    }

    // Create user
    const handleCreateUser = async (
        event: React.FormEvent<HTMLFormElement>,
    ) => {
        event.preventDefault()

        try {
            setCreating(true)
            setCreateError(null)

            await userService.createUser(formData)

            setIsCreateModalOpen(false)
            resetCreateForm()

            // Return to first page and refresh user list
            setPage(0)
            setRefreshKey((previous) => previous + 1)
        } catch (err) {
            console.error('Failed to create user:', err)
            setCreateError(getApiErrorMessage(err))
        } finally {
            setCreating(false)
        }
    }

    return (
        <div>
            {/* Page heading */}
            <div className="mb-8">
                <h2 className="text-2xl font-bold text-slate-900">
                    User Management
                </h2>

                <p className="mt-1 text-sm text-slate-500">
                    Manage users and their system access.
                </p>
            </div>

            {/* Total users */}
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

            {/* Users section */}
            <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
                {/* Section heading */}
                <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                    <div>
                        <h3 className="text-lg font-semibold text-slate-900">
                            Users
                        </h3>

                        <p className="mt-1 text-sm text-slate-500">
                            View and manage system users.
                        </p>
                    </div>

                    <button
                        type="button"
                        onClick={openCreateModal}
                        className="inline-flex items-center justify-center gap-2 rounded-lg bg-cyan-600 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-cyan-700"
                    >
                        <Plus className="h-4 w-4" />
                        Create User
                    </button>
                </div>

                {/* Fetch error */}
                {error && (
                    <div className="mb-5 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">
                        {error}
                    </div>
                )}

                {/* Search and filters */}
                <div className="mb-6 flex flex-col gap-3 md:flex-row">
                    <input
                        type="text"
                        value={keyword}
                        onChange={(event) => {
                            setKeyword(event.target.value)
                            setPage(0)
                        }}
                        placeholder="Search username, email or name..."
                        className="min-w-0 flex-1 rounded-lg border border-slate-300 px-4 py-2.5 text-sm outline-none transition focus:border-cyan-500 focus:ring-2 focus:ring-cyan-100"
                    />

                    <select
                        value={role}
                        onChange={(event) => {
                            setRole(event.target.value as UserRole | '')
                            setPage(0)
                        }}
                        className="rounded-lg border border-slate-300 px-4 py-2.5 text-sm outline-none focus:border-cyan-500"
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
                        className="rounded-lg border border-slate-300 px-4 py-2.5 text-sm outline-none focus:border-cyan-500"
                    >
                        <option value="">All Statuses</option>
                        <option value="ACTIVE">Active</option>
                        <option value="INACTIVE">Inactive</option>
                        <option value="LOCKED">Locked</option>
                    </select>
                </div>

                {/* Users table */}
                <div className="overflow-x-auto">
                    <table className="w-full min-w-[700px] text-left text-sm">
                        <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase text-slate-500">
                        <tr>
                            <th className="px-4 py-3 font-semibold">
                                ID
                            </th>
                            <th className="px-4 py-3 font-semibold">
                                User
                            </th>
                            <th className="px-4 py-3 font-semibold">
                                Email
                            </th>
                            <th className="px-4 py-3 font-semibold">
                                Role
                            </th>
                            <th className="px-4 py-3 font-semibold">
                                Status
                            </th>
                        </tr>
                        </thead>

                        <tbody className="divide-y divide-slate-100">
                        {loading ? (
                            <tr>
                                <td
                                    colSpan={5}
                                    className="px-4 py-10 text-center text-slate-500"
                                >
                                    Loading users...
                                </td>
                            </tr>
                        ) : users.length === 0 ? (
                            <tr>
                                <td
                                    colSpan={5}
                                    className="px-4 py-10 text-center text-slate-500"
                                >
                                    No users found.
                                </td>
                            </tr>
                        ) : (
                            users.map((user) => (
                                <tr
                                    key={user.id}
                                    className="transition hover:bg-slate-50"
                                >
                                    <td className="px-4 py-4 text-slate-500">
                                        {user.id}
                                    </td>

                                    <td className="px-4 py-4">
                                        <div className="font-medium text-slate-900">
                                            {user.fullName}
                                        </div>

                                        <div className="mt-1 text-xs text-slate-500">
                                            {user.username}
                                        </div>
                                    </td>

                                    <td className="px-4 py-4 text-slate-600">
                                        {user.email}
                                    </td>

                                    <td className="px-4 py-4">
                                            <span className="inline-flex rounded-full bg-cyan-50 px-2.5 py-1 text-xs font-medium text-cyan-700">
                                                {user.role}
                                            </span>
                                    </td>

                                    <td className="px-4 py-4">
                                            <span
                                                className={`inline-flex rounded-full px-2.5 py-1 text-xs font-medium ${
                                                    user.status === 'ACTIVE'
                                                        ? 'bg-green-50 text-green-700'
                                                        : user.status === 'LOCKED'
                                                            ? 'bg-red-50 text-red-700'
                                                            : 'bg-slate-100 text-slate-600'
                                                }`}
                                            >
                                                {user.status}
                                            </span>
                                    </td>
                                </tr>
                            ))
                        )}
                        </tbody>
                    </table>
                </div>

                {/* Pagination */}
                <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                    <p className="text-sm text-slate-500">
                        Showing{' '}
                        {users.length > 0 ? page * pageSize + 1 : 0}
                        {' '}to{' '}
                        {page * pageSize + users.length}
                        {' '}of {totalElements} users
                    </p>

                    <div className="flex items-center gap-2">
                        <button
                            type="button"
                            onClick={() => setPage((previous) => previous - 1)}
                            disabled={page === 0 || loading}
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
                            onClick={() => setPage((previous) => previous + 1)}
                            disabled={
                                page + 1 >= totalPages || loading
                            }
                            className="rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-600 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
                        >
                            Next
                        </button>
                    </div>
                </div>
            </div>

            {/* Create User Modal */}
            {isCreateModalOpen && (
                <div
                    className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4"
                    onMouseDown={(event) => {
                        if (event.target === event.currentTarget) {
                            closeCreateModal()
                        }
                    }}
                >
                    <div
                        role="dialog"
                        aria-modal="true"
                        aria-labelledby="create-user-title"
                        className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-xl bg-white p-6 shadow-xl"
                    >
                        {/* Modal heading */}
                        <div className="mb-6 flex items-start justify-between">
                            <div>
                                <h3
                                    id="create-user-title"
                                    className="text-xl font-bold text-slate-900"
                                >
                                    Create User
                                </h3>

                                <p className="mt-1 text-sm text-slate-500">
                                    Add a new user to the Cyber Range system.
                                </p>
                            </div>

                            <button
                                type="button"
                                onClick={closeCreateModal}
                                disabled={creating}
                                aria-label="Close modal"
                                className="rounded-lg p-2 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700 disabled:opacity-40"
                            >
                                <X className="h-5 w-5" />
                            </button>
                        </div>

                        {/* Create error */}
                        {createError && (
                            <div className="mb-5 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">
                                {createError}
                            </div>
                        )}

                        {/* Create form */}
                        <form
                            onSubmit={handleCreateUser}
                            className="space-y-4"
                        >
                            <div>
                                <label
                                    htmlFor="create-username"
                                    className="mb-1.5 block text-sm font-medium text-slate-700"
                                >
                                    Username
                                </label>

                                <input
                                    id="create-username"
                                    type="text"
                                    required
                                    minLength={3}
                                    maxLength={100}
                                    autoComplete="username"
                                    value={formData.username}
                                    onChange={(event) =>
                                        handleFormChange(
                                            'username',
                                            event.target.value,
                                        )
                                    }
                                    placeholder="Enter username"
                                    className="w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm outline-none transition focus:border-cyan-500 focus:ring-2 focus:ring-cyan-100"
                                />
                            </div>

                            <div>
                                <label
                                    htmlFor="create-email"
                                    className="mb-1.5 block text-sm font-medium text-slate-700"
                                >
                                    Email
                                </label>

                                <input
                                    id="create-email"
                                    type="email"
                                    required
                                    maxLength={255}
                                    autoComplete="email"
                                    value={formData.email}
                                    onChange={(event) =>
                                        handleFormChange(
                                            'email',
                                            event.target.value,
                                        )
                                    }
                                    placeholder="Enter email address"
                                    className="w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm outline-none transition focus:border-cyan-500 focus:ring-2 focus:ring-cyan-100"
                                />
                            </div>

                            <div>
                                <label
                                    htmlFor="create-password"
                                    className="mb-1.5 block text-sm font-medium text-slate-700"
                                >
                                    Password
                                </label>

                                <input
                                    id="create-password"
                                    type="password"
                                    required
                                    minLength={8}
                                    autoComplete="new-password"
                                    value={formData.password}
                                    onChange={(event) =>
                                        handleFormChange(
                                            'password',
                                            event.target.value,
                                        )
                                    }
                                    placeholder="Enter password (at least 8 characters)"
                                    className="w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm outline-none transition focus:border-cyan-500 focus:ring-2 focus:ring-cyan-100"
                                />
                            </div>

                            <div>
                                <label
                                    htmlFor="create-full-name"
                                    className="mb-1.5 block text-sm font-medium text-slate-700"
                                >
                                    Full Name
                                </label>

                                <input
                                    id="create-full-name"
                                    type="text"
                                    required
                                    maxLength={255}
                                    autoComplete="name"
                                    value={formData.fullName}
                                    onChange={(event) =>
                                        handleFormChange(
                                            'fullName',
                                            event.target.value,
                                        )
                                    }
                                    placeholder="Enter full name"
                                    className="w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm outline-none transition focus:border-cyan-500 focus:ring-2 focus:ring-cyan-100"
                                />
                            </div>

                            <div>
                                <label
                                    htmlFor="create-role"
                                    className="mb-1.5 block text-sm font-medium text-slate-700"
                                >
                                    Role
                                </label>

                                <select
                                    id="create-role"
                                    required
                                    value={formData.role}
                                    onChange={(event) =>
                                        handleFormChange(
                                            'role',
                                            event.target.value,
                                        )
                                    }
                                    className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-sm outline-none transition focus:border-cyan-500 focus:ring-2 focus:ring-cyan-100"
                                >
                                    <option value="STUDENT">Student</option>
                                    <option value="INSTRUCTOR">
                                        Instructor
                                    </option>
                                </select>
                            </div>

                            {/* Form actions */}
                            <div className="flex justify-end gap-3 border-t border-slate-100 pt-5">
                                <button
                                    type="button"
                                    onClick={closeCreateModal}
                                    disabled={creating}
                                    className="rounded-lg border border-slate-300 px-4 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                                >
                                    Cancel
                                </button>

                                <button
                                    type="submit"
                                    disabled={creating}
                                    className="inline-flex items-center justify-center gap-2 rounded-lg bg-cyan-600 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-cyan-700 disabled:cursor-not-allowed disabled:opacity-60"
                                >
                                    {creating ? 'Creating...' : 'Create User'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    )
}

export default UserManagementPage
