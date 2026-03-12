import { useState } from 'react'
import type { FormEvent } from 'react'
import type { Task, CreateTaskRequest, UpdateTaskRequest, Status, Priority } from '../types'

interface TaskModalProps {
    task: Task | null
    statuses: Status[]
    priorities: Priority[]
    onSubmit: (data: CreateTaskRequest | UpdateTaskRequest) => void
    onClose: () => void
}

const TaskModal = ({ task, statuses, priorities, onSubmit, onClose }: TaskModalProps) => {
    const [title, setTitle] = useState(task?.title ?? '')
    const [description, setDescription] = useState(task?.description ?? '')
    const [deadline, setDeadline] = useState(task?.deadline ? task.deadline.substring(0, 16) : '')
    const [durationHours, setDurationHours] = useState<number | ''>(task?.durationHours ?? '')
    const [priorityId, setPriorityId] = useState<number>(task?.priorityId ?? (priorities[0]?.id ?? 0))
    const [statusId, setStatusId] = useState<number>(task?.taskStatusId ?? (statuses[0]?.id ?? 0))

    function handleSubmit(e: FormEvent) {
        e.preventDefault()
        const data: CreateTaskRequest = {
            title,
            description: description || undefined,
            deadline: deadline || undefined,
            durationHours: durationHours !== '' ? durationHours : undefined,
            priorityId,
        }
        onSubmit(data)
    }

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal" onClick={e => e.stopPropagation()}>

                <div className="modal-header">
                    <h2>{task ? 'Edit Task' : 'Add Task'}</h2>
                    <button className="btn-close" onClick={onClose}>✕</button>
                </div>

                <form onSubmit={handleSubmit} className="modal-form">

                    <div className="form-group">
                        <label>Title *</label>
                        <input value={title} onChange={e => setTitle(e.target.value)} required />
                    </div>

                    <div className="form-group">
                        <label>Description</label>
                        <textarea value={description} onChange={e => setDescription(e.target.value)} rows={3} />
                    </div>

                    <div className="form-row">
                        <div className="form-group">
                            <label>Priority *</label>
                            <select value={priorityId} onChange={e => setPriorityId(Number(e.target.value))} required>
                                {priorities.map(p => (
                                    <option key={p.id} value={p.id}>{p.name}</option>
                                ))}
                            </select>
                        </div>

                        <div className="form-group">
                            <label>Status *</label>
                            <select value={statusId} onChange={e => setStatusId(Number(e.target.value))} required>
                                {statuses.map(s => (
                                    <option key={s.id} value={s.id}>{s.name}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    <div className="form-row">
                        <div className="form-group">
                            <label>Deadline</label>
                            <input type="datetime-local" value={deadline} onChange={e => setDeadline(e.target.value)} />
                        </div>

                        <div className="form-group">
                            <label>Duration (hours)</label>
                            <input
                                type="number"
                                value={durationHours}
                                onChange={e => setDurationHours(e.target.value === '' ? '' : Number(e.target.value))}
                                min={1}
                            />
                        </div>
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

export default TaskModal

