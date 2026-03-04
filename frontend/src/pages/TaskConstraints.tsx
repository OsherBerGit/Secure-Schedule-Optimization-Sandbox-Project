import { useState, useEffect, useCallback } from 'react'
import type { TaskConstraint, ConstraintType, Task } from '../types'
import { taskConstraintApi, constraintTypeApi, taskApi } from '../api'
import { useAuth } from '../context/useAuth'
import './LookupTable.css'

const TaskConstraints = () => {
    const { user: currentUser } = useAuth()
    const isAdmin = currentUser?.role === 'ADMIN' ||
        currentUser?.roles?.includes('ADMIN')

    const [constraints, setConstraints] = useState<TaskConstraint[]>([])
    const [tasks, setTasks] = useState<Task[]>([])
    const [constraintTypes, setConstraintTypes] = useState<ConstraintType[]>([])
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    // Create form state
    const [predId, setPredId] = useState<number | ''>('')
    const [succId, setSuccId] = useState<number | ''>('')
    const [typeId, setTypeId] = useState<number | ''>('')
    const [lag, setLag] = useState<number | ''>('')

    const fetchAll = useCallback(async () => {
        setIsLoading(true)
        try {
            const [cRes, tRes, ctRes] = await Promise.all([
                taskConstraintApi.getAll(),
                taskApi.getAll(),
                constraintTypeApi.getAll(),
            ])
            setConstraints(cRes.data)
            setTasks(tRes.data)
            setConstraintTypes(ctRes.data)
        } catch {
            setError('Failed to load task constraints')
        } finally {
            setIsLoading(false)
        }
    }, [])

    useEffect(() => { void fetchAll() }, [fetchAll])

    async function handleCreate(e: React.FormEvent) {
        e.preventDefault()
        if (predId === '' || succId === '' || typeId === '') return
        if (predId === succId) { setError('Predecessor and successor tasks cannot be the same'); return }
        try {
            await taskConstraintApi.create({
                predecessorTaskId: Number(predId),
                successorTaskId: Number(succId),
                constraintTypeId: Number(typeId),
                lagMinutes: lag !== '' ? Number(lag) : undefined,
            })
            setPredId(''); setSuccId(''); setTypeId(''); setLag('')
            await fetchAll()
        } catch {
            setError('Failed to create task constraint')
        }
    }

    async function handleDelete(id: number) {
        if (!window.confirm('Delete this task constraint?')) return
        try {
            await taskConstraintApi.delete(id)
            await fetchAll()
        } catch {
            setError('Failed to delete task constraint')
        }
    }

    return (
        <div className="lookup-container" style={{ maxWidth: '1100px' }}>
            <div className="lookup-header">
                <h1>⚙️ Task Constraints</h1>
            </div>

            {error && <div className="error-message">{error}</div>}

            {isAdmin && (
                <form className="lookup-add-form" onSubmit={handleCreate} style={{ flexWrap: 'wrap' }}>
                    <select
                        className="lookup-input"
                        value={predId}
                        onChange={e => setPredId(e.target.value === '' ? '' : Number(e.target.value))}
                        required
                    >
                        <option value="">Predecessor task...</option>
                        {tasks.map(t => <option key={t.id} value={t.id}>{t.title}</option>)}
                    </select>
                    <select
                        className="lookup-input"
                        value={succId}
                        onChange={e => setSuccId(e.target.value === '' ? '' : Number(e.target.value))}
                        required
                    >
                        <option value="">Successor task...</option>
                        {tasks.map(t => <option key={t.id} value={t.id}>{t.title}</option>)}
                    </select>
                    <select
                        className="lookup-input"
                        value={typeId}
                        onChange={e => setTypeId(e.target.value === '' ? '' : Number(e.target.value))}
                        required
                    >
                        <option value="">Constraint type...</option>
                        {constraintTypes.map(ct => <option key={ct.id} value={ct.id}>{ct.name}</option>)}
                    </select>
                    <input
                        type="number"
                        min={0}
                        placeholder="Lag (minutes)"
                        className="lookup-input"
                        style={{ maxWidth: '150px' }}
                        value={lag}
                        onChange={e => setLag(e.target.value === '' ? '' : Number(e.target.value))}
                    />
                    <button type="submit" className="btn-add">+ Add</button>
                </form>
            )}

            {isLoading ? (
                <div className="loading">Loading...</div>
            ) : (
                <table className="lookup-table">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Predecessor Task</th>
                            <th>Successor Task</th>
                            <th>Constraint Type</th>
                            <th>Lag (min)</th>
                            {isAdmin && <th>Actions</th>}
                        </tr>
                    </thead>
                    <tbody>
                        {constraints.map(c => (
                            <tr key={c.id}>
                                <td>{c.id}</td>
                                <td>{c.predecessorTaskTitle ?? `#${c.predecessorTaskId}`}</td>
                                <td>{c.successorTaskTitle ?? `#${c.successorTaskId}`}</td>
                                <td><span className="lookup-badge">{c.constraintTypeName ?? `#${c.constraintTypeId}`}</span></td>
                                <td>{c.lagMinutes ?? 0}</td>
                                {isAdmin && (
                                    <td>
                                        <button className="btn-delete" onClick={() => handleDelete(c.id)}>Delete</button>
                                    </td>
                                )}
                            </tr>
                        ))}
                        {constraints.length === 0 && (
                            <tr><td colSpan={isAdmin ? 6 : 5} style={{ textAlign: 'center', color: '#aaa' }}>No constraints found</td></tr>
                        )}
                    </tbody>
                </table>
            )}
        </div>
    )
}

export default TaskConstraints
