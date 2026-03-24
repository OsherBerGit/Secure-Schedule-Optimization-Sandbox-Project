import React from 'react'
import type { Task } from '../../types'

interface UnscheduledWarningProps {
    tasks: Task[]
}

const UnscheduledWarning: React.FC<UnscheduledWarningProps> = ({ tasks }) => {
    if (!tasks || tasks.length === 0) return null

    return (
        <div className="unscheduled-panel">
            <h3>⚠️ Assigned but not yet scheduled ({tasks.length})</h3>
            <div className="unscheduled-list">
                {tasks.map(t => (
                    <span key={t.id} className="unscheduled-tag">{t.title}</span>
                ))}
            </div>
        </div>
    )
}

export default UnscheduledWarning

