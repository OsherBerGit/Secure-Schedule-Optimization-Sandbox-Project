import React from 'react'
import type { UnscheduledTaskResult } from '../../types'
import { formatReason } from '../../utils/scheduleUtils'

interface ScheduleExplainabilityProps {
    failures: UnscheduledTaskResult[]
}

const ScheduleExplainability: React.FC<ScheduleExplainabilityProps> = ({ failures }) => {
    if (!failures || failures.length === 0) return null

    return (
        <div className="explain-panel">
            <div className="explain-header">
                <span className="explain-icon">⚠️</span>
                <div>
                    <h3 className="explain-title">Unscheduled Tasks</h3>
                    <p className="explain-subtitle">
                        {failures.length} task
                        {failures.length !== 1 ? 's' : ''} could not
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
                    {failures.map((u: UnscheduledTaskResult) => (
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
    )
}

export default ScheduleExplainability

