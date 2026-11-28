import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'

import api from '../../services/api'
import { getAssessmentById } from '../assessments/assessmentService'
import type { Assessment } from '../assessments/assessment.types'
import type { ApiResponse } from '../exercises/exercise.types'
import { AlertPanel } from './components/SshLabOperations'

export default function StudentSshLabPage() {
    const { id } = useParams()
    return <StudentSshAssignment key={id} assessmentId={Number(id)} />
}

function StudentSshAssignment({ assessmentId }: { assessmentId: number }) {
    const [assessment, setAssessment] = useState<Assessment | null>(null)
    const [ready, setReady] = useState(false)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        let active = true
        let timer: ReturnType<typeof setTimeout> | undefined
        if (!Number.isSafeInteger(assessmentId) || assessmentId <= 0) return

        const poll = async (exerciseId: number) => {
            try {
                const response = await api.get<ApiResponse<{ ready: boolean } | null>>(`/exercises/${exerciseId}/ssh-lab`)
                if (active) {
                    setReady(response.data.data?.ready ?? false)
                    setError(null)
                    timer = setTimeout(() => void poll(exerciseId), 3500)
                }
                return true
            } catch {
                if (active) setError('Không thể xem Lab của đội. Kiểm tra phân công BLUE hoặc thử tải lại trang.')
                return false
            }
        }

        void getAssessmentById(assessmentId).then(async (value) => {
            if (!active) return
            if (!value.sshWorkflow) {
                setError('Assessment này không thuộc bài thực hành SSH.')
                return
            }
            if (await poll(value.exerciseId) && active) setAssessment(value)
        }).catch(() => { if (active) setError('Không thể tải bài SSH được giao.') })
        return () => { active = false; clearTimeout(timer) }
    }, [assessmentId])

    if (error) return <p role="alert" className="text-red-700">{error}</p>
    if (!Number.isSafeInteger(assessmentId) || assessmentId <= 0) return <p role="alert">Assessment không hợp lệ.</p>
    if (!assessment) return <p>Đang tải bài SSH…</p>

    return (
        <div className="space-y-6">
            <h1 className="text-2xl font-bold text-slate-800">Bài thực hành SSH</h1>
            <p className="text-sm text-slate-700">{assessment.teamName} · Exercise #{assessment.exerciseId}</p>
            <p role="status" className="text-sm text-slate-700">
                {ready ? 'Lab sẵn sàng.' : 'Lab chưa sẵn sàng. Giảng viên phụ trách khởi động và điều khiển Lab.'}
            </p>
            <Link className="text-cyan-800 underline" to={`/assessments/${assessmentId}`}>Xem kết quả và Evidence</Link>
            <AlertPanel exerciseId={assessment.exerciseId} assessmentId={assessmentId} student />
        </div>
    )
}
