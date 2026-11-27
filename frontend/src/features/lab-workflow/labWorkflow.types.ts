import type { ScenarioOption } from '../exercises/exercise.types'

export type LabScenario = ScenarioOption

export interface DefensiveTask {
    id: number
    scenarioId: number
    name: string
    description: string | null
    taskType: string
    sequenceOrder: number
    maxScore: number
    required: boolean
}
