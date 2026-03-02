import { useState, useEffect, useCallback } from 'react'
import type { Settlement, CreateSettlementRequest, Task, User } from '../types'
import { settlementApi, taskApi, userApi } from '../api'
import { useAuth } from '../context/useAuth'
import SettlementModal from '../components/SettlementModal'
import './Settlements.css'

const Settlements = () => {
    const { user: currentUser } = useAuth()
    const [settlements, setSettlements] = useState<Settlement[]>([])
    const [tasks, setTasks] = useState<Task[]>([])
    const [workers, setWorkers] = useState<User[]>([])
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [showModal, setShowModal] = useState(false)

    const fetchSettlements = useCallback(async () => {
        setIsLoading(true)
        try {
            const res = await settlementApi.getAll()
            setSettlements(res.data)
        } catch (err: unknown) {
            setError(err instanceof Error ? err.message : 'Failed to load settlements')
        } finally {
            setIsLoading(false)
        }
    }, [])

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

    function handleSubmit(formData: CreateSettlementRequest) {
        settlementApi.create(formData)
            .then(() => { setShowModal(false); fetchSettlements() })
            .catch(err => setError(err instanceof Error ? err.message : 'Failed to save'))
    }

    return (
        <div className="settlements-container">
            <div className="settlements-header">
                <h1>💰 Settlements</h1>
                {currentUser?.role === 'ADMIN' && (
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
                            <th>Settlement Date</th>
                            <th>Completion Date</th>
                            {currentUser?.role === 'ADMIN' && <th>Actions</th>}
                        </tr>
                    </thead>
                    <tbody>
                        {settlements.length === 0 ? (
                            <tr>
                                <td colSpan={5} className="no-data">No settlements found</td>
                            </tr>
                        ) : (
                            settlements.map(s => (
                                <tr key={s.id}>
                                    <td>{s.workerName}</td>
                                    <td className="task-title">{s.taskTitle}</td>
                                    <td>{new Date(s.settlementDate).toLocaleDateString()}</td>
                                    <td>
                                        {s.completionDate
                                            ? new Date(s.completionDate).toLocaleDateString()
                                            : <span className="pending-badge">Pending</span>
                                        }
                                    </td>
                                    {currentUser?.role === 'ADMIN' && (
                                        <td>
                                            <button className="btn-delete" onClick={() => handleDelete(s.id)}>Delete</button>
                                        </td>
                                    )}
                                </tr>
                            ))
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

