import { useState, useEffect, useCallback, useMemo } from 'react'
import type { Vacation, CreateVacationRequest, UpdateVacationRequest, VacationRequestDto, User } from '../types'
import { vacationApi, userApi } from '../api'
import { useAuth } from '../context/useAuth'
import VacationModal from '../components/VacationModal'
import { useLocation, useSearchParams } from 'react-router-dom'
import { Plane, Plus, Check, X, Pencil, Trash2, Calendar, Search } from 'lucide-react'
import './Vacations.css'

const Vacations = () => {
    const { user: currentUser } = useAuth()
    const location = useLocation()
    const [searchParams, setSearchParams] = useSearchParams()
    const queryWorkerId = searchParams.get('workerId')

    const isAdmin = currentUser?.role === 'ADMIN'
    const isManager = currentUser?.role === 'MANAGER'
    const canManage = isAdmin || isManager

    const [vacations, setVacations] = useState<Vacation[]>([])
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [showModal, setShowModal] = useState(false)
    const [selectedVacation, setSelectedVacation] = useState<Vacation | null>(null)
    const [users, setUsers] = useState<User[]>([])

    const [filterDepartment, setFilterDepartment] = useState<string>('')
    const [filterWorker, setFilterWorker] = useState<string>('')

    useEffect(() => {
        if (location.state?.filterWorkerName)
            setFilterWorker(location.state.filterWorkerName)
    }, [location.state])

    useEffect(() => {
        const workerId = searchParams.get('workerId')
        if (workerId && users.length > 0) {
            const user = users.find(u => u.id === Number(workerId))
            if (user)
                setFilterWorker(`${user.firstName} ${user.lastName}`)
        }
    }, [searchParams, users])

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
        if (window.confirm('Are you sure you want to delete this record?')) {
            vacationApi.delete(id)
                .then(() => fetchVacations())
                .catch(err => setError(err.message))
        }
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

    const getDepartmentName = useCallback((v: Vacation) => {
        const u = users.find(user => user.id === v.workerId)
        return u?.departmentName || ''
    }, [users])

    const availableDepartments = useMemo(() =>
            Array.from(new Set(vacations.map(getDepartmentName).filter(Boolean))),
        [vacations, getDepartmentName])

    const displayedVacations = useMemo(() => {
        let list = vacations;
        if (currentUser?.role === 'WORKER')
            list = list.filter(v => v.workerId === currentUser.id);
        else {
            list = list.filter(v => {
                if (filterDepartment && getDepartmentName(v) !== filterDepartment) return false
                if (filterWorker && !v.workerName.toLowerCase().includes(filterWorker.toLowerCase())) return false
                return true
            });
        }
        return list.sort((a, b) => new Date(b.startDate).getTime() - new Date(a.startDate).getTime());
    }, [vacations, currentUser, filterDepartment, filterWorker, getDepartmentName])

    return (
        <div className="vacations-page">
            <div className="page-header">
                <div className="page-header-title">
                    <Plane className="text-primary" size={28} color="var(--primary-color)" />
                    <h1>Vacations Management</h1>
                </div>
                <button className="btn-add-primary" onClick={handleAddVacation}>
                    <Plus size={18} /> {canManage ? 'Add Vacation' : 'Request Vacation'}
                </button>
            </div>

            <div className="filters-container">
                {canManage && (
                    <>
                        <div className="search-wrapper" style={{ flex: 1 }}>
                            <Search className="search-icon" size={18} />
                            <input
                                type="text"
                                className="modern-input search-input"
                                placeholder="Search by worker name..."
                                value={filterWorker}
                                onChange={e => {
                                    setFilterWorker(e.target.value)
                                    if (searchParams.has('workerId')) {
                                        searchParams.delete('workerId')
                                        setSearchParams(searchParams)
                                    }
                                }}
                            />
                        </div>

                        <select
                            className="modern-input"
                            style={{ flex: '0 0 200px' }}
                            value={filterDepartment}
                            onChange={e => { setFilterDepartment(e.target.value); setFilterWorker(''); }}
                        >
                            <option value="">All Departments</option>
                            {availableDepartments.map(d => (
                                <option key={d} value={d}>{d}</option>
                            ))}
                        </select>
                    </>
                )}
                {!canManage && <div className="modern-input disabled-display">Viewing your requests</div>}
            </div>

            {error && <div className="error-message">{error}</div>}

            <div className="table-container">
                {isLoading ? (
                    <div className="loading-state">Loading vacations data...</div>
                ) : displayedVacations.length === 0 ? (
                    <div className="empty-state">No vacation records found.</div>
                ) : (
                    <table className="modern-table vacations-table">
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
                            const status = vacation.statusName ?? 'PENDING'

                            return (
                                <tr key={vacation.id}>
                                    <td style={{ fontWeight: 500 }}>{vacation.workerName}</td>
                                    <td>
                                        <div className="date-cell">
                                            <Calendar size={14} style={{ opacity: 0.5 }} />
                                            {new Date(vacation.startDate).toLocaleDateString()}
                                        </div>
                                    </td>
                                    <td>
                                        <div className="date-cell">
                                            <Calendar size={14} style={{ opacity: 0.5 }} />
                                            {new Date(vacation.endDate).toLocaleDateString()}
                                        </div>
                                    </td>
                                    <td>
                                            <span className="duration-badge">
                                                {days} {days === 1 ? 'day' : 'days'}
                                            </span>
                                    </td>
                                    <td>
                                            <span className={`status-badge status-${status.toLowerCase()}`}>
                                                {status}
                                            </span>
                                    </td>
                                    <td className="actions-cell">
                                        {canManage && status === 'PENDING' && (
                                            <>
                                                <button className="btn-icon approve-btn" onClick={() => handleApprove(vacation.id)} title="Approve">
                                                    <Check size={18} />
                                                </button>
                                                <button className="btn-icon reject-btn" onClick={() => handleReject(vacation.id)} title="Reject">
                                                    <X size={18} />
                                                </button>
                                            </>
                                        )}
                                        {canManage && (
                                            <button className="btn-icon edit-btn" onClick={() => handleEdit(vacation)} title="Edit">
                                                <Pencil size={16} />
                                            </button>
                                        )}
                                        {(canManage || status === 'PENDING') && (
                                            <button className="btn-icon delete-btn" onClick={() => handleDelete(vacation.id)} title="Delete">
                                                <Trash2 size={16} />
                                            </button>
                                        )}
                                    </td>
                                </tr>
                            )
                        })}
                        </tbody>
                    </table>
                )}
            </div>

            {showModal && (
                <VacationModal
                    vacation={selectedVacation}
                    isAdmin={!!canManage}
                    workers={users}
                    onSubmit={handleSubmit}
                    onClose={() => { setShowModal(false); setSelectedVacation(null) }}
                />
            )}
        </div>
    )
}

export default Vacations