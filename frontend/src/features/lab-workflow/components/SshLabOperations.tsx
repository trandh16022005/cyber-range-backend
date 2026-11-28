import { isAxiosError } from 'axios'
import { useEffect, useState } from 'react'
import { Link } from 'react-router'

import api from '../../../services/api'
import type { Assessment } from '../../assessments/assessment.types'
import { SshPracticeInstructor } from '../SshPracticePage'
import type { ApiResponse } from '../../exercises/exercise.types'

interface Health {
    target: boolean
    client: boolean
    collector: boolean
    ssh: boolean
    logging: boolean
    collectorBackend: boolean
    error?: string
}

interface LabStatus {
    id: string
    assessmentId: number
    status: 'STARTING' | 'RUNNING' | 'STOPPED' | 'ERROR'
    ready: boolean
    desiredRunning: boolean
    backend: boolean
    health?: Health
}

type AlertStatus = 'OPEN' | 'ACKNOWLEDGED' | 'IN_PROGRESS' | 'CLOSED'

interface Alert {
    id: number
    account: string
    sourceIp: string
    detectedAt: string
    severity: string
    status: AlertStatus
}

interface AlertDetails {
    alert: Alert
    events: Array<{ id: number; assetName: string; eventTimestamp: string; data: { raw?: string; targetHost?: string } }>
    history: Array<{ id: number; actorId: number; action: string; detail: string; createdAt: string; verifiedAt?: string }>
    report?: { id: number; title: string }
}

interface Evaluation {
    scoring: { totalScore: number }
    applicableMaximum: number
    finalScore?: number
    reviewStatus: string
}

const labels: Array<[keyof Omit<Health, 'error'>, string]> = [
    ['target', 'Ubuntu Target'], ['client', 'Client Lab'], ['collector', 'Event Collector'],
    ['ssh', 'SSH Server'], ['logging', 'Ghi Authentication Log'], ['collectorBackend', 'Collector kết nối Backend'],
]

const button = 'rounded-lg bg-cyan-700 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50'
const field = 'rounded-md border border-slate-300 p-2'
const section = 'space-y-3 rounded-xl border border-slate-200 bg-white p-6'

function apiError(cause: unknown) {
    return isAxiosError<ApiResponse<unknown>>(cause)
        ? cause.response?.data?.message ?? 'Không thể kết nối Backend.'
        : 'Không thể hoàn thành thao tác.'
}

