import { useState } from 'react'
import type { FormEvent } from 'react'
import type { Vacation, CreateVacationRequest, UpdateVacationRequest, VacationRequestDto } from '../types'

interface VacationModalProps {
    vacation: Vacation | null
    isAdmin?: boolean
    onSubmit: (data: CreateVacationRequest | UpdateVacationRequest | VacationRequestDto) => void
    onClose: () => void
}

const VacationModal = ({ vacation, isAdmin, onSubmit, onClose }: VacationModalProps) => {
    const [workerId, setWorkerId] = useState<number>(vacation?.workerId ?? 0)
    const [startDate, setStartDate] = useState(vacation?.startDate ?? '')
    const [endDate, setEndDate] = useState(vacation?.endDate ?? '')

    function handleSubmit(e: FormEvent) {
        e.preventDefault()
        if (vacation) {
            // Edit mode (ADMIN only)
            const data: UpdateVacationRequest = { startDate, endDate }
            onSubmit(data)
        } else if (isAdmin) {
            // ADMIN creates directly
            const data: CreateVacationRequest = { workerId, startDate, endDate }
            onSubmit(data)
        } else {
            // WORKER submits a request — no workerId needed
            const data: VacationRequestDto = { startDate, endDate }
            onSubmit(data)
        }
    }

    const title = vacation ? 'Edit Vacation' : isAdmin ? 'Add Vacation' : 'Request Vacation'

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal" onClick={e => e.stopPropagation()}>

                <div className="modal-header">
                    <h2>{title}</h2>
                    <button className="btn-close" onClick={onClose}>✕</button>
                </div>

                <form onSubmit={handleSubmit} className="modal-form">

                    {/* Show workerId only when ADMIN is creating (not editing) */}
                    {isAdmin && !vacation && (
                        <div className="form-group">
                            <label>Worker ID</label>
                            <input
                                type="number"
                                value={workerId}
                                onChange={e => setWorkerId(Number(e.target.value))}
                                required
                                min={1}
                            />
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
                            onChange={e => setEndDate(e.target.value)}
                            required
                        />
                    </div>

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
