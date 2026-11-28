import { useState } from 'react'
import api from '../../services/api'
import type { ApiResponse } from '../exercises/exercise.types'

interface EvidenceLink {
    evidenceId: number
    evidenceType: string
    verificationStatus: string
    origin?: string
}

export default function ScoreEvidence({ scoreId }: { scoreId: number }) {
    const [links, setLinks] = useState<EvidenceLink[] | null>(null)
    const [pending, setPending] = useState(false)
    const [error, setError] = useState(false)

    const load = async () => {
        setPending(true)
        setError(false)
        try {
            const response = await api.get<ApiResponse<EvidenceLink[]>>(`/assessment-scores/${scoreId}/evidence`)
            setLinks(response.data.data)
        } catch {
            setError(true)
        } finally {
            setPending(false)
        }
    }

    return (
        <details className="mt-2 text-xs" onToggle={(event) => {
            if (event.currentTarget.open && !pending && links === null) void load()
        }}>
            <summary className="cursor-pointer text-cyan-800">Evidence</summary>
            {pending && <p>Đang tải Evidence…</p>}
            {error && (
                <button type="button" className="text-red-700 underline" onClick={() => void load()}>
                    Không tải được Evidence. Thử lại
                </button>
            )}
            {links?.length === 0 && <p>Chưa có Evidence liên quan.</p>}
            {links?.map((link) => (
                <p key={link.evidenceId}>#{link.evidenceId} · {link.evidenceType} · {link.verificationStatus}{link.origin === 'TRAINING_SIMULATION' && ' · Mô phỏng'}</p>
            ))}
        </details>
    )
}