export default function SshLabOperations({ exerciseId, assessment }: { exerciseId: number; assessment: Assessment }) {
    const [lab, setLab] = useState<LabStatus | null>(null)
    const [loaded, setLoaded] = useState(false)
    const [pending, setPending] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [reachable, setReachable] = useState(false)
    const path = `/exercises/${exerciseId}/ssh-lab`

    useEffect(() => {
        let current = true
        let timer: ReturnType<typeof setTimeout>
        const poll = async () => {
            try {
                const response = await api.get<ApiResponse<LabStatus>>(path)
                if (!current) return
                setLab(response.data.data ?? null)
                setReachable(true)
            } catch {
                if (current) setReachable(false)
            } finally {
                if (current) {
                    setLoaded(true)
                    timer = setTimeout(() => void poll(), 3500)
                }
            }
        }
        void poll()
        return () => { current = false; clearTimeout(timer) }
    }, [path])

    const operate = async (action: 'start' | 'stop') => {
        setError(null)
        setPending(true)
        try {
            const response = await api.post<ApiResponse<LabStatus>>(`${path}/${action}`, { assessmentId: assessment.id })
            setLab(response.data.data)
        } catch (cause) {
            setError(apiError(cause))
        } finally {
            setPending(false)
        }
    }

    const active = !!lab && lab.status !== 'STOPPED'
    return (
        <>
            <section className={section}>
                <h2 className="text-lg font-semibold text-slate-800">4. SSH Lab</h2>
                {!loaded ? <p>Đang tải trạng thái…</p> : (
                    <>
                        <p role="status" className="text-sm text-slate-700">
                            {lab?.status ?? 'Chưa khởi tạo'}{reachable && lab?.ready ? ' · READY' : ''}
                            {active && lab.assessmentId !== assessment.id ? ` · Assessment #${lab.assessmentId}` : ''}
                            {active && !lab.desiredRunning ? ' · Đang dừng' : ''}
                        </p>
                        <button type="button" className={button}
                            disabled={pending || !reachable || (!!active && (!lab.desiredRunning || lab.assessmentId !== assessment.id))}
                            onClick={() => void operate(active ? 'stop' : 'start')}>
                            {active ? 'Dừng Lab' : 'Khởi động Lab'}
                        </button>
                        {reachable && lab?.ready && (
                            <p className="text-sm text-slate-600">Mã phiên Lab <code>{lab.id}</code></p>
                        )}
                    </>
                )}
                {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
            </section>
            <section className={section}>
                <h2 className="text-lg font-semibold text-slate-800">5. Kiểm tra dịch vụ</h2>
                <dl className="grid gap-2 text-sm sm:grid-cols-2">
                    <div><dt className="font-medium">Backend</dt><dd>{reachable ? 'Có kết nối' : 'Chưa xác nhận kết nối'}</dd></div>
                    {labels.map(([key, label]) => (
                        <div key={key}>
                            <dt className="font-medium">{label}</dt>
                            <dd>{reachable && lab?.health && active ? (lab.health[key] ? 'Đạt' : 'Chưa đạt') : 'Chưa kiểm tra'}</dd>
                        </div>
                    ))}
                </dl>
                {active && (lab.health?.error || lab.status === 'ERROR') && (
                    <p role="alert" className="text-sm text-red-700">{lab.health?.error ?? 'Lab runner không phản hồi đúng hạn.'}</p>
                )}
            </section>
            <SshPracticeInstructor assessment={assessment} />
            <AuthenticationEvents exerciseId={exerciseId} />
            <AlertPanel exerciseId={exerciseId} assessmentId={assessment.id} />
        </>
    )
}

interface AuthenticationEvent {
    id: number
    eventType: 'SSH_AUTH_SUCCESS' | 'SSH_AUTH_FAILURE'
    eventTimestamp: string
    source: string
    data: { sourceIp: string; username: string; targetHost: string }
}

function AuthenticationEvents({ exerciseId }: { exerciseId: number }) {
    const [events, setEvents] = useState<AuthenticationEvent[] | null>(null)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const load = async () => {
        setLoading(true)
        setError(null)
        try {
            const response = await api.get<ApiResponse<AuthenticationEvent[]>>('/events', { params: { exerciseId } })
            setEvents(response.data.data.filter((event) => event.source === 'OPENSSH_COLLECTOR'
                && (event.eventType === 'SSH_AUTH_SUCCESS' || event.eventType === 'SSH_AUTH_FAILURE')).slice(0, 10))
        } catch (cause) {
            setError(apiError(cause))
        } finally {
            setLoading(false)
        }
    }

    return (
        <section className={section}>
            <div className="flex flex-wrap items-center justify-between gap-2">
                <h2 className="text-lg font-semibold text-slate-800">Authentication Events</h2>
                <button type="button" className={button} disabled={loading} onClick={() => void load()}>
                    {loading ? 'Đang tải…' : 'Tải sự kiện gần nhất'}
                </button>
            </div>
            {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
            {events?.length === 0 && <p className="text-sm text-slate-600">Collector chưa ghi nhận sự kiện xác thực.</p>}
            {events && events.length > 0 && (
                <ul className="space-y-1 text-sm">
                    {events.map((event) => (
                        <li key={event.id}>
                            {new Date(event.eventTimestamp).toLocaleString('vi-VN')} · {event.data.sourceIp} · {event.data.username}@{event.data.targetHost}
                            {' · '}{event.eventType === 'SSH_AUTH_SUCCESS' ? 'Thành công' : 'Thất bại'}
                        </li>
                    ))}
                </ul>
            )}
        </section>
    )
}

export function AlertPanel({ exerciseId, assessmentId, student = false }: {
    exerciseId: number
    assessmentId: number
    student?: boolean
}) {
    const [alerts, setAlerts] = useState<Alert[]>([])
    const [filter, setFilter] = useState('')
    const [selected, setSelected] = useState<number | null>(null)
    const [details, setDetails] = useState<AlertDetails | null>(null)
    const [loading, setLoading] = useState(true)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [detail, setDetail] = useState('')
    const [title, setTitle] = useState('')
    const [analysisVerified, setAnalysisVerified] = useState(false)
    const [account, setAccount] = useState('')
    const [targetHost, setTargetHost] = useState('')
    const [evaluation, setEvaluation] = useState<Evaluation | null>(null)
    const path = `/exercises/${exerciseId}/security-alerts`

    useEffect(() => {
        let current = true
        let timer: ReturnType<typeof setTimeout>
        const poll = async () => {
            try {
                const response = await api.get<ApiResponse<Alert[]>>(path, { params: { status: filter || undefined } })
                if (current) setAlerts(response.data.data)
            } catch (cause) {
                if (current) setError(apiError(cause))
            } finally {
                if (current) {
                    setLoading(false)
                    timer = setTimeout(() => void poll(), 5000)
                }
            }
        }
        void poll()
        return () => { current = false; clearTimeout(timer) }
    }, [path, filter])

    useEffect(() => {
        if (selected === null) return
        let current = true
        let timer: ReturnType<typeof setTimeout>
        const poll = async () => {
            try {
                const response = await api.get<ApiResponse<AlertDetails>>(`${path}/${selected}`)
                if (current) setDetails(response.data.data)
            } catch (cause) {
                if (current) setError(apiError(cause))
            } finally {
                if (current) timer = setTimeout(() => void poll(), 3000)
            }
        }
        void poll()
        return () => { current = false; clearTimeout(timer) }
    }, [selected, path])

    const action = async (command: string) => {
        if (busy || selected === null) return
        setBusy(true)
        setError(null)
        try {
            const response = await api.post<ApiResponse<AlertDetails>>(`${path}/${selected}/actions`, {
                action: command, detail, title, assessmentId,
            })
            setDetails(response.data.data)
            setDetail('')
        } catch (cause) {
            setError(apiError(cause))
        } finally {
            setBusy(false)
        }
    }

    const evaluate = async (finalizeReview: boolean) => {
        if (busy || selected === null) return
        setBusy(true)
        setError(null)
        try {
            const response = await api.post<ApiResponse<Evaluation>>(`${path}/${selected}/assessment`, {
                analysisVerified, account, targetHost, finalizeReview,
            })
            setEvaluation(response.data.data)
        } catch (cause) {
            setError(apiError(cause))
        } finally {
            setBusy(false)
        }
    }

    const history = details?.history ?? []
    const containment = history.filter((entry) => entry.action.startsWith('CONTAINMENT_'))
    const lastContainment = containment[containment.length - 1]
    const recovered = history.some((entry) => entry.action === 'RECOVERY_VERIFIED')
    const controls = details?.alert.status
    return (
        <section className={section}>
            <h2 className="text-lg font-semibold text-slate-800">Security Alerts</h2>
            <label className="flex items-center gap-2 text-sm">Trạng thái
                <select className={field} value={filter} onChange={(event) => setFilter(event.target.value)}>
                    <option value="">Tất cả</option>
                    {['OPEN', 'ACKNOWLEDGED', 'IN_PROGRESS', 'CLOSED'].map((status) => <option key={status}>{status}</option>)}
                </select>
            </label>
            {loading ? <p>Đang tải Alert…</p> : alerts.length === 0 ? <p className="text-sm text-slate-600">{student ? 'Chưa có Alert cho đội. Khi có Alert từ Lab, bạn có thể mở log để bắt đầu phân tích.' : 'Chưa có Alert phù hợp.'}</p> : (
                <ul className="space-y-2">
                    {alerts.map((alert) => (
                        <li key={alert.id}>
                            <button type="button" disabled={busy} className="text-left text-sm font-medium text-cyan-800 hover:underline"
                                onClick={() => {
                                    setSelected(alert.id)
                                    setDetails(null)
                                    setEvaluation(null)
                                    setAnalysisVerified(false)
                                    setDetail('')
                                    setTitle('')
                                    setAccount('')
                                    setTargetHost('')
                                    setError(null)
                                }}>
                                #{alert.id} · {alert.sourceIp} · {alert.account} · {alert.severity} · {alert.status}
                            </button>
                        </li>
                    ))}
                </ul>
            )}
            {selected !== null && details === null && <p className="text-sm">Đang tải chi tiết Alert…</p>}
            {details && (
                <div className="space-y-3 rounded-lg border border-slate-200 p-4">
                    <h3 className="font-semibold">Alert #{details.alert.id} · {details.alert.status}</h3>
                    <p className="text-sm">Phát hiện {new Date(details.alert.detectedAt).toLocaleString('vi-VN')} · {details.alert.sourceIp} · {details.alert.account}</p>
                    <details open>
                        <summary className="cursor-pointer text-sm font-medium">Authentication Events ({details.events.length})</summary>
                        {details.events.map((event) => (
                            <pre key={event.id} className="mt-1 overflow-auto whitespace-pre-wrap rounded bg-slate-100 p-2 text-xs">
                                #{event.id} · {event.data.raw ?? event.eventTimestamp}
                            </pre>
                        ))}
                    </details>
                    <ul className="space-y-1 text-sm">
                        {history.map((entry) => (
                            <li key={entry.id}>{new Date(entry.createdAt).toLocaleString('vi-VN')} · #{entry.actorId} · {entry.action} · {entry.detail}</li>
                        ))}
                    </ul>
                    {details.report && <p className="text-sm">Incident Report #{details.report.id} · {details.report.title}</p>}
                    {(controls === 'ACKNOWLEDGED' || (controls === 'IN_PROGRESS' && (!student || !details.report))) && (
                        <label className="block text-sm">{student ? (controls === 'ACKNOWLEDGED' ? 'Nội dung phân tích' : 'Nội dung báo cáo') : 'Phân tích, yêu cầu phản ứng hoặc nội dung báo cáo'}
                            <textarea className={`mt-1 block w-full ${field}`} value={detail} onChange={(event) => setDetail(event.target.value)} />
                        </label>
                    )}
                    {controls === 'IN_PROGRESS' && !details.report && (
                        <label className="block text-sm">Tiêu đề báo cáo
                            <input className={`ml-2 ${field}`} value={title} onChange={(event) => setTitle(event.target.value)} />
                        </label>
                    )}
                    <div className="flex flex-wrap gap-2">
                        {controls === 'OPEN' && <button className={button} disabled={busy} onClick={() => void action('ACKNOWLEDGE')}>Tiếp nhận</button>}
                        {controls === 'ACKNOWLEDGED' && <button className={button} disabled={busy || !detail.trim()} onClick={() => void action('ANALYZE')}>Lưu phân tích</button>}
                        {controls === 'IN_PROGRESS' && (
                            <>
                                {!student && <button className={button} disabled={busy || !detail.trim() || lastContainment?.action === 'CONTAINMENT_REQUESTED' || lastContainment?.action === 'CONTAINMENT_VERIFIED'}
                                    onClick={() => void action('REQUEST_CONTAINMENT')}>Yêu cầu cô lập</button>}
                                <button className={button} disabled={busy || !!details.report || !detail.trim() || !title.trim()}
                                    onClick={() => void action('SUBMIT_REPORT')}>Nộp báo cáo</button>
                                {!student && <button className={button} disabled={busy || !recovered || !details.report}
                                    onClick={() => void action('CLOSE')}>Đóng Alert</button>}
                            </>
                        )}
                    </div>
                    {!student && controls !== 'OPEN' && (
                        <div className="space-y-2 border-t border-slate-200 pt-3 text-sm">
                            <h4 className="font-semibold">Đánh giá và chấm điểm</h4>
                            <label className="block"><input type="checkbox" checked={analysisVerified}
                                onChange={(event) => setAnalysisVerified(event.target.checked)} /> Đã đối chiếu bản phân tích với Authentication Events</label>
                            <label className="block">Tài khoản bị ảnh hưởng <input className={`ml-2 ${field}`} value={account} onChange={(event) => setAccount(event.target.value)} /></label>
                            <label className="block">Máy chủ bị ảnh hưởng <input className={`ml-2 ${field}`} value={targetHost} onChange={(event) => setTargetHost(event.target.value)} /></label>
                            <div className="flex flex-wrap items-center gap-2">
                                <button className={button} disabled={busy} onClick={() => void evaluate(false)}>Tính điểm tạm thời</button>
                                <button className={button} disabled={busy || controls !== 'CLOSED'} onClick={() => void evaluate(true)}>Xác nhận điểm cuối</button>
                                <Link className="font-medium text-cyan-800 underline" to={`/assessments/${assessmentId}`}>Xem Assessment</Link>
                            </div>
                            {evaluation && <p role="status">
                                {evaluation.finalScore === undefined || evaluation.finalScore === null
                                    ? `Điểm tạm thời ${evaluation.scoring.totalScore} / ${evaluation.applicableMaximum}`
                                    : `Điểm cuối ${evaluation.finalScore} / 100`} · {evaluation.reviewStatus}
                            </p>}
                        </div>
                    )}
                </div>
            )}
            {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
        </section>
    )
}
