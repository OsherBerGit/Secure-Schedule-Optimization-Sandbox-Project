import { useState } from 'react'
import DatePicker from 'react-datepicker'
import 'react-datepicker/dist/react-datepicker.css'
import { format } from 'date-fns'
import { X, Plane } from 'lucide-react'
import type { Vacation, User } from '../types'
import './VacationModal.css'

interface VacationModalProps {
    vacation: Vacation | null
    isAdmin: boolean
    users: User[]
    onSubmit: (data: any) => void
    onClose: () => void
}

const VacationModal = ({ vacation, isAdmin, users, onSubmit, onClose }: VacationModalProps) => {
    const [userId, setUserId] = useState<number | ''>(vacation?.userId || '')
    const [startDate, setStartDate] = useState<Date | null>(vacation ? new Date(vacation.startDate) : null)
    const [endDate, setEndDate] = useState<Date | null>(vacation ? new Date(vacation.endDate) : null)

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault()
        if (!startDate || !endDate || (isAdmin && !userId)) return

        onSubmit({
            userId: Number(userId),
            startDate: format(startDate, 'yyyy-MM-dd'),
            endDate: format(endDate, 'yyyy-MM-dd'),
            status: vacation?.statusName || 'PENDING'
        })
    }

    return (
        <div className="modal-overlay">
            <div className="modern-modal-card">
                <div className="modal-header">
                    <h2><Plane size={22} className="text-primary" /> {vacation ? 'Edit Vacation' : 'New Vacation Request'}</h2>
                    <button type="button" className="modern-close-btn" onClick={onClose}>
                        <X size={24} />
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="modern-modal-form">
                    <div className="modal-body">
                        {isAdmin && (
                            <div className="modern-form-group">
                                <label>Select Employee *</label>
                                <select
                                    className="modern-input"
                                    value={userId}
                                    onChange={e => setUserId(Number(e.target.value))}
                                    required
                                >
                                    <option value="">-- Select User --</option>
                                    {users.map(w => (
                                        <option key={w.id} value={w.id}>{w.firstName} {w.lastName}</option>
                                    ))}
                                </select>
                            </div>
                        )}

                        <div className="dates-grid">
                            <div className="modern-form-group">
                                <label>Start Date *</label>
                                <DatePicker
                                    className="modern-input"
                                    selected={startDate}
                                    onChange={(date: Date | null) => {
                                        setStartDate(date)
                                        if (date && endDate && date > endDate)
                                            setEndDate(null)
                                    }}
                                    dateFormat="yyyy-MM-dd"
                                    portalId="root-portal"
                                    popperPlacement="bottom-start"
                                    calendarClassName="vacation-calendar"
                                    minDate={new Date()}
                                    showIcon
                                    placeholderText="Pick start date"
                                    required
                                />
                            </div>

                            <div className="modern-form-group">
                                <label>End Date *</label>
                                <DatePicker
                                    className="modern-input"
                                    selected={endDate}
                                    onChange={(date: Date | null) => setEndDate(date)}
                                    minDate={startDate || new Date()}
                                    dateFormat="yyyy-MM-dd"
                                    portalId="root-portal"
                                    popperPlacement="bottom-start"
                                    calendarClassName="vacation-calendar"
                                    showIcon
                                    placeholderText="Pick end date"
                                    required
                                />
                            </div>
                        </div>
                    </div>

                    <div className="modal-actions">
                        <button type="button" className="btn-cancel" onClick={onClose}>Cancel</button>
                        <button type="submit" className="btn-submit" disabled={!startDate || !endDate}>
                            {vacation ? 'Update' : 'Submit Request'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    )
}

export default VacationModal