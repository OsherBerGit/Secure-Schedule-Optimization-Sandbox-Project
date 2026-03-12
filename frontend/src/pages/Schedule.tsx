import { useState, useEffect, useCallback } from 'react'
import type { Task, User, Department, ScheduleStrategy, ScheduleResult, UnscheduledTaskResult } from '../types'
import { taskApi, userApi, departmentApi, scheduleApi } from '../api'
import { useAuth } from '../context/useAuth'
import FitnessChart from '../components/FitnessChart'
import './Schedule.css'

// Group tasks by assigned worker for display
interface WorkerSchedule {
    worker: User
    tasks: Task[]
}

/**
 * Strips ugly algorithm-internal prefixes from reason strings while preserving
 * the detailed message (worker IDs, days, times) from the backend.
 *
 * Examples:
 *   "Memetic: constraint violation during decode — Worker 3 unavailable on MONDAY"
 *     → "Worker 3 unavailable on MONDAY"
 *   "Greedy: no eligible worker found for task"
 *     → "No eligible worker found for task"
 *   "Worker shift conflict: availability window 09:00–17:00 does not cover task"
 *     → "Worker shift conflict: availability window 09:00–17:00 does not cover task"
 */
function formatReason(rawReason: string): string {
    if (!rawReason) return 'Unknown reason'

    // Strip known algorithm prefixes (case-insensitive), including the separator that follows
    const prefixPattern = /^(memetic|greedy|round.?robin)\s*:\s*(constraint violation during decode\s*[—–-]+\s*|[^—–]*(decode|scheduling)\s*[—–-]+\s*)?/i
    const stripped = rawReason.replace(prefixPattern, '').trim()

    const result = stripped.length > 0 ? stripped : rawReason.trim()

    // Capitalise the first letter only
    return result.charAt(0).toUpperCase() + result.slice(1)
}

