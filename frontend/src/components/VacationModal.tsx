import { useState } from 'react'
import type { FormEvent } from 'react'
import type { Vacation, CreateVacationRequest, UpdateVacationRequest, VacationRequestDto, User } from '../types'

interface VacationModalProps {
    vacation: Vacation | null
    isAdmin?: boolean
    workers?: User[]
    onSubmit: (data: CreateVacationRequest | UpdateVacationRequest | VacationRequestDto) => void
    onClose: () => void
}

const VacationModal = ({ vacation, isAdmin, workers = [], onSubmit, onClose }: VacationModalProps) => {
    const [workerId, setWorkerId] = useState<number | ''>(vacation?.workerId ?? '')
    const [startDate, setStartDate] = useState(vacation?.startDate ?? '')
    const [endDate, setEndDate] = useState(vacation?.endDate ?? '')
    const [error, setError] = useState<string | null>(null)

    function handleSubmit(e: FormEvent) {
        e.preventDefault()

        if (new Date(endDate) < new Date(startDate)) {
            setError('End date cannot be earlier than start date')
            return
        }
        setError(null)

        if (vacation) {
            // Edit mode (ADMIN only)
            const data: UpdateVacationRequest = { startDate, endDate }
            onSubmit(data)
        } else if (isAdmin) {
            if (!workerId) return
            // ADMIN creates directly
            const data: CreateVacationRequest = { workerId: Number(workerId), startDate, endDate }
            onSubmit(data)
        } else {
            // WORKER submits a request - no workerId needed
            const data: VacationRequestDto = { startDate, endDate }
            onSubmit(data)
        }
    }

    const title = vacation ? 'Edit Vacation' : isAdmin ? 'Add Vacation' : 'Request Vacation'

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal" onClick={e => e.stopPropagation()} style={{ maxHeight: '80vh', overflow: 'visible' }}>

                <div className="modal-header">
                    <h2>{title}</h2>
                    <button className="btn-close" onClick={onClose}>✕</button>
                </div>

                <form onSubmit={handleSubmit} className="modal-form">

                    {/* Show workerId only when ADMIN is creating (not editing) */}
                    {isAdmin && !vacation && (
                        <div className="form-group">
                            <label>Worker *</label>
                            <select
                                value={workerId}
                                onChange={e => setWorkerId(Number(e.target.value) || '')}
                                required
                            >
                                <option value="">-- Select Worker --</option>
                                {workers.map(w => (
                                    <option key={w.id} value={w.id}>{w.firstName} {w.lastName}</option>
                                ))}
                            </select>
                        </div>
                    )}

                    <div className="form-group">
                        <label>Start Date</label>
                        <input
                            type="date"
                            value={startDate}
                            onChange={e => setStartDate(e.target.value)}
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label>End Date</label>
                        <input
                            type="date"
                            value={endDate}
                            min={startDate}
                            onChange={e => setEndDate(e.target.value)}
                            required
                        />
                    </div>

                    {error && <div className="error-message" style={{ marginBottom: '1rem', padding: '0.5rem', background: '#ffebee', color: '#cc0000', borderRadius: '4px', fontSize: '0.875rem' }}>{error}</div>}

                    <div className="modal-footer">
                        <button type="button" className="btn-cancel" onClick={onClose}>Cancel</button>
                        <button type="submit" className="btn-save">
                            {vacation ? 'Save' : isAdmin ? 'Create' : 'Submit Request'}
                        </button>
                    </div>

                </form>
            </div>
        </div>
    )
}

export default VacationModal
