import { useState } from 'react'
import type { FormEvent } from 'react'
import type { CreateSettlementRequest, Task, User, Settlement } from '../types'
import { settlementApi } from '../api'

interface SettlementModalProps {
    tasks: Task[]
    workers: User[]
    settlements: Settlement[]
    onSuccess: () => void
    onClose: () => void
}

const SettlementModal = ({ tasks: initialTasks, workers: initialWorkers, settlements, onSuccess, onClose }: SettlementModalProps) => {
    // We compute the filtered lists dynamically
    const [taskId, setTaskId] = useState<number | ''>('')
    const [workerId, setWorkerId] = useState<number | ''>('')
    const [settlementDate, setSettlementDate] = useState('')
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [successMessage, setSuccessMessage] = useState<string | null>(null)

    // derived filtered lists
    const filteredWorkers = initialWorkers.filter(w => {
        // Calculate active assignments
        const activeAssignmentsCount = settlements.filter(s => s.workerId === w.id && s.statusName !== 'COMPLETED').length;
        if (w.maxTasks !== null && w.maxTasks !== undefined && activeAssignmentsCount >= w.maxTasks) {
            return false;
        }

        if (!taskId) return true
        const selectedTask = initialTasks.find(t => t.id === taskId)
        if (!selectedTask || !selectedTask.requiredSkills) return true
        // Worker must possess ALL required skills
        const workerSkillIds = w.skills?.map(s => s.id) || []
        return selectedTask.requiredSkills.every(skill => workerSkillIds.includes(skill.id))
    })

    const filteredTasks = initialTasks.filter(t => {
        // Exclude tasks that already have an active settlement or are not 'OPEN'
        const hasActiveSettlement = settlements.some(s => s.taskId === t.id && s.statusName !== 'COMPLETED');
        if (hasActiveSettlement || (t.taskStatusName && t.taskStatusName !== 'OPEN')) {
            return false;
        }

        if (!workerId) return true
        const selectedWorker = initialWorkers.find(w => w.id === workerId)
        if (!selectedWorker || !selectedWorker.skills) return true
        // Task requires only skills the worker possesses
        if (!t.requiredSkills) return true
        const workerSkillIds = selectedWorker.skills.map(s => s.id) || []
        return t.requiredSkills.every(skill => workerSkillIds.includes(skill.id))
    })

    // Auto-select if there's only one option after filtering or current selection is invalid
    // But keep it simple and just clear selection if it becomes invalid.
    if (taskId && !filteredTasks.some(t => t.id === taskId)) {
        setTaskId('')
    }
    if (workerId && !filteredWorkers.some(w => w.id === workerId)) {
        setWorkerId('')
    }

    async function handleSubmit(e: FormEvent) {
        e.preventDefault()
        if (!taskId || !workerId) return

        setIsSubmitting(true)
        setError(null)
        setSuccessMessage(null)

        try {
            const toIso = (val: string) => val ? val + ':00' : val
            const data: CreateSettlementRequest = {
                taskId: Number(taskId),
                workerId: Number(workerId),
                settlementDate: toIso(settlementDate),
                completionDate: undefined,
            }
            await settlementApi.create(data)

            // Show success message briefly before closing
            setSuccessMessage('Settlement assigned successfully!')
            setTimeout(() => {
                onSuccess()
            }, 1500)
        } catch (err: unknown) {
            setError(err instanceof Error ? err.message : 'Failed to save')
            setIsSubmitting(false)
        }
    }

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal" onClick={e => e.stopPropagation()}>

                <div className="modal-header">
                    <h2>Add Settlement</h2>
                    <button className="btn-close" onClick={onClose} disabled={isSubmitting}>✕</button>
                </div>

                {error && <div className="error-message" style={{ margin: '1rem', padding: '0.5rem', background: '#ffebee', color: '#cc0000', borderRadius: '4px' }}>{error}</div>}
                {successMessage && <div className="success-message" style={{ margin: '1rem', padding: '0.5rem', background: '#e8f5e9', color: '#2e7d32', borderRadius: '4px' }}>{successMessage}</div>}

                <form onSubmit={handleSubmit} className="modal-form">

                    <div className="form-group">
                        <label>Task *</label>
                        <select value={taskId} onChange={e => setTaskId(Number(e.target.value) || '')} required>
                            <option value="">-- Select Task --</option>
                            {filteredTasks.map(t => (
                                <option key={t.id} value={t.id}>{t.title}</option>
                            ))}
                        </select>
                    </div>

                    <div className="form-group">
                        <label>Worker *</label>
                        <select value={workerId} onChange={e => setWorkerId(Number(e.target.value) || '')} required>
                            <option value="">-- Select Worker --</option>
                            {filteredWorkers.map(w => (
                                <option key={w.id} value={w.id}>{w.firstName} {w.lastName}</option>
                            ))}
                        </select>
                    </div>

                    <div className="form-group">
                        <label>Scheduled Start Time *</label>
                        <input
                            type="datetime-local"
                            value={settlementDate}
                            onChange={e => setSettlementDate(e.target.value)}
                            required
                        />
                    </div>

                    <div className="modal-footer">
                        <button type="button" className="btn-cancel" onClick={onClose} disabled={isSubmitting}>Cancel</button>
                        <button type="submit" className="btn-save" disabled={isSubmitting}>
                            {isSubmitting ? 'Saving...' : 'Save'}
                        </button>
                    </div>

                </form>
            </div>
        </div>
    )
}

export default SettlementModal
