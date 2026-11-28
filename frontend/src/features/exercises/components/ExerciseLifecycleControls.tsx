import { isAxiosError } from 'axios'
import { useEffect, useState } from 'react'
import { Link } from 'react-router'

import api from '../../../services/api'
import type { ApiResponse, Exercise } from '../exercise.types'

const labels = {
    PREPARE: 'Xác nhận sẵn sàng',
    START: 'Bắt đầu thực hành',
    PAUSE: 'Tạm dừng',
    RESUME: 'Tiếp tục',
    STOP: 'Dừng thực hành',
    COMPLETE: 'Hoàn thành',
    CANCEL: 'Hủy Exercise',
}

type Action = keyof typeof labels
interface Option {
    action: Action
    blockedReason?: string
    confirmationRequired: boolean
}

interface LifecycleView {
    exercise: Exercise
    actions: Option[]
    lab?: { status: string; desiredRunning: boolean }
    awaitingLabStop: boolean
}

function message(error: unknown) {
    return isAxiosError<ApiResponse<unknown>>(error)
        ? error.response?.data?.message ?? 'Không thể kết nối Backend.'
        : 'Không thể cập nhật vòng đời Exercise.'
}

export default function ExerciseLifecycleControls({ exercise, onUpdate }: {
    exercise: Exercise
    onUpdate: (value: Exercise) => void
}) {
    const [view, setView] = useState<LifecycleView | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [busy, setBusy] = useState(false)
    const [requested, setRequested] = useState<{ action: Action; expectedStatus: Exercise['status'] } | null>(null)
    const path = `/exercises/${exercise.id}/lifecycle`

    useEffect(() => {
        if (busy) return
        const controller = new AbortController()
        let timer: ReturnType<typeof setTimeout>
        const refresh = async () => {
            try {
                const { data } = await api.get<ApiResponse<LifecycleView>>(path, { signal: controller.signal })
                if (controller.signal.aborted) return
                setView(data.data)
                onUpdate(data.data.exercise)
                if (requested && (!data.data.lab || data.data.lab.status === 'STOPPED')) {
                    const result = await api.post<ApiResponse<LifecycleView>>(path, requested, { signal: controller.signal })
                    if (!controller.signal.aborted) {
                        setView(result.data.data)
                        onUpdate(result.data.data.exercise)
                        if (!result.data.data.awaitingLabStop) setRequested(null)
                    }
                }
            } catch (cause) {
                if (!controller.signal.aborted) {
                    setRequested(null)
                    setError(message(cause))
                }
            } finally {
                if (!controller.signal.aborted) timer = setTimeout(() => void refresh(), 3500)
            }
        }
        void refresh()
        return () => { controller.abort(); clearTimeout(timer) }
    }, [path, onUpdate, requested, busy])

    const change = async (option: Option) => {
        if (busy || requested || !view || option.blockedReason) return
        const confirmEnding = ['STOP', 'COMPLETE', 'CANCEL'].includes(option.action)
        if (confirmEnding && !window.confirm(`Xác nhận thao tác “${labels[option.action]}”?`)) return
        if (option.confirmationRequired && !window.confirm('Bạn đã kiểm tra môi trường thực hành và xác nhận đã sẵn sàng?')) return
        const request = { action: option.action, expectedStatus: view.exercise.status }
        setBusy(true)
        setError(null)
        try {
            const result = await api.post<ApiResponse<LifecycleView>>(path, {
                ...request, environmentConfirmed: option.confirmationRequired,
            })
            setView(result.data.data)
            onUpdate(result.data.data.exercise)
            if (result.data.data.awaitingLabStop) setRequested(request)
        } catch (cause) {
            setError(message(cause))
        } finally {
            setBusy(false)
        }
    }

    return (
        <section className="space-y-3 rounded-xl border border-slate-200 bg-white p-5">
            <h2 className="text-lg font-semibold">Quản lý vòng đời Exercise</h2>
            <p className="text-sm">Exercise: <strong>{exercise.status}</strong>
                {view?.lab && <> · Lab: <strong>{view.lab.status}</strong></>}
            </p>
            {exercise.scenarioName === 'SSH Security Incident Response' && (
                <Link className="text-sm text-blue-700 underline"
                    to={`/lab-workflow?exercise=${exercise.id}&scenario=${exercise.scenarioId}`}>Mở Lab Workflow và Assessment</Link>
            )}
            {['STOPPED', 'COMPLETED'].includes(exercise.status) && (
                <Link className="block text-sm text-blue-700 underline" to={`/assessments?exerciseId=${exercise.id}`}>Xem kết quả đánh giá</Link>
            )}
            {!view && !error && <p role="status">Đang tải thao tác…</p>}
            {(busy || requested) && <p role="status">{requested ? 'Đang chờ Runner xác nhận dừng Lab…' : 'Đang cập nhật Exercise…'}</p>}
            <div className="flex flex-wrap gap-3">
                {view?.actions.map((option) => (
                    <div key={option.action}>
                        <button type="button" disabled={busy || !!requested || !!option.blockedReason}
                            className="rounded-lg bg-blue-700 px-3 py-2 text-sm font-medium text-white disabled:opacity-50"
                            onClick={() => void change(option)}>{labels[option.action]}</button>
                        {option.blockedReason && <p className="mt-1 max-w-xs text-sm text-slate-600">{option.blockedReason}</p>}
                    </div>
                ))}
            </div>
            {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
        </section>
    )
}
