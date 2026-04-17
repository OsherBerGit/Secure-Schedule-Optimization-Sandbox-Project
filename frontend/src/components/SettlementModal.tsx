import { useState, useEffect } from 'react'
import type { FormEvent } from 'react'
import { X, CalendarDays, CheckCircle2 } from 'lucide-react'
import DatePicker from 'react-datepicker'
import 'react-datepicker/dist/react-datepicker.css'
import { format } from 'date-fns'
import type { CreateSettlementRequest, Task, User, Settlement } from '../types'
import { settlementApi } from '../api'
import './SettlementModal.css'

interface SettlementModalProps {
    tasks: Task[]
    workers: User[]
    settlements: Settlement[]
    onSuccess: () => void
    onClose: () => void
}

const SettlementModal = ({ tasks: initialTasks, workers: initialWorkers, settlements, onSuccess, onClose }: SettlementModalProps) => {
    const [taskId, setTaskId] = useState<number | ''>('')
    const [workerId, setWorkerId] = useState<number | ''>('')
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [successMessage, setSuccessMessage] = useState<string | null>(null)

    const [selectedDate, setSelectedDate] = useState<Date | null>(null)

    const filteredWorkers = initialWorkers.filter(w => {
        const activeAssignmentsCount = settlements.filter(s => s.workerId === w.id && s.statusName !== 'COMPLETED').length;
        if (w.maxTasks !== null && w.maxTasks !== undefined && activeAssignmentsCount >= w.maxTasks) return false;

        if (!taskId) return true
        const selectedTask = initialTasks.find(t => t.id === taskId)
        if (!selectedTask || !selectedTask.requiredSkills) return true
        const workerSkillIds = w.skills?.map(s => s.id) || []
        return selectedTask.requiredSkills.every(skill => workerSkillIds.includes(skill.id))
    })

    const filteredTasks = initialTasks.filter(t => {
        const hasActiveSettlement = settlements.some(s => s.taskId === t.id && s.statusName !== 'COMPLETED');
        if (hasActiveSettlement || (t.taskStatusName && t.taskStatusName !== 'OPEN')) return false;

        if (!workerId) return true
        const selectedWorker = initialWorkers.find(w => w.id === workerId)
        if (!selectedWorker || !selectedWorker.skills) return true
        if (!t.requiredSkills) return true
        const workerSkillIds = selectedWorker.skills.map(s => s.id) || []
        return t.requiredSkills.every(skill => workerSkillIds.includes(skill.id))
    })

    // Auto-clear invalid selections
    if (taskId && !filteredTasks.some(t => t.id === taskId))
        setTaskId('')

    if (workerId && !filteredWorkers.some(w => w.id === workerId))
        setWorkerId('')

    async function handleSubmit(e: FormEvent) {
        e.preventDefault()
        if (!taskId || !workerId || !selectedDate) return

        setIsSubmitting(true)
        setError(null)
        setSuccessMessage(null)

        try {
            const combinedIso = format(selectedDate, "yyyy-MM-dd'T'HH:mm:00")

            const data: CreateSettlementRequest = {
                taskId: Number(taskId),
                workerId: Number(workerId),
                settlementDate: combinedIso,
                completionDate: undefined,
            }
            await settlementApi.create(data)

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
        <div className="modal-overlay">
            <div className="modern-modal-card" style={{ maxWidth: '500px', width: '90%' }}>
                <div className="modal-header">
                    <h2><CalendarDays size={22} className="text-primary" /> Add Settlement</h2>
                    <button type="button" className="modern-close-btn" onClick={onClose} disabled={isSubmitting}>
                        <X size={24} />
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="modern-modal-form">
                    <div className="modal-body" style={{ padding: '2rem' }}>

                        {error && (
                            <div className="error-message" style={{ marginBottom: '1.5rem' }}>
                                {error}
                            </div>
                        )}

                        {successMessage && (
                            <div style={{ marginBottom: '1.5rem', padding: '1rem', background: '#f0fdf4', color: '#15803d', borderRadius: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem', border: '1px solid #bbf7d0', fontSize: '0.95rem', fontWeight: '500' }}>
                                <CheckCircle2 size={20} />
                                {successMessage}
                            </div>
                        )}

                        <div className="form-grid" style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '1.5rem' }}>

                            <div className="modern-form-group">
                                <label>Task *</label>
                                <select
                                    className="modern-input"
                                    value={taskId}
                                    onChange={e => setTaskId(Number(e.target.value) || '')}
                                    required
                                    disabled={isSubmitting || !!successMessage}
                                >
                                    <option value="">-- Select Task --</option>
                                    {filteredTasks.map(t => (
                                        <option key={t.id} value={t.id}>{t.title}</option>
                                    ))}
                                </select>
                            </div>

                            <div className="modern-form-group">
                                <label>Worker *</label>
                                <select
                                    className="modern-input"
                                    value={workerId}
                                    onChange={e => setWorkerId(Number(e.target.value) || '')}
                                    required
                                    disabled={isSubmitting || !!successMessage}
                                >
                                    <option value="">-- Select Worker --</option>
                                    {filteredWorkers.map(w => (
                                        <option key={w.id} value={w.id}>{w.firstName} {w.lastName}</option>
                                    ))}
                                </select>
                            </div>

                            <div className="modern-form-group">
                                <label>Scheduled Date & Time *</label>
                                <DatePicker
                                    className="modern-input"
                                    selected={selectedDate}
                                    onChange={(date: Date | null) => setSelectedDate(date)}
                                    showTimeSelect
                                    timeIntervals={15}
                                    dateFormat="Pp"
                                    portalId="root-portal"
                                    showIcon
                                    placeholderText="Select date and time"
                                    disabled={isSubmitting || !!successMessage}
                                    required
                                />
                            </div>

                        </div>
                    </div>

                    <div className="modal-actions" style={{ padding: '1.5rem 2rem', background: '#f8fafc', borderTop: '1px solid #e2e8f0' }}>
                        <button type="button" className="btn-cancel" onClick={onClose} disabled={isSubmitting}>
                            Cancel
                        </button>
                        <button type="submit" className="btn-submit" disabled={isSubmitting || !!successMessage || !selectedDate}>
                            {isSubmitting ? 'Saving...' : 'Save Settlement'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    )
}

export default SettlementModal