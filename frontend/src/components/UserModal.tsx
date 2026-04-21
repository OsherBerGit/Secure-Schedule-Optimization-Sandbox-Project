import { useState, useEffect, type FormEvent } from 'react';
import { departmentApi, skillApi } from '../api';
import type { User, Department, Skill, UserAvailability, CreateUserRequest, UpdateUserRequest } from '../types';
import { X, User as UserIcon, Trash2 } from 'lucide-react';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import './UserModal.css';

const DAYS_OF_WEEK = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

interface UserModalProps {
    user: User | null
    departments: Department[]
    skills: Skill[]
    onSubmit: (data: CreateUserRequest | UpdateUserRequest) => Promise<void> | void
    onClose: () => void
}

type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

interface AvailRow {
    id: number | null
    dayOfWeek: DayOfWeek
    startTime: string
    endTime: string
}

const UserModal = ({ user, onSubmit, onClose }: UserModalProps) => {
    const [nationalId, setNationalId] = useState(user?.nationalId ?? '')
    const [password, setPassword] = useState('')
    const [firstName, setFirstName] = useState(user?.firstName ?? '')
    const [lastName, setLastName] = useState(user?.lastName ?? '')
    const [email, setEmail] = useState(user?.email ?? '')
    const [phoneNumber, setPhoneNumber] = useState(user?.phoneNumber ?? '')
    const [role, setRole] = useState<'ADMIN' | 'MANAGER' | 'WORKER'>(user?.role ?? 'WORKER')
    const [selectedDept, setSelectedDept] = useState(user?.departmentName ?? '')
    const [selectedSkillIds, setSelectedSkillIds] = useState<number[]>(user?.skills?.map(s => s.id) ?? [])
    const [maxTasks, setMaxTasks] = useState<number | ''>(user?.maxTasks ?? 5)

    const toRows = (avs: UserAvailability[]): AvailRow[] =>
        avs.map(a => ({
            id: a.id,
            dayOfWeek: a.dayOfWeek as DayOfWeek,
            startTime: a.startTime.slice(0, 5),
            endTime:   a.endTime.slice(0, 5),
        }))

    const [availRows, setAvailRows] = useState<AvailRow[]>(user?.availabilities ? toRows(user.availabilities) : [])
    const [departments, setDepartments] = useState<Department[]>([])
    const [skills, setSkills] = useState<Skill[]>([])
    const [isLoading, setIsLoading] = useState(false)

    useEffect(() => {
        let isMounted = true;
        setIsLoading(true)
        Promise.all([
            departmentApi.getAll(),
            skillApi.getAll()
        ]).then(([deptRes, skillRes]) => {
            if (isMounted) {
                setDepartments(deptRes.data)
                setSkills(skillRes.data)
            }
        }).catch(() => {
        }).finally(() => {
            if (isMounted) setIsLoading(false)
        })
        return () => { isMounted = false; }
    }, [])

    function handleSkillChange(skillId: number) {
        setSelectedSkillIds(prev =>
            prev.includes(skillId)
                ? prev.filter(id => id !== skillId)
                : [...prev, skillId]
        )
    }

    const [errorMsg, setErrorMsg] = useState<string | null>(null)
    const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
    const [isSubmitting, setIsSubmitting] = useState(false)

    function handleAddAvail() {
        setAvailRows(prev => [
            ...prev,
            { id: null, dayOfWeek: 'SUNDAY', startTime: '08:00', endTime: '17:00' }
        ])
    }

    function handleRemoveAvail(index: number) {
        setAvailRows(prev => prev.filter((_, i) => i !== index))
    }

    function handleChangeAvail(index: number, field: keyof AvailRow, value: string) {
        setAvailRows(prev => {
            const next = [...prev]
            next[index] = { ...next[index], [field]: value }
            return next
        })
    }

    function handleTimeChange(index: number, field: 'startTime' | 'endTime', date: Date | null) {
        if (!date) return;
        const hours = date.getHours().toString().padStart(2, '0');
        const minutes = date.getMinutes().toString().padStart(2, '0');
        handleChangeAvail(index, field, `${hours}:${minutes}`);
    }

    function timeStringToDate(timeStr: string) {
        if (!timeStr) return null;
        const [hours, minutes] = timeStr.split(':').map(Number);
        const date = new Date();
        date.setHours(hours, minutes, 0, 0);
        return date;
    }

    async function handleSubmit(e: FormEvent) {
        e.preventDefault()
        setErrorMsg(null)
        setFieldErrors({})
        setIsSubmitting(true)

        for (let i = 0; i < availRows.length; i++) {
            for (let j = i + 1; j < availRows.length; j++) {
                const r1 = availRows[i]
                const r2 = availRows[j]
                if (r1.dayOfWeek === r2.dayOfWeek) {
                    if (r1.startTime < r2.endTime && r2.startTime < r1.endTime) {
                        setErrorMsg(`Overlapping shifts detected on ${r1.dayOfWeek}`)
                        setIsSubmitting(false)
                        return
                    }
                }
            }
        }

        const availabilities: UserAvailability[] = availRows.map(r => ({
            id: r.id,
            dayOfWeek: r.dayOfWeek,
            startTime: r.startTime.length === 5 ? r.startTime + ':00' : r.startTime,
            endTime:   r.endTime.length === 5   ? r.endTime   + ':00' : r.endTime,
        }))

        try {
            if (user) {
                const data: UpdateUserRequest = {
                    firstName: firstName || undefined,
                    lastName: lastName || undefined,
                    email: email || undefined,
                    phoneNumber: phoneNumber || undefined,
                    role,
                    departmentName: selectedDept || null,
                    maxTasks: maxTasks || 0,
                    availabilities,
                    skillIds: selectedSkillIds
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
                    maxTasks: maxTasks || 0,
                    availabilities,
                    skillIds: selectedSkillIds
                }
                const res = onSubmit(data)
                if (res instanceof Promise) await res
            }
        } catch (err: unknown) {
            const error = err as { response?: { status?: number, data?: any }, message?: string };
            if (error.response && error.response.status === 400 && error.response.data && typeof error.response.data === 'object') {
                const data = error.response.data
                const newFieldErrors: Record<string, string> = {}
                for (const [field, message] of Object.entries(data)) {
                    if (typeof message === 'string') {
                        newFieldErrors[field] = message
                    }
                }
                if (Object.keys(newFieldErrors).length > 0) {
                    setFieldErrors(newFieldErrors)
                    setIsSubmitting(false)
                    return
                } else if (data.message) {
                    setErrorMsg(data.message)
                    setIsSubmitting(false)
                    return
                }
            }
            setErrorMsg(error?.response?.data?.message || error.message || 'Request failed.')
        } finally {
            setIsSubmitting(false)
        }
    }

    return (
        <div className="modal-overlay">
            <div className="user-modal-card">
                <div className="modal-header">
                    <h2><UserIcon size={22} className="text-primary" /> {user ? 'Edit User' : 'Add New User'}</h2>
                    <button type="button" className="modern-close-btn" onClick={onClose}>
                        <X size={24} />
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="user-modal-form">
                    <div className="user-modal-body">
                        {errorMsg && <div className="error-message" style={{ marginBottom: '1.5rem' }}>{errorMsg}</div>}

                        <div className="form-grid">
                            <div className="modern-form-group">
                                <label style={{ fontSize: '1rem', fontWeight: '600' }}>National ID</label>
                                <input className="modern-input" style={{ fontSize: '1rem', height: '48px' }} value={nationalId} onChange={e => setNationalId(e.target.value)} required placeholder="e.g. 123456789" />
                                {fieldErrors.nationalId && <small style={{ color: 'red', marginTop: '0.25rem' }}>{fieldErrors.nationalId}</small>}
                            </div>
                            <div className="modern-form-group">
                                <label style={{ fontSize: '1rem', fontWeight: '600' }}>Password</label>
                                <input className="modern-input" style={{ fontSize: '1rem', height: '48px' }} type="password" value={password} onChange={e => setPassword(e.target.value)} required={!user} minLength={6} placeholder="Min 6 characters" />
                                {fieldErrors.password && <small style={{ color: 'red', marginTop: '0.25rem' }}>{fieldErrors.password}</small>}
                            </div>

                            <div className="modern-form-group">
                                <label style={{ fontSize: '1rem', fontWeight: '600' }}>First Name</label>
                                <input
                                    type="text"
                                    className="modern-input"
                                    style={{ fontSize: '1rem', height: '48px' }}
                                    placeholder="Enter first name"
                                    value={firstName || ''}
                                    onChange={e => setFirstName(e.target.value)}
                                    required
                                />
                            </div>

                            <div className="modern-form-group">
                                <label style={{ fontSize: '1rem', fontWeight: '600' }}>Last Name</label>
                                <input
                                    type="text"
                                    className="modern-input"
                                    style={{ fontSize: '1rem', height: '48px' }}
                                    placeholder="Enter last name"
                                    value={lastName || ''}
                                    onChange={e => setLastName(e.target.value)}
                                    required
                                />
                            </div>

                            <div className="modern-form-group">
                                <label style={{ fontSize: '1rem', fontWeight: '600' }}>Email Address</label>
                                <input
                                    type="email"
                                    className="modern-input"
                                    style={{ fontSize: '1rem', height: '48px' }}
                                    placeholder="Enter secure email"
                                    value={email || ''}
                                    onChange={e => setEmail(e.target.value)}
                                    required
                                />
                            </div>

                            <div className="modern-form-group">
                                <label style={{ fontSize: '1rem', fontWeight: '600' }}>Phone Number</label>
                                <input
                                    type="tel"
                                    className="modern-input"
                                    style={{ fontSize: '1rem', height: '48px' }}
                                    placeholder="Enter phone number"
                                    value={phoneNumber || ''}
                                    onChange={e => setPhoneNumber(e.target.value)}
                                />
                            </div>

                            <div className="modern-form-group">
                                <label style={{ fontSize: '1rem', fontWeight: '600' }}>Access Role</label>
                                <select className="modern-input" style={{ fontSize: '1rem', height: '48px' }} value={role} onChange={e => setRole(e.target.value as 'ADMIN' | 'MANAGER' | 'WORKER')}>
                                    <option value="WORKER">WORKER</option>
                                    <option value="MANAGER">MANAGER</option>
                                    <option value="ADMIN">ADMIN</option>
                                </select>
                                {fieldErrors.role && <small style={{ color: 'red', marginTop: '0.25rem' }}>{fieldErrors.role}</small>}
                            </div>
                            <div className="modern-form-group">
                                <label style={{ fontSize: '1rem', fontWeight: '600' }}>Assigned Department</label>
                                <select className="modern-input" style={{ fontSize: '1rem', height: '48px' }} value={selectedDept} onChange={e => setSelectedDept(e.target.value)}>
                                    <option value="">- None -</option>
                                    {departments.map(d => (
                                        <option key={d.id} value={d.name}>{d.name}</option>
                                    ))}
                                </select>
                                {fieldErrors.departmentName && <small style={{ color: 'red', marginTop: '0.25rem' }}>{fieldErrors.departmentName}</small>}
                            </div>

                            <div className="modern-form-group">
                                <label style={{ fontSize: '1rem', fontWeight: '600' }}>Max Tasks per Week</label>
                                <input className="modern-input" style={{ fontSize: '1rem', height: '48px' }} type="number" placeholder="e.g. 5" value={maxTasks} onChange={e => setMaxTasks(parseInt(e.target.value, 10) || 0)} required min={1} />
                                {fieldErrors.maxTasks && <small style={{ color: 'red', marginTop: '0.25rem' }}>{fieldErrors.maxTasks}</small>}
                            </div>

                            <div className="modern-form-group" style={{ gridColumn: '1 / -1', marginTop: '0.5rem' }}>
                                <label style={{ fontSize: '1rem', fontWeight: '600', marginBottom: '0.75rem', display: 'block' }}>Personnel Skills</label>
                                {isLoading ? <p>Loading skills...</p> : (
                                    <div className="skills-grid" style={{
                                        display: 'grid',
                                        gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))',
                                        gap: '1rem',
                                        padding: '1.25rem',
                                        border: '1px solid #e2e8f0',
                                        borderRadius: '0.75rem',
                                        background: '#f8fafc'
                                    }}>
                                        {skills.map(skill => (
                                            <div key={skill.id} style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                                                <input
                                                    type="checkbox"
                                                    id={`skill-${skill.id}`}
                                                    checked={selectedSkillIds.includes(skill.id)}
                                                    onChange={() => handleSkillChange(skill.id)}
                                                    style={{ margin: 0, width: '18px', height: '18px', cursor: 'pointer' }}
                                                />
                                                <label htmlFor={`skill-${skill.id}`} style={{ fontSize: '0.95rem', color: '#334155', margin: 0, cursor: 'pointer', lineHeight: '1.2' }}>{skill.name}</label>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>

                            <div className="availability-section" style={{ gridColumn: '1 / -1', marginTop: '0.5rem' }}>
                                <div className="availability-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                                    <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: '700' }}>User Availability</h3>
                                    <button type="button" className="availability-btn" onClick={handleAddAvail} style={{ padding: '0.5rem 1rem', background: '#e0f2fe', color: '#0369a1', border: 'none', borderRadius: '0.5rem', fontWeight: '600', cursor: 'pointer' }}>+ Add Shift</button>
                                </div>

                                {availRows.map((r, i) => (
                                    <div key={i} className="availability-row">
                                        <select className="modern-input" style={{ height: '42px', fontSize: '0.9rem' }} value={r.dayOfWeek} onChange={e => handleChangeAvail(i, 'dayOfWeek', e.target.value)}>
                                            {DAYS_OF_WEEK.map(d => <option key={d} value={d}>{d}</option>)}
                                        </select>
                                        <div className="time-picker-wrapper">
                                            <DatePicker
                                                selected={timeStringToDate(r.startTime)}
                                                onChange={(date) => handleTimeChange(i, 'startTime', date)}
                                                showTimeSelect
                                                showTimeSelectOnly
                                                timeIntervals={15}
                                                timeCaption="Time"
                                                dateFormat="HH:mm"
                                                className="modern-input"
                                                wrapperClassName="datePicker-wrapper-override"
                                                portalId="root-portal"
                                                popperPlacement="bottom-start"
                                                placeholderText="Select Time"
                                                required
                                            />
                                        </div>
                                        <div className="time-picker-wrapper">
                                            <DatePicker
                                                selected={timeStringToDate(r.endTime)}
                                                onChange={(date) => handleTimeChange(i, 'endTime', date)}
                                                showTimeSelect
                                                showTimeSelectOnly
                                                timeIntervals={15}
                                                timeCaption="Time"
                                                dateFormat="HH:mm"
                                                className="modern-input"
                                                wrapperClassName="datePicker-wrapper-override"
                                                portalId="root-portal"
                                                popperPlacement="bottom-start"
                                                placeholderText="Select Time"
                                                required
                                            />
                                        </div>
                                        <button type="button" className="btn-icon delete-btn" onClick={() => handleRemoveAvail(i)} title="Remove shift">
                                            <Trash2 size={18} />
                                        </button>
                                    </div>
                                ))}
                                {availRows.length === 0 && (
                                    <div style={{ textAlign: 'center', padding: '1.5rem', background: '#f8fafc', borderRadius: '0.5rem', border: '1px dashed #cbd5e1', color: '#64748b', fontSize: '0.9rem' }}>
                                        No availability defined. Click "+ Add Shift" to set working hours.
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>

                    <div className="modal-actions">
                        <button type="button" className="btn-cancel" onClick={onClose} disabled={isSubmitting}>
                            Cancel
                        </button>
                        <button type="submit" className="btn-submit" disabled={isSubmitting}>
                            {isSubmitting ? 'Saving...' : 'Save User'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    )
}

export default UserModal;
