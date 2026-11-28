import { useEffect, useState } from 'react'
import { isAxiosError } from 'axios'
import api from '../../services/api'
import type { Assessment } from '../assessments/assessment.types'
import type { ApiResponse } from '../exercises/exercise.types'
import { getExerciseParticipants } from '../exercises/exerciseService'

interface Observation {
    id: number
    second: number
    source: string
    account: string
    target: string
    outcome: 'SUCCESS' | 'FAILURE'
}

interface ObservedEvent {
    provenance?: string
    alertId?: number
    id: number
    eventType: string
    severity?: string
    eventTimestamp: string
    sourceAddress?: string
    account?: string
    target?: string
    outcome?: string
    raw?: string
}

const reportFields = {
    summary: 'Tóm tắt sự cố', timeline: 'Dòng thời gian', relatedEvents: 'Event liên quan',
    affectedAccount: 'Tài khoản ảnh hưởng', affectedHost: 'Máy chủ ảnh hưởng', findings: 'Kết quả điều tra',
    proposedResponse: 'Phản ứng đề xuất', responseOutcome: 'Kết quả phản ứng',
    recoveryVerification: 'Xác minh phục hồi', lessonsLearned: 'Bài học rút ra',
}

type ReportSection = keyof typeof reportFields
const reportKeys = Object.keys(reportFields) as ReportSection[]

interface Practice {
    id: string
    exerciseId: number
    assessmentId: number
    redTeamId: number
    blueTeamId: number
    role: 'INSTRUCTOR' | 'RED' | 'BLUE' | 'WHITE'
    state: 'APPROVED' | 'ACTIVE' | 'PROPOSED' | 'RECOVERY' | 'SUBMITTED' | 'REVIEWED'
    ready: boolean
    canStartScenario: boolean
    connectionStatus: 'NONE' | 'QUEUED' | 'CHECKING' | 'SUCCEEDED' | 'FAILED'
    target: string
    criteria: Array<{ taskId: number; name: string; maximum: number }>
    observations: Observation[]
    recovery: Observation[]
    alert: boolean
    submission?: {
        observationIds?: number[]
        analysis?: string
        account?: string
        target?: string
        proposal?: string
        responseReview?: string
        report?: string
        reportSections?: Partial<Record<ReportSection, string>>
        studentAcknowledgedAt?: string
        source?: string
        impact?: string
        recovered?: boolean
        reportVersion?: number
        returnReason?: string
        confirmedReview?: { version: number }
        review?: string
        marks?: Array<{ name: string; points: number; maximum: number; reason: string }>
    }
    score?: number
    observedEvents: ObservedEvent[]
    metrics?: {
        systemDetectionMillis?: number
        studentDetectionSeconds?: number
        studentResponseSeconds?: number
        simulatedResponseSeconds?: number
    }
    history: Array<{ action: string; detail?: string; createdAt: string; actorId?: number }>
}

const box = 'space-y-4 rounded-xl border border-slate-200 bg-white p-6'
const button = 'rounded-lg bg-cyan-800 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50'
const input = 'w-full rounded-md border border-slate-300 p-2'
const stages: Record<Practice['state'], string> = {
    APPROVED: 'Chờ RED kiểm tra kết nối và bắt đầu',
    ACTIVE: 'BLUE đang điều tra',
    PROPOSED: 'Chờ giảng viên duyệt đề xuất',
    RECOVERY: 'BLUE xác minh phục hồi và nộp bài',
    SUBMITTED: 'Chờ giảng viên duyệt bài',
    REVIEWED: 'Đã duyệt điểm',
}

function message(error: unknown, instructor = false) {
    if (isAxiosError<ApiResponse<unknown>>(error)) {
        if (error.response?.status === 404) return 'Tính năng thực hành có hướng dẫn chưa được bật trên Backend.'
        if (error.response?.status === 503 && instructor) return 'Schema bài thực hành SSH chưa sẵn sàng. Quản trị viên cần kiểm tra migration 002 và 003.'
        return error.response?.data?.message ?? 'Không thể kết nối Backend.'
    }
    return 'Không thể thực hiện thao tác.'
}

