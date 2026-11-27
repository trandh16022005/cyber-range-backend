import { useEffect, useState } from 'react'
import {
    AlertCircle,
    CheckCircle2,
    ClipboardList,
    Loader2,
    RefreshCw,
} from 'lucide-react'

import {
    getLabScenario,
    getScenarioDefensiveTasks,
} from '../../exercises/exerciseService'

import type { Exercise } from '../../exercises/exercise.types'
import type {
    DefensiveTask,
    LabScenario,
} from '../labWorkflow.types'

interface ScenarioSelectorProps {
    selectedExercise: Exercise | null
}

function ScenarioSelector({
                              selectedExercise,
                          }: ScenarioSelectorProps) {
    const [scenario, setScenario] = useState<LabScenario | null>(null)
    const [tasks, setTasks] = useState<DefensiveTask[]>([])
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const loadScenario = async () => {
        if (!selectedExercise) {
            setScenario(null)
            setTasks([])
            return
        }

        try {
            setLoading(true)
            setError(null)
            setScenario(null)
            setTasks([])

            const scenarioId = selectedExercise.scenarioId

            const [scenarioData, taskData] = await Promise.all([
                getLabScenario(scenarioId),
                getScenarioDefensiveTasks(scenarioId),
            ])

            setScenario(scenarioData)

            setTasks(
                [...taskData].sort(
                    (a, b) => a.sequenceOrder - b.sequenceOrder,
                ),
            )
        } catch {
            setError(
                'Không thể tải thông tin Scenario hoặc Defensive Tasks. Vui lòng kiểm tra kết nối Backend.',
            )
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        void loadScenario()
    }, [selectedExercise?.id, selectedExercise?.scenarioId])

    if (!selectedExercise) {
        return (
            <section className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
                <h2 className="text-lg font-semibold text-slate-800">
                    2. Chọn Scenario SSH Security
                </h2>

                <p className="mt-2 text-sm text-slate-500">
                    Vui lòng chọn Exercise ở bước 1 để xem Scenario tương ứng.
                </p>
            </section>
        )
    }

    return (
        <section className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="mb-5">
                <h2 className="text-lg font-semibold text-slate-800">
                    2. Chọn Scenario SSH Security
                </h2>

                <p className="mt-1 text-sm text-slate-500">
                    Scenario được xác định tự động từ Exercise đã chọn.
                </p>
            </div>

            {loading ? (
                <div className="flex items-center gap-2 py-6 text-sm text-slate-500">
                    <Loader2 className="h-5 w-5 animate-spin" />
                    Đang tải thông tin Scenario và Defensive Tasks...
                </div>
            ) : error ? (
                <div className="rounded-lg border border-red-200 bg-red-50 p-4">
                    <div className="flex items-start gap-2">
                        <AlertCircle className="mt-0.5 h-5 w-5 text-red-600" />

                        <p className="text-sm text-red-700">
                            {error}
                        </p>
                    </div>

                    <button
                        type="button"
                        onClick={() => void loadScenario()}
                        className="mt-3 inline-flex items-center gap-2 text-sm font-medium text-red-700 hover:text-red-900"
                    >
                        <RefreshCw size={16} />
                        Thử lại
                    </button>
                </div>
            ) : scenario ? (
                <div className="space-y-5">
                    <div className="rounded-lg border border-cyan-200 bg-cyan-50 p-4">
                        <div className="flex items-center gap-2">
                            <CheckCircle2
                                size={20}
                                className="text-cyan-600"
                            />

                            <span className="font-semibold text-slate-800">
                                Scenario đã được xác định
                            </span>
                        </div>

                        <div className="mt-4 grid gap-4 sm:grid-cols-2">
                            <div>
                                <p className="text-xs text-slate-500">
                                    Tên Scenario
                                </p>
                                <p className="mt-1 text-sm font-semibold text-slate-800">
                                    {scenario.name}
                                </p>
                            </div>

                            <div>
                                <p className="text-xs text-slate-500">
                                    Loại Scenario
                                </p>
                                <p className="mt-1 text-sm font-medium text-slate-800">
                                    {scenario.type}
                                </p>
                            </div>

                            <div>
                                <p className="text-xs text-slate-500">
                                    Độ khó
                                </p>
                                <p className="mt-1 text-sm font-medium text-slate-800">
                                    {scenario.difficulty ?? 'Chưa xác định'}
                                </p>
                            </div>

                            <div>
                                <p className="text-xs text-slate-500">
                                    Trạng thái
                                </p>
                                <p className="mt-1 text-sm font-medium text-slate-800">
                                    {scenario.status}
                                </p>
                            </div>
                        </div>

                        <div className="mt-4">
                            <p className="text-xs text-slate-500">
                                Mô tả
                            </p>
                            <p className="mt-1 whitespace-pre-line text-sm text-slate-700">
                                {scenario.description || 'Không có mô tả.'}
                            </p>
                        </div>
                    </div>

                    <div>
                        <div className="mb-3 flex items-center justify-between">
                            <div className="flex items-center gap-2">
                                <ClipboardList
                                    size={20}
                                    className="text-slate-600"
                                />

                                <h3 className="font-semibold text-slate-800">
                                    Defensive Tasks
                                </h3>
                            </div>

                            <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
                                {tasks.length} Tasks
                            </span>
                        </div>

                        {tasks.length === 0 ? (
                            <div className="rounded-lg border border-amber-200 bg-amber-50 p-4">
                                <p className="text-sm text-amber-800">
                                    Scenario này chưa có Defensive Tasks.
                                    Chưa thể tiếp tục khởi tạo Assessment.
                                </p>
                            </div>
                        ) : (
                            <div className="overflow-hidden rounded-lg border border-slate-200">
                                <div className="divide-y divide-slate-200">
                                    {tasks.map((task) => (
                                        <div
                                            key={task.id}
                                            className="p-4"
                                        >
                                            <div className="flex items-start justify-between gap-3">
                                                <div className="flex min-w-0 gap-3">
                                                    <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-cyan-100 text-sm font-semibold text-cyan-700">
                                                        {task.sequenceOrder}
                                                    </span>

                                                    <div className="min-w-0">
                                                        <p className="font-medium text-slate-800">
                                                            {task.name}
                                                        </p>

                                                        <p className="mt-1 text-sm text-slate-500">
                                                            {task.description || 'Không có mô tả.'}
                                                        </p>

                                                        <p className="mt-2 text-xs text-slate-500">
                                                            Loại: {task.taskType}
                                                        </p>
                                                    </div>
                                                </div>

                                                <div className="shrink-0 text-right">
                                                    <p className="text-sm font-semibold text-slate-800">
                                                        {task.maxScore} điểm
                                                    </p>

                                                    <span className={`mt-1 inline-block rounded-full px-2 py-1 text-xs font-medium ${
                                                        task.required
                                                            ? 'bg-red-50 text-red-700'
                                                            : 'bg-slate-100 text-slate-600'
                                                    }`}>
                                                        {task.required
                                                            ? 'Bắt buộc'
                                                            : 'Tùy chọn'}
                                                    </span>
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            ) : null}
        </section>
    )
}

export default ScenarioSelector
