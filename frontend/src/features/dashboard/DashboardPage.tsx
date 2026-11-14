
import { useEffect, useState } from 'react'
import { Activity, ShieldCheck, Users } from 'lucide-react'

import {
    dashboardService,
    type DashboardStatistics,
} from './dashboardService'

function DashboardPage() {
    const [statistics, setStatistics] = useState<DashboardStatistics | null>(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        const fetchStatistics = async () => {
            try {
                setLoading(true)
                setError(null)

                const data = await dashboardService.getStatistics()
                setStatistics(data)
            } catch (err) {
                console.error('Failed to fetch dashboard statistics:', err)
                setError('Unable to load dashboard statistics.')
            } finally {
                setLoading(false)
            }
        }

        fetchStatistics()
    }, [])

    const statisticCards = [
        {
            label: 'Total Users',
            value: statistics?.totalUsers,
            icon: Users,
        },
        {
            label: 'Active Exercises',
            value: statistics?.activeExercises,
            icon: Activity,
        },
        {
            label: 'Completed Assessments',
            value: statistics?.completedAssessments,
            icon: ShieldCheck,
        },
    ]

    return (
        <div>
            <div className="mb-8">
                <h2 className="text-2xl font-bold text-slate-900">
                    Dashboard
                </h2>
                <p className="mt-1 text-sm text-slate-500">
                    Overview of your Cyber Range environment.
                </p>
            </div>

            {error && (
                <div className="mb-6 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-600">
                    {error}
                </div>
            )}

            <div className="grid gap-6 md:grid-cols-3">
                {statisticCards.map(({ label, value, icon: Icon }) => (
                    <div
                        key={label}
                        className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm"
                    >
                        <div className="flex items-center justify-between">
                            <p className="text-sm text-slate-500">{label}</p>
                            <Icon className="h-6 w-6 text-cyan-600" />
                        </div>

                        <p className="mt-4 text-3xl font-bold text-slate-900">
                            {loading ? '...' : (value ?? 0)}
                        </p>
                    </div>
                ))}
            </div>
        </div>
    )
}

export default DashboardPage
