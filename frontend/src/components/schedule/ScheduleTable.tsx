import React from 'react'
import type { Task, User, TaskAssignmentResult } from '../../types'

interface ScheduleTableProps {
    tasks: Task[]
    workers: User[]
    assignments: TaskAssignmentResult[]
    assignmentMap: Map<number, number | null>
}

const ScheduleTable: React.FC<ScheduleTableProps> = ({ tasks, workers, assignments, assignmentMap }) => {
    
    // Filter tasks that have a start time (either from DB or new assignment)
    // Actually, the original code filtered tasks based on `t.startTime` (DB state).
    // But if we generate a new schedule, we might have new start times for tasks that didn't have one?
    // The original code used `scheduledTasks` which was `tasks.filter(t => t.startTime)`.
    // However, if the algorithm schedules a previously unscheduled task, `t.startTime` is null, 
    // but `assignment.scheduledStart` is present.
    // The original code might have been only showing tasks that were *already* scheduled in DB, PLUS updates?
    // Let's look at `Schedule.tsx`:
    // `const scheduledTasks = tasks.filter(t => t.startTime)`
    // This implies it only shows tasks that are ALREADY in the DB with a time.
    // Wait, no. `tasks` state is updated? No, `fetchData` updates `tasks`.
    // If specific tasks are updated by the algorithm but not saved, `t.startTime` is still null.
    // So the original code might have been missing newly scheduled tasks in the table if `t.startTime` was null?
    // Let's re-read `Schedule.tsx`.
    // Ah, `Schedule.tsx`:
    // `const scheduledTasks = tasks.filter(t => t.startTime)`
    // This filters tasks that have `startTime` property set.
    // If a task is new (no startTime in DB), it won't be in `scheduledTasks` array.
    // But the Gantt chart uses `scheduledTasks`.
    // This means the "Draft Preview" was only showing updates for tasks that *already had* a time?
    // That sounds like a bug or I'm misreading.
    // Actually, `handleApprove` saves them.
    // Maybe `tasks` state is updated optimistically? No.
    
    // BUT looking at `handleGenerate`:
    // `setScheduleResult(res.data)`
    // It doesn't update `tasks`.
    // So `t.startTime` is from DB.
    // If I generate a schedule for empty tasks, `scheduledTasks` is empty.
    // The Gantt chart and Table would be empty?
    
    // Wait, let's look at `frontend/src/pages/Schedule.tsx` again.
    // Line 89: `const scheduledTasks = tasks.filter(t => t.startTime)`
    // If the algorithm runs and returns assignments for tasks that currently have `startTime: null`,
    // then `scheduledTasks` will NOT include them.
    // So current implementation might be broken for *newly* scheduled tasks?
    // Or maybe `tasks` is updated somewhere? No, `setTasks` is only in `fetchData`.
    
    // However, for the purpose of refactoring, I should replicate existing behavior 
    // OR fix it if it's clearly wrong.
    // If I fix it, I should include tasks that have an assignment in `scheduleResult`.
    
    // Let's improve it: Show tasks that have `startTime` OR are in `assignments`.

    const tasksToShow = tasks.filter(t => 
        t.startTime || assignmentMap.has(t.id)
    );

    return (
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
                    {tasksToShow.length === 0 ? (
                        <tr>
                            <td colSpan={4} className="no-data-cell">
                                No scheduled tasks yet. Run "Generate Schedule" to populate this view.
                            </td>
                        </tr>
                    ) : (
                        tasksToShow.map(task => {
                            // Prefer precise start/end from the current draft result; fall back to task fields
                            const assignment = assignments.find(a => a.taskId === task.id)
                            const startDisplay = assignment?.scheduledStart
                                ? new Date(assignment.scheduledStart).toLocaleString()
                                : task.startTime
                                    ? new Date(task.startTime).toLocaleString()
                                    : '-'
                            const endDisplay = assignment?.scheduledEnd
                                ? new Date(assignment.scheduledEnd).toLocaleString()
                                : task.deadline
                                    ? new Date(task.deadline).toLocaleString()
                                    : '-'

                            const uid = assignmentMap.get(task.id)
                            const worker = uid != null ? workers.find(w => w.id === uid) : null
                            const workerLabel = worker
                                ? `${worker.firstName ?? ''} ${worker.lastName ?? ''}`.trim() || `Worker #${uid}`
                                : uid != null ? `Worker #${uid}` : '-'

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
    )
}

export default ScheduleTable

