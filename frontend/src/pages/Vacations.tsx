import { useState, useEffect, useCallback } from 'react'
import type { Vacation, CreateVacationRequest, UpdateVacationRequest, VacationRequestDto, User } from '../types'
import { vacationApi, userApi } from '../api'
import { useAuth } from '../context/useAuth'
import VacationModal from '../components/VacationModal'
import './Vacations.css'

const Vacations = () => {
    const { user: currentUser } = useAuth()
    const isAdmin = currentUser?.role === 'ADMIN'
    const isManager = currentUser?.role === 'MANAGER'
    const canManage = isAdmin || isManager

    const [vacations, setVacations] = useState<Vacation[]>([])
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [showModal, setShowModal] = useState(false)
    const [selectedVacation, setSelectedVacation] = useState<Vacation | null>(null)
    const [users, setUsers] = useState<User[]>([])

    // Filters
    const [filterDepartment, setFilterDepartment] = useState<string>('')
    const [filterWorker, setFilterWorker] = useState<string>('')

    const fetchVacations = useCallback(async () => {
        setIsLoading(true)
        try {
            const [vacRes, usersRes] = await Promise.all([
                vacationApi.getAll(),
                canManage ? userApi.getAll() : Promise.resolve({ data: [] })
            ])
            setVacations(vacRes.data)
            setUsers(usersRes.data)
        } catch (err: unknown) {
            setError(err instanceof Error ? err.message : 'Failed to load vacations')
        } finally {
            setIsLoading(false)
        }
    }, [canManage])

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
            const requestData = {
                workerId: Number(currentUser?.id),
                startDate: new Date(formData.startDate).toISOString().split('T')[0],
                endDate: new Date(formData.endDate).toISOString().split('T')[0],
                status: 'PENDING'
            } as unknown as VacationRequestDto;

            vacationApi.request(requestData)
                .then(() => { setShowModal(false); fetchVacations() })
                .catch(err => setError(err.message))
        }
    }

    function handleAddVacation() {
        setSelectedVacation(null)
        setShowModal(true)
    }

    const getDepartmentName = (v: Vacation) => {
        const u = users.find(user => user.id === v.workerId)
        return u?.departmentName || ''
    }

    const availableDepartments = Array.from(new Set(vacations.map(getDepartmentName).filter(Boolean))) as string[]
    const availableWorkers = Array.from(new Set(vacations.filter(v => filterDepartment ? getDepartmentName(v) === filterDepartment : true).map(v => v.workerName).filter(Boolean))) as string[]

    const displayedVacations = currentUser?.role === 'WORKER'
        ? vacations.filter(v => v.workerId === currentUser.id)
        : vacations.filter(v => {
            if (filterDepartment && getDepartmentName(v) !== filterDepartment) return false
            if (filterWorker && v.workerName !== filterWorker) return false
            return true
        })

    return (
        <div className="vacations-container">
            <div className="vacations-header">
                <h1>🏖️ Vacations</h1>
                <button className="btn-add" onClick={handleAddVacation}>
                    {canManage ? '+ Add Vacation' : '+ Request Vacation'}
                </button>
            </div>

            {error && <div className="error-message">{error}</div>}

            {canManage && (
                <div className="filter-row" style={{ marginBottom: '1rem', display: 'flex', gap: '1rem' }}>
                    <select
                        className="modern-select"
                        value={filterDepartment}
                        onChange={e => { setFilterDepartment(e.target.value); setFilterWorker(''); }}
                    >
                        <option value="">All Departments</option>
                        {availableDepartments.map(d => (
                            <option key={d} value={d}>{d}</option>
                        ))}
                    </select>

                    <select
                        className="modern-select"
                        value={filterWorker}
                        onChange={e => setFilterWorker(e.target.value)}
                    >
                        <option value="">All Workers</option>
                        {availableWorkers.map(w => (
                            <option key={w} value={w}>{w}</option>
                        ))}
                    </select>
                </div>
            )}

            {isLoading ? (
                <div className="loading">Loading...</div>
            ) : displayedVacations.length === 0 ? (
                <div className="empty-state">No records found matching the selected filters.</div>
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
                        {displayedVacations.map(vacation => {
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
                        }
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
