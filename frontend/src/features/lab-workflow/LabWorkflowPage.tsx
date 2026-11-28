import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router'

import ExerciseSelector from './components/ExerciseSelector'
import ScenarioSelector from './components/ScenarioSelector'
import AssessmentSetup from './components/AssessmentSetup'

import { getExerciseById } from '../exercises/exerciseService'
import type { Exercise } from '../exercises/exercise.types'

function LabWorkflowPage() {
    const [params, setParams] = useSearchParams()
    const exerciseId = Number(params.get('exercise'))
    const [exercise, setExercise] = useState<Exercise | null>(null)
    const confirmed = Number(params.get('scenario'))
    const [loadError, setLoadError] = useState(false)
    const selectedExercise = exercise?.id === exerciseId ? exercise : null

    useEffect(() => {
        if (!Number.isSafeInteger(exerciseId) || exerciseId <= 0) return
        let current = true
        void getExerciseById(exerciseId).then((value) => {
            if (!current) return
            if (value.status === 'CREATED' || value.status === 'READY') {
                setExercise(value)
                setLoadError(false)
            } else {
                setLoadError(true)
            }
        }).catch(() => { if (current) setLoadError(true) })
        return () => { current = false }
    }, [exerciseId])

    const handleExerciseSelect = (value: Exercise) => {
        setExercise(value)
        setLoadError(false)
        setParams({ exercise: String(value.id) })
    }

    return (
        <div className="space-y-6">
            <h1 className="text-2xl font-bold text-slate-800">Lab Workflow</h1>
            {loadError && <p role="alert" className="text-sm text-red-700">Không thể tải Exercise đã chọn hoặc trạng thái không phù hợp.</p>}
            <ExerciseSelector selectedExercise={selectedExercise} onSelect={handleExerciseSelect} />
            <ScenarioSelector
                key={selectedExercise?.id}
                selectedExercise={selectedExercise}
                confirmed={selectedExercise?.scenarioId === confirmed}
                onConfirm={() => {
                    if (selectedExercise) {
                        setParams({ exercise: String(selectedExercise.id), scenario: String(selectedExercise.scenarioId) })
                    }
                }}
            />
            {selectedExercise && selectedExercise.scenarioId === confirmed
                && selectedExercise.scenarioName === 'SSH Security Incident Response' && (
                <AssessmentSetup key={selectedExercise.id} exerciseId={selectedExercise.id} />
            )}
        </div>
    )
}

export default LabWorkflowPage
