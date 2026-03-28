import { useState, useEffect } from 'react'
import type { FormEvent } from 'react'
import type { User, CreateUserRequest, UpdateUserRequest, Department, WorkerAvailability, Job } from '../types'
import { departmentApi, jobApi } from '../api'

interface UserModalProps {
    user: User | null
    onSubmit: (data: CreateUserRequest | UpdateUserRequest) => void
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
    const [selectedJobIds, setSelectedJobIds] = useState<number[]>([])
    const [availRows, setAvailRows] = useState<AvailRow[]>([])

    // ── Data Fetching State ──────────────────────────────────────────────────
    const [departments, setDepartments] = useState<Department[]>([])
    const [jobs, setJobs] = useState<Job[]>([])
    const [isLoading, setIsLoading] = useState(true)

    // ── Helper to convert backend availability format to form row format ─────
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
            jobApi.getAll()
        ]).then(([deptRes, jobRes]) => {
            setDepartments(deptRes.data)
            setJobs(jobRes.data)
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
            setSelectedJobIds(user.jobs?.map(j => j.id) ?? [])
            setAvailRows(user.availabilities ? toRows(user.availabilities) : [])
        } else {
            // Reset to default values for a new user
            setNationalId('')
            setFirstName('')
            setLastName('')
            setEmail('')
            setPhoneNumber('')
            setRole('WORKER')
            setSelectedDept('')
            setSelectedJobIds([])
            setAvailRows([])
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
    
    function handleJobChange(jobId: number) {
        setSelectedJobIds(prev =>
            prev.includes(jobId)
                ? prev.filter(id => id !== jobId)
                : [...prev, jobId]
        )
    }

    // ── Form Submission ──────────────────────────────────────────────────────
    function handleSubmit(e: FormEvent) {
        e.preventDefault()

        const availabilities: WorkerAvailability[] = availRows.map(r => ({
            id: r.id,
            dayOfWeek: r.dayOfWeek,
            startTime: r.startTime.length === 5 ? r.startTime + ':00' : r.startTime,
            endTime:   r.endTime.length === 5   ? r.endTime   + ':00' : r.endTime,
        }))

        if (user) {
            const data: UpdateUserRequest = {
                firstName:      firstName  || undefined,
                lastName:       lastName   || undefined,
                email:          email      || undefined,
                phoneNumber:    phoneNumber || undefined,
                role,
                departmentName: selectedDept || null,
                availabilities,
                jobIds: selectedJobIds
            }
            onSubmit(data)
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
                jobIds: selectedJobIds
            }
            onSubmit(data)
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
                            </div>
                            <div className="form-group">
                                <label>Password</label>
                                <input type="password" value={password} onChange={e => setPassword(e.target.value)} required />
                            </div>
                        </>
                    )}

                    <div className="form-row">
                        <div className="form-group">
                            <label>First Name</label>
                            <input value={firstName} onChange={e => setFirstName(e.target.value)} />
                        </div>
                        <div className="form-group">
                            <label>Last Name</label>
                            <input value={lastName} onChange={e => setLastName(e.target.value)} />
                        </div>
                    </div>

                    <div className="form-row">
                        <div className="form-group">
                            <label>Email</label>
                            <input type="email" value={email} onChange={e => setEmail(e.target.value)} />
                        </div>
                        <div className="form-group">
                            <label>Phone Number</label>
                            <input value={phoneNumber} onChange={e => setPhoneNumber(e.target.value)} />
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
                        </div>
                        <div className="form-group">
                            <label>Department</label>
                            <select value={selectedDept} onChange={e => setSelectedDept(e.target.value)}>
                                <option value="">— None —</option>
                                {departments.map(d => (
                                    <option key={d.id} value={d.name}>{d.name}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    <div className="form-group">
                        <label>Jobs</label>
                        {isLoading ? <p>Loading...</p> : (
                            <div className="grid grid-cols-3 gap-2 p-2 border rounded-md bg-gray-50">
                                {jobs.map(job => (
                                    <div key={job.id} className="flex items-center">
                                        <input
                                            type="checkbox"
                                            id={`job-${job.id}`}
                                            checked={selectedJobIds.includes(job.id)}
                                            onChange={() => handleJobChange(job.id)}
                                            className="mr-2 h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                                        />
                                        <label htmlFor={`job-${job.id}`} className="text-sm text-gray-700">{job.name}</label>
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
