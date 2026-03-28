import { useState, useEffect, useCallback } from 'react'
import type { Vacation, CreateVacationRequest, UpdateVacationRequest, VacationRequestDto } from '../types'
import { vacationApi } from '../api'
import { useAuth } from '../context/useAuth'
import VacationModal from '../components/VacationModal'
import './Vacations.css'

const Vacations = () => {
    const { user: currentUser } = useAuth()
    const isAdmin = currentUser?.role === 'ADMIN' || currentUser?.roles?.includes('ADMIN')
    const isManager = currentUser?.role === 'MANAGER' || currentUser?.roles?.includes('MANAGER')
    const canManage = isAdmin || isManager

    const [vacations, setVacations] = useState<Vacation[]>([])
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [showModal, setShowModal] = useState(false)
    const [selectedVacation, setSelectedVacation] = useState<Vacation | null>(null)

    const fetchVacations = useCallback(async () => {
        setIsLoading(true)
        try {
            const response = await vacationApi.getAll()
            setVacations(response.data)
        } catch (err: unknown) {
            setError(err instanceof Error ? err.message : 'Failed to load vacations')
        } finally {
            setIsLoading(false)
        }
    }, [])

    useEffect(() => { void fetchVacations() }, [fetchVacations])

    function handleEdit(vacation: Vacation) {
        setSelectedVacation(vacation)
        setShowModal(true)
    }

    function handleDelete(id: number) {
        vacationApi.delete(id)
            .then(() => fetchVacations())
            .catch(err => setError(err.message))
    }

    function handleApprove(id: number) {
        vacationApi.updateStatus(id, { status: 'APPROVED' })
            .then(() => fetchVacations())
            .catch(err => setError(err.message))
    }

    function handleReject(id: number) {
        vacationApi.updateStatus(id, { status: 'REJECTED' })
            .then(() => fetchVacations())
            .catch(err => setError(err.message))
    }

    function handleSubmit(formData: CreateVacationRequest | UpdateVacationRequest | VacationRequestDto) {
        if (selectedVacation) {
            vacationApi.update(selectedVacation.id, formData as UpdateVacationRequest)
                .then(() => { setShowModal(false); setSelectedVacation(null); fetchVacations() })
                .catch(err => setError(err.message))
        } else if (canManage) {
            vacationApi.create(formData as CreateVacationRequest)
                .then(() => { setShowModal(false); fetchVacations() })
                .catch(err => setError(err.message))
        } else {
            vacationApi.request(formData as VacationRequestDto)
                .then(() => { setShowModal(false); fetchVacations() })
                .catch(err => setError(err.message))
        }
    }

    function handleAddVacation() {
        setSelectedVacation(null)
        setShowModal(true)
    }

    return (
        <div className="vacations-container">
            <div className="vacations-header">
                <h1>🏖️ Vacations</h1>
                <button className="btn-add" onClick={handleAddVacation}>
                    {canManage ? '+ Add Vacation' : '+ Request Vacation'}
                </button>
            </div>

            {error && <div className="error-message">{error}</div>}

            {isLoading ? (
                <div className="loading">Loading...</div>
            ) : (
                <table className="vacations-table">
                    <thead>
                        <tr>
                            <th>Worker</th>
                            <th>Start Date</th>
                            <th>End Date</th>
                            <th>Duration</th>
                            <th>Status</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {vacations.length === 0 ? (
                            <tr>
                                <td colSpan={6} className="no-data">No vacations found</td>
                            </tr>
                        ) : (
                            vacations.map(vacation => {
                                const start = new Date(vacation.startDate)
                                const end = new Date(vacation.endDate)
                                const days = Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1
                                const status = vacation.statusName ?? 'UNKNOWN'
                                return (
                                    <tr key={vacation.id}>
                                        <td>{vacation.workerName}</td>
                                        <td>{new Date(vacation.startDate).toLocaleDateString()}</td>
                                        <td>{new Date(vacation.endDate).toLocaleDateString()}</td>
                                        <td><span className="duration-badge">{days} day{days > 1 ? 's' : ''}</span></td>
                                        <td>
                                            <span className={`status-badge status-${status.toLowerCase()}`}>
                                                {status}
                                            </span>
                                        </td>
                                        <td>
                                            {canManage && status === 'PENDING' && (
                                                <>
                                                    <button className="btn-approve" onClick={() => handleApprove(vacation.id)}>✓ Approve</button>
                                                    <button className="btn-reject" onClick={() => handleReject(vacation.id)}>✗ Reject</button>
                                                </>
                                            )}
                                            {canManage && (
                                                <>
                                                    <button className="btn-edit" onClick={() => handleEdit(vacation)}>Edit</button>
                                                    <button className="btn-delete" onClick={() => handleDelete(vacation.id)}>Delete</button>
                                                </>
                                            )}
                                        </td>
                                    </tr>
                                )
                            })
                        )}
                    </tbody>
                </table>
            )}

            {showModal && (
                <VacationModal
                    vacation={selectedVacation}
                    isAdmin={!!canManage}
                    onSubmit={handleSubmit}
                    onClose={() => { setShowModal(false); setSelectedVacation(null) }}
                />
            )}
        </div>
    )
}

export default Vacations