const Schedule = () => {
    const { user: currentUser } = useAuth()
    const [tasks, setTasks] = useState<Task[]>([])
    const [workers, setWorkers] = useState<User[]>([])
    const [departments, setDepartments] = useState<Department[]>([])
    const [selectedDepartmentId, setSelectedDepartmentId] = useState<number | null>(null)
    const [isLoading, setIsLoading] = useState(false)
    const [isGenerating, setIsGenerating] = useState(false)
    const [isSaving, setIsSaving] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [successMsg, setSuccessMsg] = useState<string | null>(null)
    const [viewMode, setViewMode] = useState<'gantt' | 'table'>('gantt')
    const [strategy, setStrategy] = useState<ScheduleStrategy>('GREEDY')
    const [scheduleResult, setScheduleResult] = useState<ScheduleResult | null>(null)

    const fetchData = useCallback(async () => {
        setIsLoading(true)
        try {
            const [tasksRes, workersRes] = await Promise.all([
                taskApi.getAll(),
                userApi.getByRole('WORKER'),
            ])
            setTasks(tasksRes.data)
            setWorkers(workersRes.data)
        } catch (err: unknown) {
            setError(err instanceof Error ? err.message : 'Failed to load schedule data')
        } finally {
            setIsLoading(false)
        }
    }, [])

    useEffect(() => {
        void fetchData()
        departmentApi.getAll()
            .then(res => setDepartments(res.data))
            .catch(() => { /* non-fatal — dropdown will be empty */ })
    }, [fetchData])

    // --- Gantt helpers ---

    // Find the earliest and latest dates across all tasks with a startTime
    const scheduledTasks = tasks.filter(t => t.startTime)

    const allDates = scheduledTasks.flatMap(t => [
        t.startTime ? new Date(t.startTime) : null,
        t.deadline  ? new Date(t.deadline)  : null,
    ]).filter(Boolean) as Date[]

    const minDate = allDates.length > 0
        ? new Date(Math.min(...allDates.map(d => d.getTime())))
        : new Date()

    const maxDate = allDates.length > 0
        ? new Date(Math.max(...allDates.map(d => d.getTime())))
        : new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)

    const totalMs = maxDate.getTime() - minDate.getTime() || 1

    function getBarStyle(task: Task) {
        if (!task.startTime) return {}
        const start = new Date(task.startTime).getTime()
        const end = task.deadline
            ? new Date(task.deadline).getTime()
            : start + (task.durationHours ?? 8) * 3600 * 1000

        const left  = ((start - minDate.getTime()) / totalMs) * 100
        const width = Math.max(((end - start) / totalMs) * 100, 2)
        return { left: `${left}%`, width: `${width}%` }
    }

    function getPriorityColor(priorityName: string | null) {
        switch (priorityName?.toUpperCase()) {
            case 'HIGH':     return '#e74c3c'
            case 'MEDIUM':   return '#f39c12'
            case 'LOW':      return '#27ae60'
            default:         return '#667eea'
        }
    }

    // Build a taskId → assignedUserId map from the latest schedule result
    const assignmentMap = new Map<number, number | null>(
        scheduleResult?.assignments.map(a => [a.taskId, a.assignedUserId]) ?? []
    )

    // Build worker → tasks map using the assignment map
    const workerSchedules: WorkerSchedule[] = workers.map(w => ({
        worker: w,
        tasks: scheduledTasks.filter(t => assignmentMap.get(t.id) === w.id),
    }))

    const unassignedTasks  = tasks.filter(t => !assignmentMap.has(t.id) || assignmentMap.get(t.id) == null)
    const unscheduledTasks = tasks.filter(t => assignmentMap.get(t.id) != null && !t.startTime)

    // Trigger the scheduling algorithm
    async function handleGenerate() {
        setIsGenerating(true)
        setError(null)
        setSuccessMsg(null)
        setScheduleResult(null)
        try {
            const res = await scheduleApi.run(strategy, selectedDepartmentId)
            console.log('Fitness History:', res.data.fitnessHistory)
            setScheduleResult(res.data)
            setSuccessMsg(
                `✅ Draft generated using ${res.data.strategyUsed} — ` +
                `${res.data.assignedTasks} assigned, ${res.data.unassignedTasks} unassigned. ` +
                `Review below and click "Approve & Save" to persist.`
            )
            await fetchData()
        } catch (err: unknown) {
            setError(err instanceof Error ? err.message : 'Failed to generate schedule')
        } finally {
            setIsGenerating(false)
        }
    }

    // Persist the approved draft to the database
    async function handleApprove() {
        if (!scheduleResult) return
        setIsSaving(true)
        setError(null)
        setSuccessMsg(null)
        try {
            await scheduleApi.save({
                assignments: scheduleResult.assignments.map(a => ({
                    taskId: a.taskId,
                    assignedUserId: a.assignedUserId ?? null,
                    scheduledStart: a.scheduledStart ?? null,
                    scheduledEnd: a.scheduledEnd ?? null,
                })),
            })
            setSuccessMsg(
                `✅ Schedule approved and saved — ${scheduleResult.assignedTasks} task(s) scheduled.`
            )
            setScheduleResult(null) // clear draft state after saving
            await fetchData()       // refresh task list to reflect new SCHEDULED statuses
        } catch (err: unknown) {
            setError(err instanceof Error ? err.message : 'Failed to save schedule')
        } finally {
            setIsSaving(false)
        }
    }

    return (
        <div className="schedule-container">

            {/* Header */}
            <div className="schedule-header">
                <div className="schedule-title">
                    <h1>📅 Schedule</h1>
                    <p className="schedule-subtitle">
                        {scheduledTasks.length} of {tasks.length} tasks scheduled
                    </p>
                </div>
                <div className="schedule-actions">
                    <div className="view-toggle">
                        <button
                            className={viewMode === 'gantt' ? 'active' : ''}
                            onClick={() => setViewMode('gantt')}
                        >
                            Gantt
                        </button>
                        <button
                            className={viewMode === 'table' ? 'active' : ''}
                            onClick={() => setViewMode('table')}
                        >
                            Table
                        </button>
                    </div>
                    {currentUser?.role === 'ADMIN' && (
                        <div className="schedule-generate-group">
                            <select
                                className="strategy-select"
                                value={strategy}
                                onChange={e => setStrategy(e.target.value as ScheduleStrategy)}
                                disabled={isGenerating}
                            >
                                <option value="GREEDY">⚡ Greedy (Best-Fit)</option>
                                <option value="ROUND_ROBIN">🔄 Round-Robin (Fair)</option>
                                <option value="MEMETIC">🧬 Memetic (Optimised)</option>
                            </select>
                            <select
                                className="strategy-select"
                                value={selectedDepartmentId ?? ''}
                                onChange={e => setSelectedDepartmentId(
                                    e.target.value === '' ? null : Number(e.target.value)
                                )}
                                disabled={isGenerating}
                                title="Scope scheduling to a specific department"
                            >
                                <option value="">🌐 All Departments</option>
                                {departments.map(d => (
                                    <option key={d.id} value={d.id}>🏢 {d.name}</option>
                                ))}
                            </select>
                            <button
                                className="btn-generate-cta"
                                onClick={handleGenerate}
                                disabled={isGenerating}
                            >
                                {isGenerating ? '⏳ Generating...' : '⚡ Generate Schedule'}
                            </button>
                        </div>
                    )}
                </div>
            </div>

            {/* Messages */}
            {error      && <div className="alert alert-error">{error}</div>}
            {successMsg && <div className="alert alert-info">{successMsg}</div>}

            {/* Algorithm Result Panel */}
            {scheduleResult && (
                <div className="result-panel">
                    <h3 className="result-title">📊 Draft Preview — {scheduleResult.strategyUsed}</h3>
                    <div className="result-stats">
                        <div className="result-stat">
                            <span className="stat-value">{scheduleResult.totalTasks}</span>
                            <span className="stat-label">Total</span>
                        </div>
                        <div className="result-stat result-stat--success">
                            <span className="stat-value">{scheduleResult.assignedTasks}</span>
                            <span className="stat-label">Assigned</span>
                        </div>
                        <div className="result-stat result-stat--warn">
                            <span className="stat-value">{scheduleResult.unassignedTasks}</span>
                            <span className="stat-label">Unassigned</span>
                        </div>
                    </div>
                    <div className="result-assignments">
                        {scheduleResult.assignments.map(a => (
                            <div key={a.taskId} className={`result-row ${a.assignedUserId ? 'assigned' : 'unassigned'}`}>
                                <span className="result-task">{a.taskTitle}</span>
                                <span className="result-arrow">→</span>
                                <span className="result-user">
                                    {a.assignedUserFullName ?? '⚠️ Unassigned'}
                                </span>
                                {a.scheduledStart && (
                                    <span className="result-dates">
                                        {new Date(a.scheduledStart).toLocaleDateString()} –{' '}
                                        {a.scheduledEnd ? new Date(a.scheduledEnd).toLocaleDateString() : '?'}
                                    </span>
                                )}
                                <span className="result-reason">{a.reason}</span>
                            </div>
                        ))}
                    </div>

                    {/* Approve & Save — only visible to ADMIN after a draft is generated */}
                    {currentUser?.role === 'ADMIN' && (
                        <div className="result-approve-row">
                            <p className="result-approve-hint">
                                ℹ️ This is a <strong>draft preview</strong>. Nothing has been saved yet.
                                Click the button below to commit these assignments to the database.
                            </p>
                            <button
                                className="btn-approve"
                                onClick={handleApprove}
                                disabled={isSaving || scheduleResult.assignedTasks === 0}
                            >
                                {isSaving ? '💾 Saving...' : '✅ Approve & Save Schedule'}
                            </button>
                        </div>
                    )}
                </div>
            )}

            {/* ── Convergence Graph — shown after every Memetic run, regardless of view mode ── */}
            {scheduleResult?.fitnessHistory && scheduleResult.fitnessHistory.length > 0 && (
                <FitnessChart fitnessHistory={scheduleResult.fitnessHistory} />
            )}

            {isLoading ? (
                <div className="loading">Loading schedule data...</div>
            ) : (
                <>
                    {/* Summary Cards */}
                    <div className="summary-cards">
                        <div className="summary-card">
                            <span className="card-value">{tasks.length}</span>
                            <span className="card-label">Total Tasks</span>
                        </div>
                        <div className="summary-card">
                            <span className="card-value">{scheduledTasks.length}</span>
                            <span className="card-label">Scheduled</span>
                        </div>
                        <div className="summary-card">
                            <span className="card-value">{unscheduledTasks.length}</span>
                            <span className="card-label">Unscheduled</span>
                        </div>
                        <div className="summary-card">
                            <span className="card-value">{unassignedTasks.length}</span>
                            <span className="card-label">Unassigned</span>
                        </div>
                        <div className="summary-card">
                            <span className="card-value">{workers.length}</span>
                            <span className="card-label">Workers</span>
                        </div>
                    </div>

                    {scheduledTasks.length === 0 ? (
                        <div className="empty-state">
                            <div className="empty-icon">📋</div>
                            <h3>No scheduled tasks yet</h3>
                            <p>
                                {currentUser?.role === 'ADMIN'
                                    ? 'Click "Generate Schedule" to run the scheduling algorithm.'
                                    : 'The admin has not generated a schedule yet.'}
                            </p>
                        </div>
                    ) : viewMode === 'gantt' ? (

                        /* ---- GANTT VIEW ---- */
                        <div className="gantt-view-section">
                            <div className="gantt-wrapper">
                                <div className="gantt-legend">
                                    <span>🔴 High</span>
                                    <span>🟡 Medium</span>
                                    <span>🟢 Low</span>
                                </div>

                                {/* Date axis */}
                                <div className="gantt-axis">
                                    <div className="gantt-label-col" />
                                    <div className="gantt-bar-col">
                                        <div className="date-start">{minDate.toLocaleDateString()}</div>
                                        <div className="date-end">{maxDate.toLocaleDateString()}</div>
                                    </div>
                                </div>

                                {/* Rows per worker */}
                                {workerSchedules.filter(ws => ws.tasks.length > 0).map(ws => (
                                    <div key={ws.worker.id} className="gantt-row">
                                        <div className="gantt-label-col">
                                            <div className="worker-name">
                                                {ws.worker.firstName} {ws.worker.lastName}
                                            </div>
                                            <div className="worker-task-count">
                                                {ws.tasks.length} task{ws.tasks.length !== 1 ? 's' : ''}
                                            </div>
                                        </div>
                                        <div className="gantt-bar-col">
                                            {ws.tasks.map(task => (
                                                <div
                                                    key={task.id}
                                                    className="gantt-bar"
                                                    style={{
                                                        ...getBarStyle(task),
                                                        background: getPriorityColor(task.priorityName),
                                                    }}
                                                    title={`${task.title}\nStatus: ${task.taskStatusName}\nPriority: ${task.priorityName}\nDuration: ${task.durationHours}h`}
                                                >
                                                    <span className="bar-label">{task.title}</span>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                ))}
                            </div>

                            {/* Convergence chart is rendered below, outside the gantt/table toggle */}
                        </div>

                    ) : (

                        /* ---- TABLE VIEW ---- */
                        <div className="schedule-table-wrapper">
                            <table className="schedule-table">
                                <thead>
                                    <tr>
                                        <th>Task Title</th>
                                        <th>Assigned Worker</th>
                                        <th>Start Time</th>
                                        <th>End Time</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {scheduledTasks.length === 0 ? (
                                        <tr>
                                            <td colSpan={4} className="no-data-cell">
                                                No scheduled tasks yet. Run "Generate Schedule" to populate this view.
                                            </td>
                                        </tr>
                                    ) : (
                                        scheduledTasks.map(task => {
                                            // Prefer precise start/end from the current draft result; fall back to task fields
                                            const assignment = scheduleResult?.assignments.find(a => a.taskId === task.id)
                                            const startDisplay = assignment?.scheduledStart
                                                ? new Date(assignment.scheduledStart).toLocaleString()
                                                : task.startTime
                                                    ? new Date(task.startTime).toLocaleString()
                                                    : '—'
                                            const endDisplay = assignment?.scheduledEnd
                                                ? new Date(assignment.scheduledEnd).toLocaleString()
                                                : task.deadline
                                                    ? new Date(task.deadline).toLocaleString()
                                                    : '—'

                                            const uid = assignmentMap.get(task.id)
                                            const worker = uid != null ? workers.find(w => w.id === uid) : null
                                            const workerLabel = worker
                                                ? `${worker.firstName ?? ''} ${worker.lastName ?? ''}`.trim() || `Worker #${uid}`
                                                : uid != null ? `Worker #${uid}` : '—'

                                            return (
                                                <tr key={task.id}>
                                                    <td className="task-title-cell">{task.title}</td>
                                                    <td>{workerLabel}</td>
                                                    <td>{startDisplay}</td>
                                                    <td>{endDisplay}</td>
                                                </tr>
                                            )
                                        })
                                    )}
                                </tbody>
                            </table>
                        </div>
                    )}

                    {/* Unscheduled tasks warning */}
                    {unscheduledTasks.length > 0 && (
                        <div className="unscheduled-panel">
                            <h3>⚠️ Assigned but not yet scheduled ({unscheduledTasks.length})</h3>
                            <div className="unscheduled-list">
                                {unscheduledTasks.map(t => (
                                    <span key={t.id} className="unscheduled-tag">{t.title}</span>
                                ))}
                            </div>
                        </div>
                    )}

                    {/* ---- EXPLAINABILITY PANEL ---- */}
                    {scheduleResult?.unscheduledTasks && scheduleResult.unscheduledTasks.length > 0 && (
                        <div className="explain-panel">
                            <div className="explain-header">
                                <span className="explain-icon">⚠️</span>
                                <div>
                                    <h3 className="explain-title">Unscheduled Tasks</h3>
                                    <p className="explain-subtitle">
                                        {scheduleResult.unscheduledTasks.length} task
                                        {scheduleResult.unscheduledTasks.length !== 1 ? 's' : ''} could not
                                        be assigned — review the reasons below and adjust constraints or worker availability.
                                    </p>
                                </div>
                            </div>
                            <table className="explain-table">
                                <thead>
                                    <tr>
                                        <th className="explain-th explain-th--task">Task</th>
                                        <th className="explain-th explain-th--reason">Why it was skipped</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {scheduleResult.unscheduledTasks.map((u: UnscheduledTaskResult) => (
                                        <tr key={u.taskId} className="explain-row">
                                            <td className="explain-td explain-td--task">
                                                <span className="explain-task-name">{u.taskName}</span>
                                                <span className="explain-task-id">#{u.taskId}</span>
                                            </td>
                                            <td className="explain-td explain-td--reason">
                                                <span className="explain-reason-badge">{formatReason(u.reason)}</span>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </>
            )}
        </div>
    )
}

export default Schedule

