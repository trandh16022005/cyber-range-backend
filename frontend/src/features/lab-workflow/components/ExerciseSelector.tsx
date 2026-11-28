import { useEffect, useState } from 'react'
import { CheckCircle2, ChevronDown, Loader2, RefreshCw } from 'lucide-react'

import { getExercises } from '../../exercises/exerciseService'
import type {
    Exercise,
    ExerciseStatus,
} from '../../exercises/exercise.types'

interface ExerciseSelectorProps {
    selectedExercise: Exercise | null
    onSelect: (exercise: Exercise) => void
}

const ELIGIBLE_STATUSES: ExerciseStatus[] = [
    'CREATED',
    'READY',
    'RUNNING',
    'PAUSED',
    'STOPPED',
    'COMPLETED',
    'CANCELLED',
]

function getStatusLabel(status: ExerciseStatus): string {
    const labels: Record<ExerciseStatus, string> = {
        CREATED: 'Mới tạo',
        READY: 'Sẵn sàng',
        RUNNING: 'Đang chạy',
        PAUSED: 'Tạm dừng',
        STOPPED: 'Đã dừng',
        COMPLETED: 'Hoàn thành',
        CANCELLED: 'Đã hủy',
    }

    return labels[status]
}

function ExerciseSelector({
                              selectedExercise,
                              onSelect,
                          }: ExerciseSelectorProps) {
    const [exercises, setExercises] = useState<Exercise[]>([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)

    const loadExercises = async () => {
        try {
            setLoading(true)
            setError(null)

            const response = await getExercises({
                page: 0,
                size: 100,
            })

            const eligibleExercises = response.content.filter(
                (exercise) =>
                    ELIGIBLE_STATUSES.includes(exercise.status),
            )

            setExercises(eligibleExercises)
        } catch {
            setError(
                'Không thể tải danh sách Exercise. Vui lòng kiểm tra kết nối Backend.',
            )
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        void loadExercises()
    }, [])

    return (
        <section className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="mb-5">
                <h2 className="text-lg font-semibold text-slate-800">
                    1. Chọn Exercise
                </h2>

                <p className="mt-1 text-sm text-slate-500">
                    Chọn Exercise để điều phối bài thực hành hoặc xem lịch sử.
                </p>
            </div>

            {loading ? (
                <div className="flex items-center gap-2 py-6 text-sm text-slate-500">
                    <Loader2 className="h-5 w-5 animate-spin" />
                    Đang tải danh sách Exercise...
                </div>
            ) : error ? (
                <div className="rounded-lg border border-red-200 bg-red-50 p-4">
                    <p className="text-sm text-red-700">
                        {error}
                    </p>

                    <button
                        type="button"
                        onClick={() => void loadExercises()}
                        className="mt-3 inline-flex items-center gap-2 text-sm font-medium text-red-700 hover:text-red-900"
                    >
                        <RefreshCw size={16} />
                        Thử lại
                    </button>
                </div>
            ) : exercises.length === 0 ? (
                <div className="rounded-lg border border-amber-200 bg-amber-50 p-4">
                    <p className="text-sm text-amber-800">
                        Chưa có Exercise.
                    </p>
                </div>
            ) : (
                <div className="space-y-4">
                    <label
                        htmlFor="exercise-select"
                        className="block text-sm font-medium text-slate-700"
                    >
                        Exercise
                    </label>

                    <div className="relative">
                        <select
                            id="exercise-select"
                            value={selectedExercise?.id ?? ''}
                            onChange={(event) => {
                                const exercise = exercises.find(
                                    (item) =>
                                        item.id === Number(event.target.value),
                                )

                                if (exercise) {
                                    onSelect(exercise)
                                }
                            }}
                            className="w-full appearance-none rounded-lg border border-slate-300 bg-white px-4 py-3 pr-10 text-sm text-slate-800 outline-none transition focus:border-cyan-500 focus:ring-2 focus:ring-cyan-100"
                        >
                            <option value="" disabled>
                                -- Chọn Exercise --
                            </option>

                            {exercises.map((exercise) => (
                                <option
                                    key={exercise.id}
                                    value={exercise.id}
                                >
                                    {exercise.name} - {getStatusLabel(exercise.status)}
                                </option>
                            ))}
                        </select>

                        <ChevronDown
                            size={18}
                            className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-slate-400"
                        />
                    </div>

                    {selectedExercise && (
                        <div className="rounded-lg border border-cyan-200 bg-cyan-50 p-4">
                            <div className="flex items-center gap-2">
                                <CheckCircle2
                                    size={20}
                                    className="text-cyan-600"
                                />

                                <span className="font-semibold text-slate-800">
                                    Exercise đã chọn
                                </span>
                            </div>

                            <div className="mt-3 grid gap-3 sm:grid-cols-2">
                                <div>
                                    <p className="text-xs text-slate-500">
                                        Tên Exercise
                                    </p>
                                    <p className="mt-1 text-sm font-medium text-slate-800">
                                        {selectedExercise.name}
                                    </p>
                                </div>

                                <div>
                                    <p className="text-xs text-slate-500">
                                        Trạng thái
                                    </p>
                                    <p className="mt-1 text-sm font-medium text-slate-800">
                                        {getStatusLabel(selectedExercise.status)}
                                    </p>
                                </div>

                                <div>
                                    <p className="text-xs text-slate-500">
                                        Scenario
                                    </p>
                                    <p className="mt-1 text-sm font-medium text-slate-800">
                                        {selectedExercise.scenarioName}
                                    </p>
                                </div>

                                <div>
                                    <p className="text-xs text-slate-500">
                                        Cyber Range
                                    </p>
                                    <p className="mt-1 text-sm font-medium text-slate-800">
                                        {selectedExercise.cyberRangeName}
                                    </p>
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            )}
        </section>
    )
}

export default ExerciseSelector