function usePractice(path: string, instructor = false) {
    const [runs, setRuns] = useState<Practice[] | null>(null)
    const [error, setError] = useState<string | null>(null)
    useEffect(() => {
        let active = true
        let timer: ReturnType<typeof setTimeout>
        const poll = async () => {
            try {
                const response = await api.get<ApiResponse<Practice[]>>(path)
                if (active) {
                    setRuns(response.data.data)
                    setError(null)
                }
            } catch (cause) {
                if (active) setError(message(cause, instructor))
            } finally {
                if (active) timer = setTimeout(() => void poll(), 4000)
            }
        }
        void poll()
        return () => { active = false; clearTimeout(timer) }
    }, [path, instructor])
    const update = (run: Practice) => setRuns((current) => [run, ...(current ?? []).filter((item) => item.id !== run.id)])
    return { runs, error, update }
}

export default function SshPracticePage() {
    const { runs, error, update } = usePractice('/ssh-practice')
    return (
        <div className="space-y-4">
            <h1 className="text-2xl font-bold text-slate-900">Bài thực hành SSH được giao</h1>
            {error && <p role="alert" className="text-red-700">{error}</p>}
            {!runs && !error && <p>Đang tải bài thực hành…</p>}
            {runs?.length === 0 && <p>Chưa có lượt được giao. Giảng viên sẽ phân công đội và duyệt lượt trước khi bạn bắt đầu.</p>}
            {runs?.map((run) => <PracticeCard key={run.id} run={run} update={update} />)}
        </div>
    )
}

export function SshPracticeInstructor({ assessment, lab, reachable }: {
    assessment: Assessment
    lab: { id: string; assessmentId: number; ready: boolean; desiredRunning: boolean } | null
    reachable: boolean
}) {
    const path = `/assessments/${assessment.id}/ssh-practice`
    const { runs, error, update } = usePractice(path, true)
    const [redTeam, setRedTeam] = useState('')
    const [teams, setTeams] = useState<Array<{ id: number; name: string }>>([])
    const [saving, setSaving] = useState(false)
    const [notice, setNotice] = useState<string | null>(null)
    const [eligibility, setEligibility] = useState<{ key: string; reason: string | null } | null>(null)
    const labBlocker = !reachable ? 'Chưa xác nhận kết nối Backend.'
        : !lab ? 'Khởi động Lab cho Assessment này.'
        : lab.assessmentId !== assessment.id ? 'Lab thuộc Assessment khác.'
        : !lab.desiredRunning ? 'Phiên Lab đang dừng hoặc đã dừng.'
        : !lab.ready ? 'Chờ các dịch vụ Lab đạt READY.'
        : null
    const eligibilityKey = !labBlocker && redTeam && lab ? `${path}:${lab.id}:${redTeam}` : null
    const currentEligibility = eligibilityKey && eligibility?.key === eligibilityKey ? eligibility : null

    useEffect(() => {
        if (!eligibilityKey) return
        const controller = new AbortController()
        void api.get<ApiResponse<string>>(`${path}/approval`, {
            params: { redTeamId: redTeam }, signal: controller.signal,
        }).then(({ data }) => {
            if (!controller.signal.aborted) {
                setEligibility({ key: eligibilityKey, reason: data.data === lab?.id ? null : 'Phiên Lab đã thay đổi.' })
            }
        }).catch((cause: unknown) => {
            if (!controller.signal.aborted) setEligibility({ key: eligibilityKey, reason: message(cause, true) })
        })
        return () => controller.abort()
    }, [eligibilityKey, path, redTeam, lab?.id, runs])

    useEffect(() => {
        let active = true
        void getExerciseParticipants(assessment.exerciseId).then((participants) => {
            if (!active) return
            const red = new Map<number, string>()
            for (const participant of participants) {
                if (participant.role === 'RED' && participant.teamId !== null && participant.teamName) {
                    red.set(participant.teamId, participant.teamName)
                }
            }
            setTeams([...red].map(([id, name]) => ({ id, name })))
            if (red.size === 1) setRedTeam(String(red.keys().next().value))
        }).catch((cause: unknown) => { if (active) setNotice(message(cause)) })
        return () => { active = false }
    }, [assessment.exerciseId])

    const approve = async () => {
        if (!lab || labBlocker || !currentEligibility || currentEligibility.reason || saving || !runs || error) return
        setSaving(true)
        setNotice(null)
        try {
            const response = await api.post<ApiResponse<Practice>>(path, { redTeamId: Number(redTeam), labSessionId: lab.id })
            update(response.data.data)
        } catch (cause) {
            setNotice(message(cause, true))
        } finally {
            setSaving(false)
        }
    }

    return (
        <div className="space-y-4">
            <section className={box}>
                <h2 className="text-lg font-semibold">Điều phối bài thực hành</h2>
                <p className="text-sm">Đội BLUE: {assessment.teamName}. Phân công học viên RED và BLUE trong Exercise, rồi đợi Lab READY.</p>
                {teams.length === 0 && <p className="text-sm text-slate-600">Chưa có đội RED được phân công.</p>}
                <label className="block text-sm">Đội RED
                    <select className={input} value={redTeam} onChange={(event) => setRedTeam(event.target.value)}>
                        <option value="">Chọn đội RED</option>
                        {teams.map((team) => <option value={team.id} key={team.id}>{team.name}</option>)}
                    </select>
                </label>
                <button type="button" className={button} disabled={saving || !!labBlocker || !runs || !!error || !currentEligibility || !!currentEligibility.reason}
                    onClick={() => void approve()}>Duyệt lượt cho phiên Lab hiện tại</button>
                <p className="text-sm text-slate-600">{labBlocker ?? (!redTeam ? 'Chọn đội RED để kiểm tra điều kiện duyệt.' :
                    currentEligibility?.reason ?? (!currentEligibility ? 'Đang kiểm tra điều kiện duyệt…' : 'Đủ điều kiện duyệt.'))}</p>
                {(error || notice) && <p role="alert" className="text-red-700">{notice ?? error}</p>}
            </section>
            {runs?.map((run) => <PracticeCard key={run.id} run={run} update={update} />)}
        </div>
    )
}

