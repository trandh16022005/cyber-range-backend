import { Eye, ClipboardList } from 'lucide-react'

import type { Exercise } from '../exercise.types'

interface ExerciseTableProps {
    exercises: Exercise[]
    loading: boolean
    onView: (exercise: Exercise) => void
}

const statusStyles: Record<string, string> = {
    CREATED: 'bg-slate-100 text-slate-700',
    READY: 'bg-blue-100 text-blue-700',
    RUNNING: 'bg-green-100 text-green-700',
    PAUSED: 'bg-amber-100 text-amber-700',
    STOPPED: 'bg-orange-100 text-orange-700',
    COMPLETED: 'bg-emerald-100 text-emerald-700',
    CANCELLED: 'bg-red-100 text-red-700',
}

function ExerciseTable({
                           exercises,
                           loading,
                           onView,
                       }: ExerciseTableProps) {
    if (loading) {
        return (
            <div className="py-12 text-center text-sm text-slate-500">
                Loading exercises...
            </div>
        )
    }

    if (exercises.length === 0) {
        return (
            <div className="py-12 text-center">
                <ClipboardList className="mx-auto mb-3 h-10 w-10 text-slate-300" />

                <p className="font-medium text-slate-700">
                    No exercises found
                </p>

                <p className="mt-1 text-sm text-slate-500">
                    Create an exercise or change your filters.
                </p>
            </div>
        )
    }

    return (
        <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
                <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase text-slate-500">
                <tr>
                    <th className="px-4 py-3">ID</th>
                    <th className="px-4 py-3">Exercise Name</th>
                    <th className="px-4 py-3">Scenario</th>
                    <th className="px-4 py-3">Cyber Range</th>
                    <th className="px-4 py-3">Status</th>
                    <th className="px-4 py-3">Created At</th>
                    <th className="px-4 py-3 text-right">Actions</th>
                </tr>
                </thead>

                <tbody className="divide-y divide-slate-100">
                {exercises.map((exercise) => (
                    <tr
                        key={exercise.id}
                        className="transition hover:bg-slate-50"
                    >
                        <td className="px-4 py-4 font-medium text-slate-600">
                            #{exercise.id}
                        </td>

                        <td className="px-4 py-4 font-medium text-slate-900">
                            {exercise.name}
                        </td>

                        <td className="px-4 py-4 text-slate-600">
                            {exercise.scenarioName}
                        </td>

                        <td className="px-4 py-4 text-slate-600">
                            {exercise.cyberRangeName}
                        </td>

                        <td className="px-4 py-4">
                                <span
                                    className={`inline-flex rounded-full px-2.5 py-1 text-xs font-medium ${
                                        statusStyles[exercise.status] ??
                                        'bg-slate-100 text-slate-700'
                                    }`}
                                >
                                    {exercise.status}
                                </span>
                        </td>

                        <td className="px-4 py-4 text-slate-500">
                            {new Date(
                                exercise.createdAt,
                            ).toLocaleDateString()}
                        </td>

                        <td className="px-4 py-4 text-right">
                            <button
                                type="button"
                                onClick={() => onView(exercise)}
                                className="inline-flex items-center gap-1.5 rounded-lg border border-slate-300 px-3 py-2 text-xs font-medium text-slate-700 transition hover:bg-slate-100"
                            >
                                <Eye className="h-3.5 w-3.5" />
                                View
                            </button>
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>
        </div>
    )
}

export default ExerciseTable
