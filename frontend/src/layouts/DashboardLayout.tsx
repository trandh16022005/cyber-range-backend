import { NavLink, Outlet } from 'react-router'
import {
    Activity,
    ClipboardList,
    LayoutDashboard,
    Users,
    UsersRound,
} from 'lucide-react'

import { useAuth } from '../features/auth/AuthContext'

function DashboardLayout() {
    const { role } = useAuth()

    const navigation = [
        {
            name: 'Dashboard',
            path: '/dashboard',
            icon: LayoutDashboard,
            roles: ['ADMIN', 'INSTRUCTOR', 'STUDENT'],
        },
        {
            name: 'Users',
            path: '/users',
            icon: Users,
            roles: ['ADMIN'],
        },
        {
            name: 'Teams',
            path: '/teams',
            icon: UsersRound,
            roles: ['ADMIN', 'INSTRUCTOR'],
        },
        {
            name: 'Exercises',
            path: '/exercises',
            icon: ClipboardList,
            roles: ['ADMIN', 'INSTRUCTOR'],
        },
        {
            name: 'Assessments',
            path: '/assessments',
            icon: ClipboardList,
            roles: ['ADMIN', 'INSTRUCTOR', 'STUDENT'],
        },
    ]

    const visibleNavigation = navigation.filter((item) =>
        item.roles.includes(role ?? ''),
    )

    return (
        <div className="flex min-h-screen bg-slate-100">
            {/* Sidebar */}
            <aside className="flex w-64 flex-col bg-slate-900 text-white">
                <div className="flex items-center gap-3 border-b border-slate-700 px-6 py-5">
                    <Activity className="h-7 w-7 text-cyan-400" />
                    <span className="text-lg font-bold">
                        Cyber Range
                    </span>
                </div>

                <nav className="flex-1 space-y-2 p-4">
                    {visibleNavigation.map(
                        ({ name, path, icon: Icon }) => (
                            <NavLink
                                key={path}
                                to={path}
                                className={({ isActive }) =>
                                    `flex items-center gap-3 rounded-lg px-4 py-3 text-sm transition ${
                                        isActive
                                            ? 'bg-cyan-600 text-white'
                                            : 'text-slate-300 hover:bg-slate-800 hover:text-white'
                                    }`
                                }
                            >
                                <Icon size={20} />
                                {name}
                            </NavLink>
                        ),
                    )}
                </nav>

                <div className="border-t border-slate-700 px-6 py-4 text-xs text-slate-400">
                    Cyber Range Management System
                </div>
            </aside>

            {/* Main content */}
            <div className="min-w-0 flex-1">
                <header className="flex h-16 items-center justify-between border-b border-slate-200 bg-white px-8">
                    <h1 className="font-semibold text-slate-800">
                        Management Console
                    </h1>

                    <span className="text-sm text-slate-500">
                        {role ?? 'Guest'}
                    </span>
                </header>

                <main className="p-8">
                    <Outlet />
                </main>
            </div>
        </div>
    )
}

export default DashboardLayout
