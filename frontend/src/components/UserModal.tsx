import { useState, useEffect } from 'react'
import type { FormEvent } from 'react'
import type { User, CreateUserRequest, UpdateUserRequest, Department, WorkerAvailability } from '../types'
import { departmentApi } from '../api'

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
    // ── Basic fields ─────────────────────────────────────────────────────────
    const [nationalId, setNationalId] = useState(user?.nationalId ?? '')
    const [password, setPassword] = useState('')
    const [firstName, setFirstName] = useState(user?.firstName ?? '')
    const [lastName, setLastName] = useState(user?.lastName ?? '')
    const [email, setEmail] = useState(user?.email ?? '')
    const [phoneNumber, setPhoneNumber] = useState(user?.phoneNumber ?? '')
    const derivedRole: 'ADMIN' | 'WORKER' =
        user?.role ?? (user?.roles?.includes('ADMIN') ? 'ADMIN' : 'WORKER')
    const [role, setRole] = useState<'ADMIN' | 'WORKER'>(derivedRole)

    // ── Department ───────────────────────────────────────────────────────────
    const [departments, setDepartments] = useState<Department[]>([])
    const [selectedDept, setSelectedDept] = useState<string>(user?.departmentName ?? '')

    // ── Availabilities ───────────────────────────────────────────────────────
    const toRows = (avs: WorkerAvailability[]): AvailRow[] =>
        avs.map(a => ({
            id: a.id,
            dayOfWeek: a.dayOfWeek as DayOfWeek,
            startTime: a.startTime.slice(0, 5),   // "HH:MM:SS" → "HH:MM"
            endTime:   a.endTime.slice(0, 5),
        }))

    const [availRows, setAvailRows] = useState<AvailRow[]>(
        user?.availabilities ? toRows(user.availabilities) : []
    )

    useEffect(() => {
        departmentApi.getAll()
            .then(res => setDepartments(res.data))
            .catch(() => { /* non-fatal */ })
    }, [])

    // ── Availability row helpers ─────────────────────────────────────────────
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

    // ── Submit ───────────────────────────────────────────────────────────────
    function handleSubmit(e: FormEvent) {
        e.preventDefault()

        // Map rows → WorkerAvailability (backend format: "HH:MM:SS")
        const availabilities: WorkerAvailability[] = availRows.map(r => ({
            id: r.id,
            dayOfWeek: r.dayOfWeek,
            startTime: r.startTime.length === 5 ? r.startTime + ':00' : r.startTime,
            endTime:   r.endTime.length === 5   ? r.endTime   + ':00' : r.endTime,
        }))

        if (user) {
            // PUT /api/users/{id} accepts UserDto — send all editable fields
            const data: UpdateUserRequest = {
                firstName:      firstName  || undefined,
                lastName:       lastName   || undefined,
                email:          email      || undefined,
                phoneNumber:    phoneNumber || undefined,
                roles:          [role],
                departmentName: selectedDept || null,
                availabilities,
            }
            onSubmit(data)
        } else {
            const data: CreateUserRequest = {
                nationalId,
                password,
                firstName,
                lastName,
                email,
                phoneNumber,
                role,
                departmentName: selectedDept || undefined,
                availabilities,
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

                    {/* ── Create-only fields ──────────────────────────────── */}
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

                    {/* ── Two-column grid for basic info ──────────────────── */}
                    <div className="form-row">
                        <div className="form-group">
                            <label>First Name</label>
                            <input value={firstName ?? ''} onChange={e => setFirstName(e.target.value)} />
                        </div>
                        <div className="form-group">
                            <label>Last Name</label>
                            <input value={lastName ?? ''} onChange={e => setLastName(e.target.value)} />
                        </div>
                    </div>

                    <div className="form-row">
                        <div className="form-group">
                            <label>Email</label>
                            <input type="email" value={email ?? ''} onChange={e => setEmail(e.target.value)} />
                        </div>
                        <div className="form-group">
                            <label>Phone Number</label>
                            <input value={phoneNumber ?? ''} onChange={e => setPhoneNumber(e.target.value)} />
                        </div>
                    </div>

                    <div className="form-row">
                        <div className="form-group">
                            <label>Role</label>
                            <select value={role} onChange={e => setRole(e.target.value as 'ADMIN' | 'WORKER')}>
                                <option value="WORKER">WORKER</option>
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

                    {/* ── Weekly Shifts / Availabilities ──────────────────── */}
                    <div className="avail-section">
                        <div className="avail-section-header">
                            <span className="avail-section-title">📅 Weekly Shifts</span>
                            <button type="button" className="btn-add-row" onClick={addRow}>+ Add Shift</button>
                        </div>

                        {availRows.length === 0 ? (
                            <p className="avail-empty">No shifts defined. Click "+ Add Shift" to add one.</p>
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
                                                        {DAYS_OF_WEEK.map(d => (
                                                            <option key={d} value={d}>{d}</option>
                                                        ))}
                                                    </select>
                                                </td>
                                                <td>
                                                    <input
                                                        type="time"
                                                        value={row.startTime}
                                                        onChange={e => updateRow(idx, 'startTime', e.target.value)}
                                                    />
                                                </td>
                                                <td>
                                                    <input
                                                        type="time"
                                                        value={row.endTime}
                                                        onChange={e => updateRow(idx, 'endTime', e.target.value)}
                                                    />
                                                </td>
                                                <td>
                                                    <button
                                                        type="button"
                                                        className="btn-remove-row"
                                                        onClick={() => removeRow(idx)}
                                                        title="Remove shift"
                                                    >✕</button>
                                                </td>
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
