import { useState, useEffect, useCallback } from 'react'
import type { Task, User } from '../types'
import { taskApi, userApi } from '../api'
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

    // Build worker → tasks map
    const workerSchedules: WorkerSchedule[] = workers.map(w => ({
        worker: w,
        tasks: scheduledTasks.filter(t => t.assignedWorkerId === w.id),
    }))

    const unassignedTasks = tasks.filter(t => !t.assignedWorkerId)
    const unscheduledTasks = tasks.filter(t => t.assignedWorkerId && !t.startTime)

    // Simulate generate (will call real endpoint when backend is ready)
    async function handleGenerate() {
        setIsGenerating(true)
        setError(null)
        setSuccessMsg(null)
        try {
            // TODO: replace with real API call when backend algorithm is ready
            // await scheduleApi.generate()
            await new Promise(r => setTimeout(r, 1500)) // simulate delay
            setSuccessMsg('⚠️ Algorithm not yet implemented on the backend. This page is ready to connect.')
            await fetchData()
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
                        <button
                            className="btn-generate"
                            onClick={handleGenerate}
                            disabled={isGenerating}
                        >
                            {isGenerating ? '⏳ Generating...' : '⚡ Generate Schedule'}
                        </button>
                    )}
                </div>
            </div>

            {/* Messages */}
            {error      && <div className="alert alert-error">{error}</div>}
            {successMsg && <div className="alert alert-info">{successMsg}</div>}

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
                                                title={`${task.title}\nStatus: ${task.statusName}\nPriority: ${task.priorityName}\nDuration: ${task.durationHours}h`}
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
                                        <td>{task.assignedWorkerName ?? '—'}</td>
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
                                            <span className={`status-badge status-${task.statusName?.toLowerCase().replace('_', '-')}`}>
                                                {task.statusName ?? '—'}
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

