import { useState, useEffect } from 'react'
import type { FormEvent } from 'react'
import type { Task, CreateTaskRequest, UpdateTaskRequest, Status, Priority, Department, Role } from '../types'
import { departmentApi, roleApi } from '../api'
import { useAuth } from '../context/useAuth'

interface TaskModalProps {
    task: Task | null
    statuses: Status[]
    priorities: Priority[]
    onSubmit: (data: CreateTaskRequest | UpdateTaskRequest) => void
    onClose: () => void
}

const TaskModal = ({ task, statuses, priorities, onSubmit, onClose }: TaskModalProps) => {
    const { user } = useAuth()
    const [title, setTitle] = useState(task?.title ?? '')
    const [description, setDescription] = useState(task?.description ?? '')
    const [deadline, setDeadline] = useState(task?.deadline ? task.deadline.substring(0, 16) : '')
    const [durationHours, setDurationHours] = useState<number | ''>(task?.durationHours ?? '')
    const [priorityId, setPriorityId] = useState<number>(task?.priorityId ?? (priorities[0]?.id ?? 0))
    const [statusId, setStatusId] = useState<number>(task?.taskStatusId ?? (statuses[0]?.id ?? 0))
    const [departments, setDepartments] = useState<Department[]>([])
    const [roles, setRoles] = useState<Role[]>([])
    const [departmentId, setDepartmentId] = useState<number | ''>('')
    const [requiredRoleIds, setRequiredRoleIds] = useState<number[]>(task?.requiredRoleIds ?? [])

    const isAdmin = user?.roles?.includes('ADMIN') || user?.role === 'ADMIN'
    const isManager = user?.roles?.includes('MANAGER')

    useEffect(() => {
        // Fetch available roles
        roleApi.getAll()
            .then(res => setRoles(res.data))
            .catch(console.error)

        // Load departments if Admin or Manager (to resolve their own department ID)
        if (isAdmin || isManager) {
            departmentApi.getAll().then(res => {
                const depts = res.data
                setDepartments(depts)

                // If editing an existing task, try to match its department name to an ID
                if (task?.departmentName) {
                    const match = depts.find(d => d.name === task.departmentName)
                    if (match) setDepartmentId(match.id)
                } 
                // If creating new task and is Manager, auto-select their department
                else if (isManager && user?.departmentName) {
                    const match = depts.find(d => d.name === user.departmentName)
                    if (match) setDepartmentId(match.id)
                }
            }).catch(console.error)
        }
    }, [isAdmin, isManager, task, user])

    function handleRoleToggle(roleId: number) {
        setRequiredRoleIds(prev =>
            prev.includes(roleId) ? prev.filter(r => r !== roleId) : [...prev, roleId]
        )
    }

    function handleSubmit(e: FormEvent) {
        e.preventDefault()
        const data: CreateTaskRequest = {
            title,
            description: description || undefined,
            deadline: deadline || undefined,
            durationHours: durationHours !== '' ? durationHours : undefined,
            priorityId,
            departmentId: departmentId !== '' ? departmentId : undefined,
            requiredRoleIds: requiredRoleIds.length > 0 ? requiredRoleIds : undefined
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

                        {isAdmin && (
                            <div className="form-group">
                                <label>Department</label>
                                <select 
                                    value={departmentId} 
                                    onChange={e => setDepartmentId(e.target.value === '' ? '' : Number(e.target.value))}
                                >
                                    <option value="">— Unassigned —</option>
                                    {departments.map(d => (
                                        <option key={d.id} value={d.id}>{d.name}</option>
                                    ))}
                                </select>
                            </div>
                        )}
                        {/* Status is not editable here as it's settlement-driven, but we kept it in prev code? 
                            The previous code had status select. I'll keep it but it might not be used by backend now.
                            Actually, the user requirements don't say to remove it.
                         */}
                        <div className="form-group">
                            <label>Status *</label>
                            <select value={statusId} onChange={e => setStatusId(Number(e.target.value))} required>
                                {statuses.map(s => (
                                    <option key={s.id} value={s.id}>{s.name}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    {/* Required Roles Section */}
                    <div className="form-group">
                        <label>Required Roles</label>
                        <div className="roles-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))', gap: '8px', marginTop: '4px', border: '1px solid #e2e8f0', padding: '10px', borderRadius: '6px' }}>
                            {roles.length === 0 && <span style={{ color: '#888', fontStyle: 'italic', fontSize: '0.9rem' }}>No roles available.</span>}
                            {roles.map(role => (
                                <label key={role.id} style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.9rem', cursor: 'pointer', userSelect: 'none' }}>
                                    <input
                                        type="checkbox"
                                        checked={requiredRoleIds.includes(role.id)}
                                        onChange={() => handleRoleToggle(role.id)}
                                        style={{ accentColor: '#4f46e5', width: '16px', height: '16px' }}
                                    />
                                    {role.roleName}
                                </label>
                            ))}
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
