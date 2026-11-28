import { isAxiosError } from 'axios'
import { useEffect, useState } from 'react'

import api from '../../../services/api'
import SshLabOperations from './SshLabOperations'
import { getAssessments, initializeAssessments } from '../../assessments/assessmentService'
import type { Assessment } from '../../assessments/assessment.types'
import { getExerciseParticipants } from '../../exercises/exerciseService'
import type { ApiResponse } from '../../exercises/exercise.types'

interface CriterionStatus {
    taskId: number
    count: number
    configured: boolean
}

function message(error: unknown) {
    if (isAxiosError<ApiResponse<unknown>>(error)) {
        return error.response?.data?.message ?? 'Không thể kết nối Backend.'
    }
    return 'Không thể tải hoặc khởi tạo Assessment.'
}

export default function AssessmentSetup({ exerciseId, readOnly }: { exerciseId: number; readOnly: boolean }) {
    const [teams, setTeams] = useState<Map<number, string>>(new Map())
    const [assessments, setAssessments] = useState<Assessment[]>([])
    const [teamId, setTeamId] = useState<number | null>(null)
    const [missingCriteria, setMissingCriteria] = useState<number | null>(null)
    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        let current = true
        void Promise.all([
            getExerciseParticipants(exerciseId),
            getAssessments(exerciseId),
            api.get<ApiResponse<CriterionStatus[]>>(`/exercises/${exerciseId}/assessment-criteria-status`),
        ]).then(([participants, existing, criteria]) => {
            if (!current) return
            const blueTeams = new Map<number, string>()
            for (const participant of participants) {
                if (participant.role === 'BLUE' && participant.teamId !== null && participant.teamName) {
                    blueTeams.set(participant.teamId, participant.teamName)
                }
            }
            setTeams(blueTeams)
            if (blueTeams.size === 1) setTeamId(blueTeams.keys().next().value ?? null)
            setAssessments(existing)
            setMissingCriteria(criteria.data.data.filter((task) => !task.configured).length)
        }).catch((cause: unknown) => {
            if (current) setError(message(cause))
        }).finally(() => { if (current) setLoading(false) })
        return () => { current = false }
    }, [exerciseId])

    const assessment = assessments.find((item) => item.teamId === teamId)

    const initialize = async () => {
        if (teamId === null || saving || assessment) return
        setSaving(true)
        setError(null)
        try {
            const [created] = await initializeAssessments(exerciseId, teamId)
            if (!created) throw new Error('Assessment response is empty')
            setAssessments((items) => [...items.filter((item) => item.id !== created.id), created])
        } catch (cause) {
            setError(message(cause))
        } finally {
            setSaving(false)
        }
    }

    return (
        <>
        <section className="space-y-3 rounded-xl border border-slate-200 bg-white p-6">
            <h2 className="text-lg font-semibold text-slate-800">3. Khởi tạo Assessment</h2>
            {loading ? <p className="text-sm text-slate-600">Đang tải Team và Criteria…</p> : (
                <>
                    {teams.size === 0 ? <p className="text-sm text-slate-600">Chưa có Team BLUE.</p> : (
                        <label className="block text-sm text-slate-700">
                            Team BLUE
                            <select
                                className="ml-2 rounded-md border border-slate-300 p-2"
                                disabled={saving}
                                value={teamId ?? ''}
                                onChange={(event) => setTeamId(event.target.value ? Number(event.target.value) : null)}
                            >
                                <option value="">Chọn Team</option>
                                {[...teams].map(([id, name]) => <option key={id} value={id}>{name}</option>)}
                            </select>
                        </label>
                    )}
                    {!!missingCriteria && <p className="text-sm text-amber-800">{missingCriteria} task thiếu Criteria hợp lệ.</p>}
                    {assessment ? (
                        <p role="status" className="text-sm text-emerald-800">
                            Assessment #{assessment.id} · {assessment.teamName} · {assessment.status}
                        </p>
                    ) : (
                        <button
                            type="button"
                            disabled={readOnly || teamId === null || saving || missingCriteria !== 0}
                            onClick={() => void initialize()}
                            className="rounded-lg bg-cyan-700 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
                        >
                            {saving ? 'Đang khởi tạo…' : 'Khởi tạo Assessment'}
                        </button>
                    )}
                </>
            )}
            {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
        </section>
        {assessment && <SshLabOperations key={assessment.id} exerciseId={exerciseId} assessment={assessment} readOnly={readOnly} />}
        </>
    )
}