function PracticeCard({ run, update }: { run: Practice; update: (value: Practice) => void }) {
    const [selected, setSelected] = useState<number[]>(run.submission?.observationIds ?? [])
    const [account, setAccount] = useState(run.submission?.account ?? '')
    const [target, setTarget] = useState(run.submission?.target ?? '')
    const [analysis, setAnalysis] = useState(run.submission?.analysis ?? '')
    const [proposal, setProposal] = useState(run.submission?.proposal ?? '')
    const [report, setReport] = useState(run.submission?.reportSections ?? {})
    const [recovered, setRecovered] = useState(run.submission?.recovered === undefined ? '' : run.submission.recovered ? 'yes' : 'no')
    const [source, setSource] = useState(run.submission?.source ?? '')
    const [impact, setImpact] = useState(run.submission?.impact ?? '')
    const [review, setReview] = useState('')
    const [analysisResult, setAnalysisResult] = useState('')
    const [reportResult, setReportResult] = useState('')
    const [pending, setPending] = useState(false)
    const [feedback, setFeedback] = useState('')
    const studentBlue = run.role === 'BLUE'
    const canInvestigate = studentBlue && run.state === 'ACTIVE' && !!run.submission?.studentAcknowledgedAt
    const selectedObservations = selected.filter((id) => run.observations.some((row) => row.id === id))
    const investigationIssues = [
        selectedObservations.length === 0 && 'Chọn ít nhất một bản ghi liên quan.',
        !account.trim() && 'Nhập tài khoản bị ảnh hưởng.',
        !target.trim() && 'Nhập máy chủ bị ảnh hưởng.',
        !source.trim() && 'Nhập IP nguồn nghi vấn.',
        impact.trim().length < 3 && 'Nhập phạm vi ảnh hưởng (ít nhất 3 ký tự).',
        analysis.trim().length < 10 && 'Phân tích log phải có ít nhất 10 ký tự.',
    ].filter(Boolean)
    const instructor = run.role === 'INSTRUCTOR'

    const act = async (action: string, fields: Record<string, unknown> = {}) => {
        if (action === 'PUBLISH_REVIEW' && !window.confirm('Công bố điểm bài thực hành vào Assessment của đội?')) return
        setPending(true)
        setFeedback('')
        try {
            const response = await api.post<ApiResponse<Practice>>(`/ssh-practice/${run.id}/actions`, { action, ...fields })
            update(response.data.data)
            if (action === 'SAVE_INVESTIGATION') setFeedback('Đã lưu điều tra. Tiếp theo, gửi đề xuất phản ứng.')
            if (action === 'SAVE_REPORT_DRAFT') setFeedback('Đã lưu bản nháp.')
        } catch (cause) {
            setFeedback(message(cause))
        } finally {
            setPending(false)
        }
    }

    const toggle = (id: number) => setSelected((values) =>
        values.includes(id) ? values.filter((value) => value !== id) : [...values, id])

    return (
        <section className={box}>
            <div>
                <h2 className="text-lg font-semibold">Exercise #{run.exerciseId} · {run.role}</h2>
                <p className="text-sm text-slate-600">SSH Security Incident Response · Target {run.target} · {run.ready ? 'Lab READY' : 'Lab chưa READY'}</p>
                <p role="status" className="text-sm font-medium">{stages[run.state]}</p>
            </div>
            {run.role === 'RED' && (
                <div className="space-y-3">
                    <p>Mục tiêu: xác nhận dịch vụ SSH của bài và mở tình huống điều tra cho đội BLUE.</p>
                    <ol className="list-inside list-decimal text-sm">
                        <li>Bấm Kiểm tra kết nối. Linux client Ubuntu dùng tài khoản của Lab để xác thực tới Target đã phân công.</li>
                        <li>Khi hệ thống xác nhận kết nối, bấm Bắt đầu tình huống mô phỏng.</li>
                        <li>Kiểm tra kết quả phát hành và lịch sử lượt.</li>
                    </ol>
                    <p className="text-sm font-medium">Linux client → Ubuntu Target chạy OpenSSH → Event Collector</p>
                    <p role="status">Kiểm tra kết nối thật: {{
                        NONE: 'Chưa kiểm tra', QUEUED: 'Đang chờ Linux client', CHECKING: 'Đang kiểm tra',
                        SUCCEEDED: 'Đã xác nhận', FAILED: 'Thất bại. Báo giảng viên kiểm tra hoặc cấp phiên Lab mới.',
                    }[run.connectionStatus]}</p>
                    <div className="flex flex-wrap gap-2">
                        <button className={button} disabled={pending || !run.ready || run.connectionStatus !== 'NONE'}
                            onClick={() => void act('CHECK_CONNECTION')}>Kiểm tra kết nối</button>
                        <button className={button} disabled={pending || !run.ready || !run.canStartScenario || run.connectionStatus !== 'SUCCEEDED'}
                            onClick={() => void act('START_SCENARIO')}>{run.state === 'ACTIVE' && run.canStartScenario
                            ? 'Phát hành Event cho lượt hiện tại' : 'Bắt đầu tình huống mô phỏng'}</button>
                    </div>
                    {run.state !== 'APPROVED' && <p role="status">Tình huống đã phát hành cho đội BLUE. Theo dõi tiến độ ở trạng thái phía trên.</p>}
                </div>
            )}
            {run.role !== 'RED' && (
                <>
                    <PracticeEventExplorer events={run.observedEvents} />
                    {run.metrics && <dl className="grid gap-2 text-sm sm:grid-cols-2">
                        <div><dt>Độ trễ xử lý cảnh báo mô phỏng</dt><dd>{metric(run.metrics.systemDetectionMillis, 'ms')}</dd></div>
                        <div><dt>Thời gian học viên nhận diện</dt><dd>{metric(run.metrics.studentDetectionSeconds, 'giây')}</dd></div>
                        <div><dt>Thời gian học viên đề xuất phản ứng</dt><dd>{metric(run.metrics.studentResponseSeconds, 'giây')}</dd></div>
                        <div><dt>Thời gian phản ứng mô phỏng sau duyệt</dt><dd>{metric(run.metrics.simulatedResponseSeconds, 'giây')}</dd></div>
                    </dl>}
                    <details>
                        <summary className="cursor-pointer font-medium">Defensive Tasks · {run.criteria.reduce((total, item) => total + item.maximum, 0)} điểm</summary>
                        {run.criteria.length === 0 && <p className="mt-2 text-sm">Assessment chưa có nhiệm vụ được cấu hình.</p>}
                        <ol className="ml-5 mt-2 list-decimal text-sm">
                            {run.criteria.map((criterion) => <li key={criterion.taskId}>{criterion.name} · {criterion.maximum} điểm</li>)}
                        </ol>
                    </details>
                    {run.alert && <p className="rounded-md bg-amber-50 p-3 text-sm">Alert mô phỏng · Phát hiện cụm xác thực SSH thất bại. Đối chiếu các bản ghi để tìm những lần có liên quan.</p>}
                    {run.observations.length > 0 && <ObservationTable rows={run.observations} selected={selected}
                        toggle={canInvestigate ? toggle : undefined} />}
                    {studentBlue && run.state === 'ACTIVE' && run.alert && !run.submission?.studentAcknowledgedAt && (
                        <button className={button} disabled={pending} onClick={() => void act('ACKNOWLEDGE')}>
                            Xác nhận đã nhận diện sự cố
                        </button>
                    )}
                    {studentBlue && run.state === 'ACTIVE' && run.submission?.studentAcknowledgedAt && (
                        <div className="space-y-2">
                            <label className="block">Tài khoản bị ảnh hưởng<input className={input} value={account} onChange={(e) => setAccount(e.target.value)} /></label>
                            <label className="block">Máy chủ bị ảnh hưởng<input className={input} value={target} onChange={(e) => setTarget(e.target.value)} /></label>
                            <label className="block">IP nguồn nghi vấn<input className={input} value={source} onChange={(e) => setSource(e.target.value)} /></label>
                            <label className="block">Phạm vi ảnh hưởng<textarea className={input} value={impact} onChange={(e) => setImpact(e.target.value)} /></label>
                            <label className="block">Phân tích log<textarea className={input} value={analysis} onChange={(e) => setAnalysis(e.target.value)} /></label>
                            {investigationIssues.length > 0 && (
                                <ul className="list-disc space-y-1 pl-5 text-sm text-slate-700">
                                    {investigationIssues.map((issue) => <li key={String(issue)}>{issue}</li>)}
                                </ul>
                            )}
                            <button className={button} disabled={pending || investigationIssues.length > 0}
                                onClick={() => void act('SAVE_INVESTIGATION', { observationIds: selectedObservations, account, target, source, impact, note: analysis })}>Lưu điều tra</button>
                            <label className="block">Đề xuất phản ứng<textarea className={input} value={proposal} onChange={(e) => setProposal(e.target.value)} /></label>
                            <button className={button} disabled={pending || !run.submission?.analysis || proposal.trim().length < 10}
                                onClick={() => void act('PROPOSE_RESPONSE', { note: proposal })}>Gửi đề xuất</button>
                        </div>
                    )}
                    {run.submission?.analysis && <p className="whitespace-pre-wrap text-sm">Phân tích đã lưu · {run.submission.analysis}</p>}
                    {run.submission?.proposal && <p className="whitespace-pre-wrap text-sm">Đề xuất · {run.submission.proposal}</p>}
                    {run.submission?.responseReview && <p className="whitespace-pre-wrap text-sm">Phản hồi đề xuất · {run.submission.responseReview}</p>}
                    {instructor && run.state === 'PROPOSED' && (
                        <div className="space-y-2">
                            <label className="block">Nhận xét đề xuất<textarea className={input} value={review} onChange={(e) => setReview(e.target.value)} /></label>
                            <div className="flex gap-2">
                                <button className={button} disabled={pending || review.trim().length < 3}
                                    onClick={() => void act('APPROVE_RESPONSE', { note: review })}>Duyệt phản ứng mô phỏng</button>
                                <button className={button} disabled={pending || review.trim().length < 3}
                                    onClick={() => void act('RETURN_RESPONSE', { note: review })}>Trả lại đề xuất</button>
                            </div>
                        </div>
                    )}
                    {run.recovery.length > 0 && <ObservationTable rows={run.recovery} selected={[]} recovery />}
                    {studentBlue && run.state === 'RECOVERY' && (
                        <div className="space-y-2">
                            <label className="block">Kết luận từ log phục hồi
                                <select className={input} value={recovered} onChange={(e) => setRecovered(e.target.value)}>
                                    <option value="">Chọn kết luận</option>
                                    <option value="yes">Xác thực hợp lệ đã hoạt động</option>
                                    <option value="no">Xác thực hợp lệ chưa hoạt động</option>
                                </select>
                            </label>
                            <fieldset className="space-y-2">
                                <legend className="font-medium">Incident Report</legend>
                                {reportKeys.map((key) => (
                                    <label key={key} className="block text-sm">{reportFields[key]}
                                        <textarea className={input} maxLength={2000} value={report[key] ?? ''}
                                            onChange={(event) => setReport((current) => ({ ...current, [key]: event.target.value }))} />
                                    </label>
                                ))}
                            </fieldset>
                            <div className="flex flex-wrap gap-2">
                                <button className={button} disabled={pending}
                                    onClick={() => void act('SAVE_REPORT_DRAFT', { reportSections: report, recovered: recovered ? recovered === 'yes' : null })}>Lưu nháp</button>
                                <button className={button} disabled={pending || !recovered || reportKeys.some((key) => (report[key]?.trim().length ?? 0) < 3)}
                                    onClick={() => void act('SUBMIT_REPORT', { reportSections: report, recovered: recovered === 'yes' })}>Nộp báo cáo và đề xuất đóng sự cố</button>
                            </div>
                        </div>
                    )}
                    {run.submission?.returnReason && <p role="status">Yêu cầu bổ sung · {run.submission.returnReason}</p>}
                    {run.submission?.reportVersion && (
                        <details>
                            <summary className="cursor-pointer font-medium">Incident Report · Phiên bản {run.submission.reportVersion}</summary>
                            <dl className="space-y-2 text-sm">
                                {reportKeys.map((key) => (
                                    <div key={key}><dt className="font-semibold">{reportFields[key]}</dt>
                                        <dd className="whitespace-pre-wrap">{run.submission?.reportSections?.[key]}</dd></div>
                                ))}
                            </dl>
                        </details>
                    )}
                    {run.submission?.report && <p className="whitespace-pre-wrap text-sm">Incident Report · {run.submission.report}</p>}
                    {instructor && run.state === 'SUBMITTED' && (
                        <div className="space-y-2">
                            <ReviewSelect label="Phân tích" value={analysisResult} change={setAnalysisResult} />
                            <ReviewSelect label="Báo cáo" value={reportResult} change={setReportResult} />
                            <label className="block">Nhận xét bài nộp<textarea className={input} value={review} onChange={(e) => setReview(e.target.value)} /></label>
                            <button className={button} disabled={pending || !analysisResult || !reportResult || review.trim().length < 3}
                                onClick={() => void act('CONFIRM_REVIEW', { note: review, analysisAccepted: analysisResult === 'yes', reportAccepted: reportResult === 'yes' })}>Xác nhận kết quả</button>
                            <button type="button" className={button}
                                disabled={pending || run.submission?.confirmedReview?.version !== run.submission?.reportVersion || !run.submission?.confirmedReview}
                                onClick={() => void act('PUBLISH_REVIEW')}>Công bố điểm</button>
                            <button type="button" className={button} disabled={pending || review.trim().length < 3}
                                onClick={() => void act('RETURN_SUBMISSION', { note: review })}>Yêu cầu bổ sung</button>
                        </div>
                    )}
                    {run.state === 'REVIEWED' && (
                        <div className="space-y-2">
                            <h3 className="font-semibold">Điểm thực hành mô phỏng · {run.score}/100</h3>
                            <p>{run.submission?.review}</p>
                            <ul className="text-sm">
                                {run.submission?.marks?.map((mark) => <li key={mark.name}>{mark.name} · {mark.points}/{mark.maximum} · {mark.reason}</li>)}
                            </ul>
                        </div>
                    )}
                    {run.history.length > 0 && (
                        <details>
                            <summary className="cursor-pointer font-medium">Lịch sử thực hành</summary>
                            <ul className="mt-2 text-sm">
                                {run.history.map((item, index) => (
                                    <li key={index}>{new Date(item.createdAt).toLocaleString('vi-VN')} · {item.action}{item.actorId ? ` · Người dùng #${item.actorId}` : ''}{item.detail ? ` · ${item.detail}` : ''}</li>
                                ))}
                            </ul>
                        </details>
                    )}
                </>
            )}
            {run.role === 'RED' && run.history.length > 0 && (
                <ul className="space-y-1 text-sm" aria-label="Lịch sử thao tác RED">
                    {run.history.map((item, index) => (
                        <li key={index}>{new Date(item.createdAt).toLocaleString('vi-VN')} · {item.action}</li>
                    ))}
                </ul>
            )}
            {feedback && <p role="status" className="text-sm">{feedback}</p>}
        </section>
    )
}

