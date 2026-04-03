import { useState, useEffect } from 'react'
import type { FormEvent } from 'react'
import type { Task, CreateTaskRequest, UpdateTaskRequest, Status, Priority, Department, Skill } from '../types'
import { departmentApi, skillApi } from '../api'
import { useAuth } from '../context/useAuth'

interface TaskModalProps {
    task: Task | null
    statuses: Status[]
    priorities: Priority[]
    onSubmit: (data: CreateTaskRequest | UpdateTaskRequest) => Promise<any> | void
    onClose: () => void
}

const TaskModal = ({ task, statuses, priorities, onSubmit, onClose }: TaskModalProps) => {
    const { user } = useAuth()
    const [title, setTitle] = useState(task?.title ?? '')
    const [description, setDescription] = useState(task?.description ?? '')
    const [deadline, setDeadline] = useState(task?.deadline ? task.deadline.substring(0, 16) : '')
    const [durationHours, setDurationHours] = useState<number | ''>(task?.durationHours ?? 1)
    const [priorityId, setPriorityId] = useState<number>(task?.priorityId ?? (priorities[0]?.id ?? 0))
    const [statusId, setStatusId] = useState<number>(task?.taskStatusId ?? (statuses[0]?.id ?? 0))
    const [departments, setDepartments] = useState<Department[]>([])
    const [skills, setSkills] = useState<Skill[]>([])
    const [departmentId, setDepartmentId] = useState<number | ''>('')
    const [requiredSkillId, setRequiredSkillId] = useState<number | ''>(task?.requiredSkill?.id ?? '')
    const [isLoading, setIsLoading] = useState(true)

    const isAdmin = user?.role === 'ADMIN'
    const isManager = user?.role === 'MANAGER'

    const [errorMsg, setErrorMsg] = useState<string | null>(null)
    const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

    useEffect(() => {
        Promise.all([
            skillApi.getAll(),
            (isAdmin || isManager) ? departmentApi.getAll() : Promise.resolve({ data: [] })
        ]).then(([skillRes, deptRes]) => {
            setSkills(skillRes.data)
            setDepartments(deptRes.data)

            if (task?.departmentName) {
                const match = deptRes.data.find(d => d.name === task.departmentName)
                if (match) setDepartmentId(match.id)
            } else if (isManager && user?.departmentName) {
                const match = deptRes.data.find(d => d.name === user.departmentName)
                if (match) setDepartmentId(match.id)
            }
        }).catch(console.error).finally(() => setIsLoading(false))
    }, [isAdmin, isManager, task, user])

    async function handleSubmit(e: FormEvent) {
        e.preventDefault()
        setErrorMsg(null)
        setFieldErrors({})

        // If creating, logic expects OPEN status which is ID 1 or find by name. But we just omit it if your API defaults to it, or we rely on the init state.
        const data: CreateTaskRequest | UpdateTaskRequest = {
            title,
            description: description || undefined,
            deadline: deadline || undefined,
            durationHours: durationHours !== '' ? durationHours : undefined,
            priorityId,
            departmentId: departmentId !== '' ? departmentId : undefined,
            requiredSkill: requiredSkillId !== '' ? Number(requiredSkillId) : null
        }

        if (task) {
            (data as UpdateTaskRequest).statusId = statusId;
        }

        try {
            const res = onSubmit(data)
            if (res instanceof Promise) await res
        } catch (err: any) {
            if (err.response && err.response.status === 400 && err.response.data && typeof err.response.data === 'object') {
                const mapData = err.response.data
                const newFieldErrors: Record<string, string> = {}
                for (const [field, message] of Object.entries(mapData)) {
                    if (typeof message === 'string') {
                        newFieldErrors[field] = message
                    }
                }
                if (Object.keys(newFieldErrors).length > 0) {
                    setFieldErrors(newFieldErrors)
                    return
                } else if (mapData.message) {
                    setErrorMsg(mapData.message)
                    return
                }
            }
            setErrorMsg(err?.response?.data?.message || err.message || 'Request failed.')
        }
    }

    const isClosed = task?.taskStatusName === 'CLOSED'

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal modal-wide" onClick={e => e.stopPropagation()}>

                <div className="modal-header">
                    <h2>{task ? 'Edit Task' : 'Add Task'}</h2>
                    <button className="btn-close" onClick={onClose}>✕</button>
                </div>

                <form onSubmit={handleSubmit} className="modal-form">
                    <fieldset disabled={isClosed} style={{ border: 'none', padding: 0, margin: 0 }}>
                        <div className="form-group">
                            <label>Title *</label>
                            <input value={title} onChange={e => setTitle(e.target.value)} required />
                            {fieldErrors.title && <small style={{ color: 'red', marginTop: '0.25rem' }}>{fieldErrors.title}</small>}
                        </div>

                        <div className="form-group">
                            <label>Description</label>
                            <textarea value={description ?? ''} onChange={e => setDescription(e.target.value)} rows={3} />
                            {fieldErrors.description && <small style={{ color: 'red', marginTop: '0.25rem' }}>{fieldErrors.description}</small>}
                        </div>

                        <div className="form-row">
                            <div className="form-group">
                                <label>Priority *</label>
                                <select value={priorityId} onChange={e => setPriorityId(Number(e.target.value))} required>
                                    {priorities.map(p => (
                                        <option key={p.id} value={p.id}>{p.name}</option>
                                    ))}
                                </select>
                                {fieldErrors.priorityId && <small style={{ color: 'red', marginTop: '0.25rem' }}>{fieldErrors.priorityId}</small>}
                            </div>

                            {isAdmin && (
                                <div className="form-group">
                                    <label>Department</label>
                                    <select
                                        value={departmentId}
                                        onChange={e => setDepartmentId(e.target.value === '' ? '' : Number(e.target.value))}
                                    >
                                        <option value="">- Unassigned -</option>
                                        {departments.map(d => (
                                            <option key={d.id} value={d.id}>{d.name}</option>
                                        ))}
                                    </select>
                                    {fieldErrors.departmentId && <small style={{ color: 'red', marginTop: '0.25rem' }}>{fieldErrors.departmentId}</small>}
                                </div>
                            )}

                            {task && (
                                <div className="form-group">
                                    <label>Status *</label>
                                    <select
                                      value={statusId}
                                      onChange={(e) => setStatusId(Number(e.target.value))}
                                      required
                                    >
                                      {task.taskStatusName === 'OPEN' || task.taskStatusName === 'LOCKED' ? (
                                        <>
                                          <option value={statuses.find(s => s.name === 'OPEN')?.id}>OPEN</option>
                                          <option value={statuses.find(s => s.name === 'LOCKED')?.id}>LOCKED</option>
                                        </>
                                      ) : (
                                        <option value={task.taskStatusId ?? ''}>{task.taskStatusName}</option>
                                      )}
                                    </select>
                                    {fieldErrors.statusId && <small style={{ color: 'red', marginTop: '0.25rem' }}>{fieldErrors.statusId}</small>}
                                </div>
                            )}
                        </div>

                        <div className="form-group">
                            <label>Required Skill</label>
                            {isLoading ? <p>Loading skills...</p> : (
                                <select value={requiredSkillId} onChange={e => setRequiredSkillId(e.target.value === '' ? '' : Number(e.target.value))}>
                                    <option value="">-- None --</option>
                                    {skills.map(skill => (
                                        <option key={skill.id} value={skill.id}>{skill.name}</option>
                                    ))}
                                </select>
                            )}
                            {fieldErrors.requiredSkill && <small style={{ color: 'red', marginTop: '0.25rem' }}>{fieldErrors.requiredSkill}</small>}
                        </div>

                        <div className="form-row">
                            <div className="form-group">
                                <label>Deadline</label>
                                <input type="datetime-local" value={deadline} onChange={e => setDeadline(e.target.value)} />
                                {fieldErrors.deadline && <small style={{ color: 'red', marginTop: '0.25rem' }}>{fieldErrors.deadline}</small>}
                            </div>

                            <div className="form-group">
                                <label>Duration (hours)</label>
                                <input
                                    type="number"
                                    value={durationHours}
                                    onChange={e => setDurationHours(e.target.value === '' ? '' : Number(e.target.value))}
                                    min={1}
                                    required
                                />
                                {fieldErrors.durationHours && <small style={{ color: 'red', marginTop: '0.25rem' }}>{fieldErrors.durationHours}</small>}
                            </div>
                        </div>

                        <div className="modal-footer">
                            <button type="button" className="btn-cancel" onClick={onClose}>Cancel</button>
                            {!isClosed && <button type="submit" className="btn-save">Save</button>}
                        </div>
                    </fieldset>
                </form>
            </div>
        </div>
    )
}

export default TaskModal
