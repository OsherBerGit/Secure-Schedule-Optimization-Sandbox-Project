import { useState } from 'react'
import type { FormEvent } from 'react'
import type { Task, ConstraintType } from '../types'

interface TaskConstraintModalProps {
    tasks: Task[]
    constraintTypes: ConstraintType[]
    onSubmit: (data: { predecessorTaskId: number, successorTaskId: number, constraintTypeId: number, lagMinutes?: number }) => void
    onClose: () => void
}

const TaskConstraintModal = ({ tasks, constraintTypes, onSubmit, onClose }: TaskConstraintModalProps) => {
    const [predId, setPredId] = useState<number | ''>('')
    const [succId, setSuccId] = useState<number | ''>('')
    const [typeId, setTypeId] = useState<number | ''>('')
    const [lag, setLag] = useState<number | ''>('')
    const [error, setError] = useState<string | null>(null)

    function handleSubmit(e: FormEvent) {
        e.preventDefault()
        setError(null)
        if (predId === '' || succId === '' || typeId === '') {
            setError('Please fill all required fields')
            return
        }
        if (predId === succId) {
            setError('Predecessor and successor tasks cannot be the same')
            return
        }
        onSubmit({
            predecessorTaskId: Number(predId),
            successorTaskId: Number(succId),
            constraintTypeId: Number(typeId),
            lagMinutes: lag !== '' ? Number(lag) : undefined,
        })
    }

    return (
        <div className="modal-overlay" onClick={onClose} style={{ overflow: 'visible' }}>
            <div className="modal" onClick={e => e.stopPropagation()} style={{ overflow: 'visible' }}>
                <div className="modal-header">
                    <h2>Add Task Constraint</h2>
                    <button type="button" className="btn-close" onClick={onClose}>✕</button>
                </div>
                {error && <div className="error-message" style={{ margin: '1rem', marginBottom: 0 }}>{error}</div>}
                <form onSubmit={handleSubmit} className="modal-form">
                    <div className="form-group">
                        <label>Predecessor Task</label>
                        <select
                            value={predId}
                            onChange={e => setPredId(e.target.value === '' ? '' : Number(e.target.value))}
                            required
                        >
                            <option value="" disabled>Select predecessor...</option>
                            {tasks.map(t => <option key={t.id} value={t.id}>{t.title}</option>)}
                        </select>
                    </div>

                    <div className="form-group">
                        <label>Successor Task</label>
                        <select
                            value={succId}
                            onChange={e => setSuccId(e.target.value === '' ? '' : Number(e.target.value))}
                            required
                        >
                            <option value="" disabled>Select successor...</option>
                            {tasks.map(t => <option key={t.id} value={t.id}>{t.title}</option>)}
                        </select>
                    </div>

                    <div className="form-group">
                        <label>Constraint Type</label>
                        <select
                            value={typeId}
                            onChange={e => setTypeId(e.target.value === '' ? '' : Number(e.target.value))}
                            required
                        >
                            <option value="" disabled>Select constraint type...</option>
                            {constraintTypes.map(ct => <option key={ct.id} value={ct.id}>{ct.name}</option>)}
                        </select>
                    </div>

                    <div className="form-group">
                        <label>Lag (minutes, optional)</label>
                        <input
                            type="number"
                            min={0}
                            placeholder="e.g. 60"
                            value={lag}
                            onChange={e => setLag(e.target.value === '' ? '' : Number(e.target.value))}
                        />
                    </div>

                    <div className="modal-footer">
                        <button type="button" className="btn-cancel" onClick={onClose}>Cancel</button>
                        <button type="submit" className="btn-save">Create Constraint</button>
                    </div>
                </form>
            </div>
        </div>
    )
}

export default TaskConstraintModal

