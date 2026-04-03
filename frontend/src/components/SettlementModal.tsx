import { useState } from 'react'
import type { FormEvent } from 'react'
import type { CreateSettlementRequest, Task, User } from '../types'

interface SettlementModalProps {
    tasks: Task[]
    workers: User[]
    onSubmit: (data: CreateSettlementRequest) => void
    onClose: () => void
}

const SettlementModal = ({ tasks, workers, onSubmit, onClose }: SettlementModalProps) => {
    const [taskId, setTaskId] = useState<number>(tasks[0]?.id ?? 0)
    const [workerId, setWorkerId] = useState<number>(workers[0]?.id ?? 0)
    const [settlementDate, setSettlementDate] = useState('')
    const [completionDate, setCompletionDate] = useState('')

    function handleSubmit(e: FormEvent) {
        e.preventDefault()
        // datetime-local gives "YYYY-MM-DDTHH:mm" - Spring LocalDateTime needs seconds appended
        const toIso = (val: string) => val ? val + ':00' : val
        const data: CreateSettlementRequest = {
            taskId,
            workerId,
            settlementDate: toIso(settlementDate),
            completionDate: completionDate ? toIso(completionDate) : undefined,
        }
        onSubmit(data)
    }

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal" onClick={e => e.stopPropagation()}>

                <div className="modal-header">
                    <h2>Add Settlement</h2>
                    <button className="btn-close" onClick={onClose}>✕</button>
                </div>

                <form onSubmit={handleSubmit} className="modal-form">

                    <div className="form-group">
                        <label>Task *</label>
                        <select value={taskId} onChange={e => setTaskId(Number(e.target.value))} required>
                            {tasks.map(t => (
                                <option key={t.id} value={t.id}>{t.title}</option>
                            ))}
                        </select>
                    </div>

                    <div className="form-group">
                        <label>Worker *</label>
                        <select value={workerId} onChange={e => setWorkerId(Number(e.target.value))} required>
                            {workers.map(w => (
                                <option key={w.id} value={w.id}>{w.firstName} {w.lastName}</option>
                            ))}
                        </select>
                    </div>

                    <div className="form-group">
                        <label>Settlement Date *</label>
                        <input
                            type="datetime-local"
                            value={settlementDate}
                            onChange={e => setSettlementDate(e.target.value)}
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label>Completion Date</label>
                        <input
                            type="datetime-local"
                            value={completionDate}
                            onChange={e => setCompletionDate(e.target.value)}
                        />
                    </div>

                    <div className="modal-footer">
                        <button type="button" className="btn-cancel" onClick={onClose}>Cancel</button>
                        <button type="submit" className="btn-save">Save</button>
                    </div>

                </form>
            </div>
        </div>
    )
}

export default SettlementModal

