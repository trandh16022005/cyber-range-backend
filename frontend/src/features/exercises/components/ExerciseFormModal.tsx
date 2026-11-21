import { useEffect, useState, type FormEvent } from 'react'
import { X } from 'lucide-react'

import {
    createExercise,
    getActiveCyberRanges,
    getPublishedScenarios,
} from '../exerciseService'

import type {
    CyberRangeOption,
    ScenarioOption,
} from '../exercise.types'

interface ExerciseFormModalProps {
    isOpen: boolean
    onClose: () => void
    onSuccess: () => void
}

function ExerciseFormModal({
                               isOpen,
                               onClose,
                               onSuccess,
                           }: ExerciseFormModalProps) {
    const [name, setName] = useState('')
    const [scenarioId, setScenarioId] = useState('')
    const [cyberRangeId, setCyberRangeId] = useState('')

    const [scenarios, setScenarios] = useState<ScenarioOption[]>([])
    const [cyberRanges, setCyberRanges] = useState<CyberRangeOption[]>([])

    const [loadingOptions, setLoadingOptions] = useState(false)
    const [submitting, setSubmitting] = useState(false)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        if (!isOpen) return

        const loadOptions = async () => {
            setLoadingOptions(true)
            setError(null)

            try {
                const [scenarioData, cyberRangeData] = await Promise.all([
                    getPublishedScenarios(),
                    getActiveCyberRanges(),
                ])

                setScenarios(scenarioData)
                setCyberRanges(cyberRangeData)
            } catch (err: unknown) {
                const message =
                    typeof err === 'object' &&
                    err !== null &&
                    'response' in err
                        ? (
                            err as {
                                response?: {
                                    data?: { message?: string }
                                }
                            }
                        ).response?.data?.message
                        : undefined

                setError(message ?? 'Failed to load form options.')
            } finally {
                setLoadingOptions(false)
            }
        }

        void loadOptions()
    }, [isOpen])

    if (!isOpen) return null

    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault()
        setError(null)

        if (!name.trim() || !scenarioId || !cyberRangeId) {
            setError('Please complete all required fields.')
            return
        }

        setSubmitting(true)

        try {
            await createExercise({
                name: name.trim(),
                scenarioId: Number(scenarioId),
                cyberRangeId: Number(cyberRangeId),
            })

            setName('')
            setScenarioId('')
            setCyberRangeId('')

            onSuccess()
            onClose()
        } catch (err: unknown) {
            const message =
                typeof err === 'object' &&
                err !== null &&
                'response' in err
                    ? (
                        err as {
                            response?: {
                                data?: { message?: string }
                            }
                        }
                    ).response?.data?.message
                    : undefined

            setError(message ?? 'Failed to create exercise.')
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
            <div className="w-full max-w-lg rounded-xl bg-white shadow-xl">
                <div className="flex items-center justify-between border-b border-slate-200 px-6 py-4">
                    <div>
                        <h2 className="text-lg font-semibold text-slate-900">
                            Create Exercise
                        </h2>
                        <p className="mt-1 text-sm text-slate-500">
                            Configure a new Cyber Range exercise.
                        </p>
                    </div>

                    <button
                        type="button"
                        onClick={onClose}
                        disabled={submitting}
                        className="rounded-lg p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-700"
                    >
                        <X className="h-5 w-5" />
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="space-y-5 p-6">
                    {error && (
                        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                            {error}
                        </div>
                    )}

                    <div>
                        <label className="mb-1.5 block text-sm font-medium text-slate-700">
                            Exercise Name
                        </label>

                        <input
                            type="text"
                            value={name}
                            onChange={(event) => setName(event.target.value)}
                            maxLength={150}
                            placeholder="Enter exercise name"
                            disabled={submitting}
                            className="w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                            required
                        />
                    </div>

                    <div>
                        <label className="mb-1.5 block text-sm font-medium text-slate-700">
                            Published Scenario
                        </label>

                        <select
                            value={scenarioId}
                            onChange={(event) => setScenarioId(event.target.value)}
                            disabled={loadingOptions || submitting}
                            className="w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                            required
                        >
                            <option value="">Select a scenario</option>

                            {scenarios.map((scenario) => (
                                <option
                                    key={scenario.id}
                                    value={scenario.id}
                                >
                                    {scenario.name}
                                </option>
                            ))}
                        </select>

                        {!loadingOptions && scenarios.length === 0 && (
                            <p className="mt-1.5 text-xs text-amber-600">
                                No published scenarios available.
                            </p>
                        )}
                    </div>

                    <div>
                        <label className="mb-1.5 block text-sm font-medium text-slate-700">
                            Active Cyber Range
                        </label>

                        <select
                            value={cyberRangeId}
                            onChange={(event) => setCyberRangeId(event.target.value)}
                            disabled={loadingOptions || submitting}
                            className="w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                            required
                        >
                            <option value="">Select a Cyber Range</option>

                            {cyberRanges.map((cyberRange) => (
                                <option
                                    key={cyberRange.id}
                                    value={cyberRange.id}
                                >
                                    {cyberRange.name}
                                </option>
                            ))}
                        </select>

                        {!loadingOptions && cyberRanges.length === 0 && (
                            <p className="mt-1.5 text-xs text-amber-600">
                                No active Cyber Ranges available.
                            </p>
                        )}
                    </div>

                    {loadingOptions && (
                        <p className="text-sm text-slate-500">
                            Loading scenarios and Cyber Ranges...
                        </p>
                    )}

                    <div className="flex justify-end gap-3 border-t border-slate-100 pt-4">
                        <button
                            type="button"
                            onClick={onClose}
                            disabled={submitting}
                            className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
                        >
                            Cancel
                        </button>

                        <button
                            type="submit"
                            disabled={
                                submitting ||
                                loadingOptions ||
                                scenarios.length === 0 ||
                                cyberRanges.length === 0
                            }
                            className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                        >
                            {submitting ? 'Creating...' : 'Create Exercise'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    )
}

export default ExerciseFormModal
