import { useState } from 'react'

import ExerciseSelector from './components/ExerciseSelector'
import ScenarioSelector from './components/ScenarioSelector'

import type { Exercise } from '../exercises/exercise.types'

function LabWorkflowPage() {
    const [selectedExercise, setSelectedExercise] =
        useState<Exercise | null>(null)

    const handleExerciseSelect = (exercise: Exercise) => {
        setSelectedExercise(exercise)
    }

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-2xl font-bold text-slate-800">
                    Lab Workflow
                </h1>

                <p className="mt-1 text-sm text-slate-500">
                    Khởi tạo và quản lý quy trình thực hành Cyber Range.
                </p>
            </div>

            <div className="rounded-xl border border-slate-200 bg-white p-6">
                <p className="text-sm text-slate-600">
                    Quy trình thực hành:
                    Chọn Exercise → Chọn Scenario SSH Security
                    → Khởi tạo Assessment → Khởi động Lab
                    → Kiểm tra trạng thái dịch vụ.
                </p>
            </div>

            {/* Step 1: Select Exercise */}
            <ExerciseSelector
                selectedExercise={selectedExercise}
                onSelect={handleExerciseSelect}
            />

            {/* Step 2: Select Scenario and view Defensive Tasks */}
            <ScenarioSelector
                selectedExercise={selectedExercise}
            />
        </div>
    )
}

export default LabWorkflowPage
