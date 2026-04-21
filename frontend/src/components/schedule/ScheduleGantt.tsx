import React, { useMemo } from 'react'
import type { Task, User } from '../../types'
import { getPriorityColor } from '../../utils/scheduleUtils'
import { Users } from 'lucide-react'
import './ScheduleGantt.css'

interface ScheduleGanttProps {
    tasks: Task[]
    users: User[]
    assignmentMap: Map<number, number | null>
    onTaskClick?: (task: Task) => void
}

const ScheduleGantt: React.FC<ScheduleGanttProps> = ({ tasks, users, assignmentMap, onTaskClick }) => {
    const { minDate, maxDate, totalMs, userSchedules, timelineTicks } = useMemo(() => {
        const scheduledTasks = tasks.filter(t => t.startTime)
        if (scheduledTasks.length === 0) return { minDate: new Date(), maxDate: new Date(), totalMs: 1, userSchedules: [], timelineTicks: [] }

        const allDates = scheduledTasks.flatMap(t => [
            new Date(t.startTime!),
            t.deadline ? new Date(t.deadline) : new Date(new Date(t.startTime!).getTime() + (t.durationHours || 1) * 3600000)
        ])

        const min = new Date(Math.min(...allDates.map(d => d.getTime())))
        min.setHours(0, 0, 0, 0)
        const max = new Date(Math.max(...allDates.map(d => d.getTime())))
        max.setHours(23, 59, 59, 999)

        const ticks = []
        const curr = new Date(min)
        while (curr <= max) {
            ticks.push(new Date(curr));
            curr.setDate(curr.getDate() + 1);
        }

        const schedules = users.map(w => ({
            user: w,
            userTasks: scheduledTasks.filter(t => assignmentMap.get(t.id) === w.id)
        })).filter(ws => ws.userTasks.length > 0)

        return { minDate: min, maxDate: max, totalMs: max.getTime() - min.getTime() || 1, userSchedules: schedules, timelineTicks: ticks }
    }, [tasks, users, assignmentMap])

    if (userSchedules.length === 0) return null

    return (
        <div className="gn-container">
            <div className="gn-toolbar">
                <div className="gn-legend">
                    <div className="gn-legend-item"><span className="gn-dot" style={{backgroundColor: '#ef4444'}}></span> High</div>
                    <div className="gn-legend-item"><span className="gn-dot" style={{backgroundColor: '#f59e0b'}}></span> Medium</div>
                    <div className="gn-legend-item"><span className="gn-dot" style={{backgroundColor: '#10b981'}}></span> Low</div>
                </div>
            </div>

            <div className="gn-scroll-area">
                <div className="gn-canvas" style={{ minWidth: Math.max(timelineTicks.length * 120, 1000), position: 'relative' }}>

                    <div className="gn-header-row" style={{ display: 'flex', position: 'sticky', top: 0, zIndex: 30 }}>
                        <div className="gn-user-label-header">
                            <Users size={14} /> Users
                        </div>
                        <div className="gn-timeline-header" style={{ display: 'flex', flex: 1 }}>
                            {timelineTicks.map((date, i) => (
                                <div key={i} className="gn-tick-cell">
                                    <div className="gn-tick-day">{date.toLocaleDateString('en-US', { weekday: 'short' })}</div>
                                    <div className="gn-tick-date">{date.getDate()}/{date.getMonth() + 1}</div>
                                </div>
                            ))}
                        </div>
                    </div>

                    <div className="gn-body">
                        {userSchedules.map(ws => (
                            <div key={ws.user.id} className="gn-row" style={{ display: 'flex', position: 'relative', borderBottom: '1px solid #f1f5f9', minHeight: '60px' }}>
                                <div className="gn-user-side-info">
                                    <div className="gn-user-name-main">{ws.user.firstName} {ws.user.lastName}</div>
                                    <div className="gn-user-task-count">{ws.userTasks.length} tasks</div>
                                </div>

                                <div className="gn-bars-container" style={{ position: 'relative', flex: 1, display: 'flex' }}>
                                    {timelineTicks.map((_, i) => (
                                        <div key={i} className="gn-grid-line" style={{ flex: 1, borderRight: '1px solid #f8fafc' }} />
                                    ))}

                                    {ws.userTasks.map(task => {
                                        const startTs = new Date(task.startTime!).getTime()
                                        const endTs = task.deadline ? new Date(task.deadline).getTime() : startTs + (task.durationHours || 1) * 3600000
                                        const leftPercent = ((startTs - minDate.getTime()) / totalMs) * 100
                                        const widthPercent = Math.max(((endTs - startTs) / totalMs) * 100, 2)

                                        return (
                                            <div
                                                key={task.id}
                                                className="gn-task-bar"
                                                style={{
                                                    position: 'absolute',
                                                    left: `${leftPercent}%`,
                                                    width: `${widthPercent}%`,
                                                    backgroundColor: getPriorityColor(task.priorityName),
                                                    top: '50%',
                                                    transform: 'translateY(-50%)',
                                                    zIndex: 10,
                                                    height: '32px'
                                                }}
                                                onClick={() => onTaskClick?.(task)}
                                            >
                                                <span className="gn-bar-label">{task.title}</span>
                                            </div>
                                        )
                                    })}
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    )
}

export default ScheduleGantt