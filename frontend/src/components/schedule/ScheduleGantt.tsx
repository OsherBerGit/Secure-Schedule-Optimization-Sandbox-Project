import React, { useMemo } from 'react'
import type { Task, User } from '../../types'
import { getPriorityColor } from '../../utils/scheduleUtils'

interface ScheduleGanttProps {
    tasks: Task[]
    workers: User[]
    assignmentMap: Map<number, number | null>
}

const ScheduleGantt: React.FC<ScheduleGanttProps> = ({ tasks, workers, assignmentMap }) => {
    
    // Memoize calculations to prevent re-renders
    const { minDate, maxDate, totalMs, workerSchedules } = useMemo(() => {
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

        const workerSchedules = workers.map(w => ({
            worker: w,
            tasks: scheduledTasks.filter(t => assignmentMap.get(t.id) === w.id),
        }))

        return { minDate, maxDate, totalMs, workerSchedules }
    }, [tasks, workers, assignmentMap])

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

    return (
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
        </div>
    )
}

export default ScheduleGantt