function metric(value: number | undefined, unit: string) {
    return value == null ? 'Chưa đo được' : `${value} ${unit}`
}

function PracticeEventExplorer({ events }: { events: ObservedEvent[] }) {
    const [filter, setFilter] = useState('')
    const [outcome, setOutcome] = useState('')
    const [from, setFrom] = useState('')
    const [severity, setSeverity] = useState('')
    const [eventType, setEventType] = useState('')
    const [alertId, setAlertId] = useState('')
    const [page, setPage] = useState(0)
    const normalized = filter.trim().toLowerCase()
    const filtered = events.filter((event) =>
        (!from || new Date(event.eventTimestamp).getTime() >= new Date(from).getTime())
        && (!outcome || event.outcome === outcome)
        && (!severity || event.severity === severity)
        && (!eventType || event.eventType === eventType)
        && (!alertId || String(event.alertId) === alertId)
        && (!normalized || [String(event.id), event.account, event.sourceAddress, event.target].some((value) => value?.toLowerCase().includes(normalized))),
    )
    const pageSize = 15
    const lastPage = Math.max(0, Math.ceil(filtered.length / pageSize) - 1)
    const currentPage = Math.min(page, lastPage)
    return (
        <details>
            <summary className="cursor-pointer font-medium">Nhật ký Event SSH ({events.length})</summary>
            <div className="my-2 grid gap-2 text-sm sm:grid-cols-3">
                <label>Tài khoản, nguồn hoặc máy chủ<input className={input} value={filter} onChange={(e) => setFilter(e.target.value)} /></label>
                <label>Kết quả<select className={input} value={outcome} onChange={(e) => setOutcome(e.target.value)}>
                    <option value="">Tất cả</option><option value="SUCCESS">Thành công</option><option value="FAILURE">Thất bại</option>
                </select></label>
                <label>Từ thời điểm<input className={input} type="datetime-local" value={from} onChange={(e) => setFrom(e.target.value)} /></label>
                <label>Mức độ<select className={input} value={severity} onChange={(e) => { setSeverity(e.target.value); setPage(0) }}>
                    <option value="">Tất cả</option>{[...new Set(events.map((event) => event.severity).filter(Boolean))].map((value) => <option key={value}>{value}</option>)}
                </select></label>
                <label>Loại Event<select className={input} value={eventType} onChange={(e) => { setEventType(e.target.value); setPage(0) }}>
                    <option value="">Tất cả</option>{[...new Set(events.map((event) => event.eventType))].map((value) => <option key={value}>{value}</option>)}
                </select></label>
                <label>Alert<select className={input} value={alertId} onChange={(e) => { setAlertId(e.target.value); setPage(0) }}>
                    <option value="">Tất cả</option>{[...new Set(events.flatMap((event) => event.alertId ? [event.alertId] : []))].map((value) => <option key={value} value={value}>#{value}</option>)}
                </select></label>
            </div>
            {filtered.length === 0 && <p className="text-sm">Chưa có Event phù hợp trong phiên Lab.</p>}
            {filtered.slice(currentPage * pageSize, (currentPage + 1) * pageSize).map((event) => (
                <details className="border-b py-2 text-sm" key={event.id}>
                    <summary className="cursor-pointer">#{event.id} · {new Date(event.eventTimestamp).toLocaleString('vi-VN')} · {event.eventType} · {event.severity ?? '—'} · {event.sourceAddress ?? '—'} · {event.account ?? '—'}@{event.target ?? '—'}</summary>
                    <p>{event.provenance === 'SIMULATED_PRACTICE' ? 'Mô phỏng' : 'Quan sát thực tế'} · {event.outcome ?? '—'}{event.alertId ? ` · Alert #${event.alertId}` : ''}</p>
                    <pre className="overflow-auto whitespace-pre-wrap">{event.raw ?? 'Nhật ký gốc được giới hạn quyền hiển thị.'}</pre>
                </details>
            ))}
            {lastPage > 0 && <div className="flex items-center gap-3 text-sm">
                <button type="button" disabled={currentPage === 0} onClick={() => setPage(currentPage - 1)}>Trang trước</button>
                <span>Trang {currentPage + 1}/{lastPage + 1}</span>
                <button type="button" disabled={currentPage === lastPage} onClick={() => setPage(currentPage + 1)}>Trang sau</button>
            </div>}
        </details>
    )
}

function ReviewSelect({ label, value, change }: { label: string; value: string; change: (value: string) => void }) {
    return (
        <label className="block">{label}
            <select className={input} value={value} onChange={(e) => change(e.target.value)}>
                <option value="">Chọn mức đánh giá</option>
                <option value="yes">Đạt</option>
                <option value="no">Chưa đạt</option>
            </select>
        </label>
    )
}

function ObservationTable({ rows, selected, toggle, recovery = false }: {
    rows: Observation[]
    selected: number[]
    toggle?: (id: number) => void
    recovery?: boolean
}) {
    return (
        <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
                <caption className="text-left font-medium">{recovery ? 'Log phục hồi mô phỏng' : 'Authentication Events mô phỏng'}</caption>
                <thead><tr>{toggle && <th scope="col">Chọn</th>}<th>Bản ghi</th><th>Mốc thời gian</th><th>Nguồn</th><th>Tài khoản</th><th>Target</th><th>Kết quả</th></tr></thead>
                <tbody>
                    {rows.map((row) => (
                        <tr className={`border-b border-slate-100 ${selected.includes(row.id) ? 'bg-sky-50' : ''}`} key={row.id}>
                            {toggle && <td className="py-2 pr-3">
                                <input className="h-4 w-4 cursor-pointer accent-blue-700" aria-label={`Chọn bản ghi ${row.id}`} type="checkbox"
                                    checked={selected.includes(row.id)} onChange={() => toggle(row.id)} />
                            </td>}
                            <td>#{row.id}</td>
                            <td>{row.second} giây</td><td>{row.source}</td><td>{row.account}</td><td>{row.target}</td>
                            <td>{row.outcome === 'SUCCESS' ? 'Thành công' : 'Thất bại'}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
            {toggle && <p role="status" className="mt-2 text-sm">Đã chọn {rows.filter((row) => selected.includes(row.id)).length} bản ghi.</p>}
        </div>
    )
}
