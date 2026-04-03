import { useState, useEffect } from 'react'
import type { FormEvent } from 'react'
import type { User, CreateUserRequest, UpdateUserRequest, Department, WorkerAvailability, Skill } from '../types'
import { departmentApi, skillApi } from '../api'

interface UserModalProps {
    user: User | null
    onSubmit: (data: CreateUserRequest | UpdateUserRequest) => Promise<any> | void
    onClose: () => void
}

const DAYS_OF_WEEK = [
    'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY',
] as const

type DayOfWeek = typeof DAYS_OF_WEEK[number]

interface AvailRow {
    id: number | null
    dayOfWeek: DayOfWeek
    startTime: string
    endTime: string
}

const UserModal = ({ user, onSubmit, onClose }: UserModalProps) => {
    // ── Form State ───────────────────────────────────────────────────────────
    const [nationalId, setNationalId] = useState('')
    const [password, setPassword] = useState('')
    const [firstName, setFirstName] = useState('')
    const [lastName, setLastName] = useState('')
    const [email, setEmail] = useState('')
    const [phoneNumber, setPhoneNumber] = useState('')
    const [role, setRole] = useState<'ADMIN' | 'MANAGER' | 'WORKER'>('WORKER')
    const [selectedDept, setSelectedDept] = useState('')
    const [selectedSkillIds, setSelectedSkillIds] = useState<number[]>([])
    const [availRows, setAvailRows] = useState<AvailRow[]>([])
    const [maxTasks, setMaxTasks] = useState<number | ''>(user?.maxTasks ?? 5)

    // ── Data Fetching State ────────────────────────────────────────────────────────────────────────────────────────────────────────
    const [departments, setDepartments] = useState<Department[]>([])
    const [skills, setSkills] = useState<Skill[]>([])
    const [isLoading, setIsLoading] = useState(true)

    // ── Helper to convert backend availability format to form row format ────
    const toRows = (avs: WorkerAvailability[]): AvailRow[] =>
        avs.map(a => ({
            id: a.id,
            dayOfWeek: a.dayOfWeek as DayOfWeek,
            startTime: a.startTime.slice(0, 5), // "HH:MM:SS" → "HH:MM"
            endTime:   a.endTime.slice(0, 5),
        }))

    // Effect for fetching static data like departments and jobs
    useEffect(() => {
        setIsLoading(true)
        Promise.all([
            departmentApi.getAll(),
            skillApi.getAll()
        ]).then(([deptRes, skillRes]) => {
            setDepartments(deptRes.data)
            setSkills(skillRes.data)
        }).catch(() => {
            // Handle errors if necessary, e.g., show a toast notification
        }).finally(() => {
            setIsLoading(false)
        })
    }, [])

    // Effect for resetting form state when the user prop changes
    useEffect(() => {
        if (user) {
            setNationalId(user.nationalId ?? '')
            setFirstName(user.firstName ?? '')
            setLastName(user.lastName ?? '')
            setEmail(user.email ?? '')
            setPhoneNumber(user.phoneNumber ?? '')
            setRole(user.role ?? 'WORKER')
            setSelectedDept(user.departmentName ?? '')
            setSelectedSkillIds(user.skills?.map(s => s.id) ?? [])
            setAvailRows(user.availabilities ? toRows(user.availabilities) : [])
            setMaxTasks(user.maxTasks ?? 5)
        } else {
            // Reset to default values for a new user
            setNationalId('')
            setFirstName('')
            setLastName('')
            setEmail('')
            setPhoneNumber('')
            setRole('WORKER')
            setSelectedDept('')
            setSelectedSkillIds([])
            setAvailRows([])
            setMaxTasks(5)
        }
        setPassword('') // Always clear password field
    }, [user])

    // ── UI Helpers for dynamic availability rows ─────────────────────────────
    function addRow() {
        setAvailRows(prev => [
            ...prev,
            { id: null, dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '17:00' },
        ])
    }

    function removeRow(idx: number) {
        setAvailRows(prev => prev.filter((_, i) => i !== idx))
    }

    function updateRow<K extends keyof AvailRow>(idx: number, key: K, val: AvailRow[K]) {
        setAvailRows(prev => prev.map((r, i) => i === idx ? { ...r, [key]: val } : r))
    }
    
    function handleSkillChange(skillId: number) {
        setSelectedSkillIds(prev =>
            prev.includes(skillId)
                ? prev.filter(id => id !== skillId)
                : [...prev, skillId]
        )
    }

    const [errorMsg, setErrorMsg] = useState<string | null>(null)
    const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

    // ── Form Submission ──────────────────────────────────────────────────────────────────────────────────────────────────────────
    async function handleSubmit(e: FormEvent) {
        e.preventDefault()
        setErrorMsg(null)
        setFieldErrors({})

        const availabilities: WorkerAvailability[] = availRows.map(r => ({
            id: r.id,
            dayOfWeek: r.dayOfWeek,
            startTime: r.startTime.length === 5 ? r.startTime + ':00' : r.startTime,
            endTime:   r.endTime.length === 5   ? r.endTime   + ':00' : r.endTime,
        }))

        try {
            if (user) {
                const data: UpdateUserRequest = {
                    firstName:      firstName  || undefined,
                    lastName:       lastName   || undefined,
                    email:          email      || undefined,
                    phoneNumber:    phoneNumber || undefined,
                    role,
                    departmentName: selectedDept || null,
                    availabilities,
                    skillIds: selectedSkillIds,
                    maxTasks: maxTasks !== '' ? maxTasks : undefined
                }
                const res = onSubmit(data)
                if (res instanceof Promise) await res
            } else {
                const data: CreateUserRequest = {
                    nationalId,
                    password,
                    firstName: firstName || undefined,
                    lastName: lastName || undefined,
                    email: email || undefined,
                    phoneNumber: phoneNumber || undefined,
                    role,
                    departmentName: selectedDept || undefined,
                    availabilities,
                    skillIds: selectedSkillIds,
                    maxTasks: maxTasks !== '' ? maxTasks : undefined
                }
                const res = onSubmit(data)
                if (res instanceof Promise) await res
            }
        } catch (err: any) {
            // Check if backend returned a 400 with a field error map
            if (err.response && err.response.status === 400 && err.response.data && typeof err.response.data === 'object') {
                const data = err.response.data
                const newFieldErrors: Record<string, string> = {}
                for (const [field, message] of Object.entries(data)) {
                    if (typeof message === 'string') {
                        newFieldErrors[field] = message
                    }
                }
                if (Object.keys(newFieldErrors).length > 0) {
                    setFieldErrors(newFieldErrors)
                    return
                } else if (data.message) {
                    setErrorMsg(data.message)
                    return
                }
            }
            // Fallback generic error
            setErrorMsg(err?.response?.data?.message || err.message || 'Request failed.')
        }
    }

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal modal-wide" onClick={e => e.stopPropagation()}>

                <div className="modal-header">
                    <h2>{user ? 'Edit User' : 'Add User'}</h2>
                    <button className="btn-close" onClick={onClose}>✕</button>
                </div>

                <form onSubmit={handleSubmit} className="modal-form">
                    {!user && (
                        <>
                            <div className="form-group">
                                <label>National ID</label>
                                <input value={nationalId} onChange={e => setNationalId(e.target.value)} required placeholder="e.g. 123456789" />
                                {fieldErrors.nationalId && <small style={{ color: 'red', marginTop: '0.25rem' }}>{fieldErrors.nationalId}</small>}
                            </div>
                            <div className="form-group">
                                <label>Password</label>
                                <input type="password" value={password} onChange={e => setPassword(e.target.value)} required minLength={6} />
                                {fieldErrors.password && <small style={{ color: 'red', marginTop: '0.25rem' }}>{fieldErrors.password}</small>}
                            </div>
                        </>
                    )}

                    <div className="form-row">
                        <div className="form-group">
                            <label>First Name</label>
                            <input value={firstName} onChange={e => setFirstName(e.target.value)} />
                            {fieldErrors.firstName && <small style={{ color: 'red', marginTop: '0.25rem' }}>{fieldErrors.firstName}</small>}
                        </div>
                        <div className="form-group">
                            <label>Last Name</label>
                            <input value={lastName} onChange={e => setLastName(e.target.value)} />
                            {fieldErrors.lastName && <small style={{ color: 'red', marginTop: '0.25rem' }}>{fieldErrors.lastName}</small>}
                        </div>
                    </div>

                    <div className="form-row">
                        <div className="form-group">
                            <label>Email</label>
                            <input type="email" value={email} onChange={e => setEmail(e.target.value)} />
                            {fieldErrors.email && <small style={{ color: 'red', marginTop: '0.25rem' }}>{fieldErrors.email}</small>}
                        </div>
                        <div className="form-group">
                            <label>Phone Number</label>
                            <input type="tel" value={phoneNumber} onChange={e => setPhoneNumber(e.target.value)} />
                            {fieldErrors.phoneNumber && <small style={{ color: 'red', marginTop: '0.25rem' }}>{fieldErrors.phoneNumber}</small>}
                        </div>
                    </div>

                    <div className="form-row">
                        <div className="form-group">
                            <label>Role</label>
                            <select value={role} onChange={e => setRole(e.target.value as 'ADMIN' | 'MANAGER' | 'WORKER')}>
                                <option value="WORKER">WORKER</option>
                                <option value="MANAGER">MANAGER</option>
                                <option value="ADMIN">ADMIN</option>
                            </select>
                            {fieldErrors.role && <small style={{ color: 'red', marginTop: '0.25rem' }}>{fieldErrors.role}</small>}
                        </div>
                        <div className="form-group">
                            <label>Department</label>
                            <select value={selectedDept} onChange={e => setSelectedDept(e.target.value)}>
                                <option value="">- None -</option>
                                {departments.map(d => (
                                    <option key={d.id} value={d.name}>{d.name}</option>
                                ))}
                            </select>
                            {fieldErrors.departmentName && <small style={{ color: 'red', marginTop: '0.25rem' }}>{fieldErrors.departmentName}</small>}
                        </div>
                    </div>

                    <div className="form-group">
                        <label>Max Tasks</label>
                        <input type="number" value={maxTasks} onChange={e => setMaxTasks(e.target.value === '' ? '' : Number(e.target.value))} required min={1} />
                        {fieldErrors.maxTasks && <small style={{ color: 'red', marginTop: '0.25rem' }}>{fieldErrors.maxTasks}</small>}
                    </div>

                    <div className="form-group">
                        <label>Skills</label>
                        {isLoading ? <p>Loading skills...</p> : (
                            <div className="skills-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '0.5rem', padding: '0.5rem', border: '1px solid #e2e8f0', borderRadius: '6px', background: '#f8fafc' }}>
                                {skills.map(skill => (
                                    <div key={skill.id} style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                        <input
                                            type="checkbox"
                                            id={`skill-${skill.id}`}
                                            checked={selectedSkillIds.includes(skill.id)}
                                            onChange={() => handleSkillChange(skill.id)}
                                            style={{ margin: 0, width: '16px', height: '16px', cursor: 'pointer' }}
                                        />
                                        <label htmlFor={`skill-${skill.id}`} style={{ fontSize: '0.85rem', color: '#4a5568', margin: 0, cursor: 'pointer' }}>{skill.name}</label>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    <div className="avail-section">
                        <div className="avail-section-header">
                            <span className="avail-section-title">📅 Weekly Shifts</span>
                            <button type="button" className="btn-add-row" onClick={addRow}>+ Add Shift</button>
                        </div>

                        {availRows.length === 0 ? (
                            <p className="avail-empty">No shifts defined.</p>
                        ) : (
                            <div className="avail-table-wrap">
                                <table className="avail-table">
                                    <thead>
                                        <tr>
                                            <th>Day</th>
                                            <th>Start</th>
                                            <th>End</th>
                                            <th></th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {availRows.map((row, idx) => (
                                            <tr key={idx}>
                                                <td>
                                                    <select
                                                        value={row.dayOfWeek}
                                                        onChange={e => updateRow(idx, 'dayOfWeek', e.target.value as DayOfWeek)}
                                                    >
                                                        {DAYS_OF_WEEK.map(d => <option key={d} value={d}>{d}</option>)}
                                                    </select>
                                                </td>
                                                <td><input type="time" value={row.startTime} onChange={e => updateRow(idx, 'startTime', e.target.value)} /></td>
                                                <td><input type="time" value={row.endTime} onChange={e => updateRow(idx, 'endTime', e.target.value)} /></td>
                                                <td><button type="button" className="btn-remove-row" onClick={() => removeRow(idx)} title="Remove shift">✕</button></td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
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

export default UserModal
