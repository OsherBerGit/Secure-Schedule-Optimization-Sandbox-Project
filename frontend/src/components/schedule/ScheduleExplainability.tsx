import React, { useState, useEffect } from 'react'
import type { UnscheduledTaskResult } from '../../types'
import { formatReason } from '../../utils/scheduleUtils'
import { AlertCircle, ChevronRight } from 'lucide-react'
import './ScheduleExplainability.css'

const ScheduleExplainability = ({ failures }: { failures: UnscheduledTaskResult[] }) => {
    const [isDarkMode, setIsDarkMode] = useState(false)

    useEffect(() => {
        const checkDarkMode = () => setIsDarkMode(document.documentElement.classList.contains('dark'))
        checkDarkMode()
        const observer = new MutationObserver(checkDarkMode)
        observer.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] })
        return () => observer.disconnect()
    }, [])

    if (!failures?.length) return null

    return (
        <div className="ex-container">
            <div
                className="ex-header"
                style={isDarkMode ? { backgroundColor: 'rgba(245, 158, 11, 0.15)', borderColor: 'rgba(245, 158, 11, 0.3)' } : {}}
            >
                <div className="ex-header-title-row">
                    <AlertCircle className="ex-icon" size={20} />
                    <h3>Optimization Insights</h3>
                </div>
                <p className="ex-subtitle">Resource conflicts or constraints prevented these tasks from being scheduled.</p>
            </div>

            <div className="ex-table-wrapper">
                <table className="ex-table">
                    <thead>
                    <tr>
                        <th>Target Task</th>
                        <th>Reason for Exclusion</th>
                    </tr>
                    </thead>
                    <tbody>
                    {failures.map(u => (
                        <tr key={u.taskId} className="ex-tr">
                            <td className="ex-td">
                                <div className="ex-task-box">
                                    <span className="ex-task-name">{u.taskName}</span>
                                    <span className="ex-task-id">ID: #{u.taskId}</span>
                                </div>
                            </td>
                            <td className="ex-td">
                                <div className="ex-reason-box">
                                    <ChevronRight size={14} className="ex-chevron" />
                                    <span className="ex-reason-text">{formatReason(u.reason)}</span>
                                </div>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    )
}

export default ScheduleExplainability