import { useState, useEffect, useCallback } from 'react'
import type { Task, User, ScheduleStrategy, ScheduleResult } from '../types'
import { taskApi, userApi, scheduleApi } from '../api'
import { useAuth } from '../context/useAuth'
import './Schedule.css'

// Group tasks by assigned worker for display
interface WorkerSchedule {
    worker: User
    tasks: Task[]
}

const Schedule = () => {
    const { user: currentUser } = useAuth()
    const [tasks, setTasks] = useState<Task[]>([])
    const [workers, setWorkers] = useState<User[]>([])
    const [isLoading, setIsLoading] = useState(false)
    const [isGenerating, setIsGenerating] = useState(false)
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

    useEffect(() => { void fetchData() }, [fetchData])

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
            const res = await scheduleApi.run(strategy)
            setScheduleResult(res.data)
            setSuccessMsg(
                `✅ Schedule generated using ${res.data.strategyUsed} — ` +
                `${res.data.assignedTasks} assigned, ${res.data.unassignedTasks} unassigned.`
            )
            await fetchData() // refresh tasks with new assignments
        } catch (err: unknown) {
            setError(err instanceof Error ? err.message : 'Failed to generate schedule')
        } finally {
            setIsGenerating(false)
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
                            <button
                                className="btn-generate"
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
                    <h3 className="result-title">📊 Last Run — {scheduleResult.strategyUsed}</h3>
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
                </div>
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

                    ) : (

                        /* ---- TABLE VIEW ---- */
                        <table className="schedule-table">
                            <thead>
                                <tr>
                                    <th>Task</th>
                                    <th>Assigned Worker</th>
                                    <th>Start Time</th>
                                    <th>Deadline</th>
                                    <th>Duration</th>
                                    <th>Priority</th>
                                    <th>Status</th>
                                </tr>
                            </thead>
                            <tbody>
                                {scheduledTasks.map(task => (
                                    <tr key={task.id}>
                                        <td className="task-title-cell">{task.title}</td>
                                        <td>{
                                            (() => {
                                                const uid = assignmentMap.get(task.id)
                                                if (uid == null) return '—'
                                                const w = workers.find(w => w.id === uid)
                                                return w ? `${w.firstName ?? ''} ${w.lastName ?? ''}`.trim() || `Worker #${uid}` : `Worker #${uid}`
                                            })()
                                        }</td>
                                        <td>{task.startTime ? new Date(task.startTime).toLocaleString() : '—'}</td>
                                        <td>{task.deadline ? new Date(task.deadline).toLocaleString() : '—'}</td>
                                        <td>{task.durationHours != null ? `${task.durationHours}h` : '—'}</td>
                                        <td>
                                            <span
                                                className="priority-dot"
                                                style={{ background: getPriorityColor(task.priorityName) }}
                                            />
                                            {task.priorityName ?? '—'}
                                        </td>
                                        <td>
                                            <span className={`status-badge status-${task.taskStatusName?.toLowerCase().replace('_', '-')}`}>
                                                {task.taskStatusName ?? '—'}
                                            </span>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
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
                </>
            )}
        </div>
    )
}

export default Schedule

