import { useState, useEffect, useCallback } from 'react'
import { CheckCircle } from 'lucide-react'
import type { Settlement, CreateSettlementRequest, Task, User } from '../types'
import { settlementApi, taskApi, userApi } from '../api'
import { useAuth } from '../context/useAuth'
import SettlementModal from '../components/SettlementModal'
import './Settlements.css'

// Jackson may serialize LocalDateTime as an array [2025,3,4,10,30,0] or ISO string.
// This helper handles both formats safely.
function formatDate(value: string | number[] | null | undefined): string {
    if (!value) return '-'
    if (Array.isArray(value)) {
        const [y, mo, d, h = 0, m = 0] = value as number[]
        return new Date(y, mo - 1, d, h, m).toLocaleString()
    }
    const d = new Date(value as string)
    return isNaN(d.getTime()) ? String(value) : d.toLocaleString()
}

const Settlements = () => {
    const { user: currentUser } = useAuth()
    const isAdmin = currentUser?.role === 'ADMIN'

    const [settlements, setSettlements] = useState<Settlement[]>([])
    const [tasks, setTasks] = useState<Task[]>([])
    const [workers, setWorkers] = useState<User[]>([])
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [showModal, setShowModal] = useState(false)

    const fetchSettlements = useCallback(async () => {
        setIsLoading(true)
        setError(null)
        try {
            // Admins see all; workers see only their own
            const res = isAdmin
                ? await settlementApi.getAll()
                : await settlementApi.getMySettlements()
            setSettlements(res.data)
        } catch (err: unknown) {
            setError(err instanceof Error ? err.message : 'Failed to load settlements')
        } finally {
            setIsLoading(false)
        }
    }, [isAdmin])

    useEffect(() => {
        void fetchSettlements()
        taskApi.getAll().then(res => setTasks(res.data)).catch(() => {})
        userApi.getByRole('WORKER').then(res => setWorkers(res.data)).catch(() => {})
    }, [fetchSettlements])

    function handleDelete(id: number) {
        settlementApi.delete(id)
            .then(() => fetchSettlements())
            .catch(err => setError(err instanceof Error ? err.message : 'Failed to delete'))
    }

    function handleComplete(id: number) {
        settlementApi.completeSettlement(id)
            .then(() => fetchSettlements())
            .catch(err => setError(err instanceof Error ? err.message : 'Failed to mark as done'))
    }

    function handleSubmit(formData: CreateSettlementRequest) {
        settlementApi.create(formData)
            .then(() => { setShowModal(false); fetchSettlements() })
            .catch(err => setError(err instanceof Error ? err.message : 'Failed to save'))
    }

    return (
        <div className="settlements-container">
            <div className="settlements-header">
                <h1>💰 Settlements</h1>
                {isAdmin && (
                    <button className="btn-add" onClick={() => setShowModal(true)}>+ Add Settlement</button>
                )}
            </div>

            {error && <div className="error-message">{error}</div>}

            {isLoading ? (
                <div className="loading">Loading...</div>
            ) : (
                <table className="settlements-table">
                    <thead>
                        <tr>
                            <th>Worker</th>
                            <th>Task</th>
                            <th>Status</th>
                            <th>Settlement Date</th>
                            <th>Completion Date</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {settlements.length === 0 ? (
                            <tr>
                                <td colSpan={6} className="no-data">No settlements found</td>
                            </tr>
                        ) : (
                            settlements.map(s => {
                                const isCompleted = s.statusName === 'COMPLETED'
                                return (
                                    <tr key={s.id}>
                                        <td>{s.workerName}</td>
                                        <td className="task-title">{s.taskTitle}</td>
                                        <td>
                                            <span
                                                className="status-badge"
                                                style={s.statusColorCode ? { background: s.statusColorCode + '22', color: s.statusColorCode, border: `1px solid ${s.statusColorCode}44` } : undefined}
                                            >
                                                {s.statusName ?? '-'}
                                            </span>
                                        </td>
                                        <td>{formatDate(s.settlementDate)}</td>
                                        <td>
                                            {s.completionDate
                                                ? formatDate(s.completionDate)
                                                : <span className="pending-badge">Pending</span>
                                            }
                                        </td>
                                        <td className="actions-cell">
                                            {!isCompleted && (
                                                <button
                                                    className="btn-complete"
                                                    title="Mark as Done"
                                                    onClick={() => handleComplete(s.id)}
                                                >
                                                    <CheckCircle size={16} />
                                                    <span>Mark as Done</span>
                                                </button>
                                            )}
                                            {isAdmin && (
                                                <button className="btn-delete" onClick={() => handleDelete(s.id)}>Delete</button>
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
                <SettlementModal
                    tasks={tasks}
                    workers={workers}
                    onSubmit={handleSubmit}
                    onClose={() => setShowModal(false)}
                />
            )}
        </div>
    )
}

export default Settlements

