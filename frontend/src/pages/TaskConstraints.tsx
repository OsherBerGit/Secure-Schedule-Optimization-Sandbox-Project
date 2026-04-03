import { useState, useEffect, useCallback, useMemo } from 'react'
import type { TaskConstraint, ConstraintType, Task, Department } from '../types'
import { taskConstraintApi, constraintTypeApi, taskApi, departmentApi } from '../api'
import { useAuth } from '../context/useAuth'
import TaskConstraintModal from '../components/TaskConstraintModal'
import './LookupTable.css'
import './Tasks.css' // Import to use filter-row and layout styles if needed

const TaskConstraints = () => {
    const { user: currentUser } = useAuth()
    const isAdmin = currentUser?.role === 'ADMIN'
    const isManager = currentUser?.role === 'MANAGER'
    const canManage = isAdmin || isManager

    const [constraints, setConstraints] = useState<TaskConstraint[]>([])
    const [tasks, setTasks] = useState<Task[]>([])
    const [constraintTypes, setConstraintTypes] = useState<ConstraintType[]>([])
    const [departments, setDepartments] = useState<Department[]>([])
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const [showModal, setShowModal] = useState(false)

    const [search, setSearch] = useState('')
    const [filterDepartment, setFilterDepartment] = useState<string>('')
    const [filterConstraintType, setFilterConstraintType] = useState<string>('')

    const fetchAll = useCallback(async () => {
        setIsLoading(true)
        try {
            const [cRes, tRes, ctRes, dRes] = await Promise.all([
                taskConstraintApi.getAll(),
                taskApi.getAll(),
                constraintTypeApi.getAll(),
                departmentApi.getAll()
            ])
            setConstraints(cRes.data)
            setTasks(tRes.data)
            setConstraintTypes(ctRes.data)
            setDepartments(dRes.data)
        } catch {
            setError('Failed to load task constraints')
        } finally {
            setIsLoading(false)
        }
    }, [])

    useEffect(() => { void fetchAll() }, [fetchAll])

    async function handleCreate(data: { predecessorTaskId: number, successorTaskId: number, constraintTypeId: number, lagMinutes?: number }) {
        if (data.predecessorTaskId === data.successorTaskId) { setError('Predecessor and successor tasks cannot be the same'); return }
        try {
            await taskConstraintApi.create(data)
            setShowModal(false)
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

    const filteredConstraints = useMemo(() => {
        return constraints.filter(c => {
            // Task titles might act as search scope, or IDs
            const searchLower = search.toLowerCase()
            const predStr = (c.predecessorTaskTitle || '').toLowerCase()
            const succStr = (c.successorTaskTitle || '').toLowerCase()
            const typeStr = (c.constraintTypeName || '').toLowerCase()

            const matchesSearch = search === '' ||
                predStr.includes(searchLower) ||
                succStr.includes(searchLower) ||
                typeStr.includes(searchLower)

            // Filtering by department: We check if the predecessor or successor task belongs to the selected department.
            // Since we need to look up the task's department, we find the task in `tasks` array.
            const matchesDept = filterDepartment === '' || (() => {
                const predTask = tasks.find(t => t.id === c.predecessorTaskId)
                const succTask = tasks.find(t => t.id === c.successorTaskId)
                return predTask?.departmentName === filterDepartment || succTask?.departmentName === filterDepartment
            })()

            const matchesType = filterConstraintType === '' || c.constraintTypeName === filterConstraintType

            return matchesSearch && matchesDept && matchesType
        })
    }, [constraints, tasks, search, filterDepartment, filterConstraintType])

    return (
        <div className="lookup-container" style={{ maxWidth: '1100px' }}>
            <div className="lookup-header">
                <h1>⚙️ Task Constraints</h1>
                {canManage && (
                    <button className="btn-add" onClick={() => setShowModal(true)}>
                        + Add Constraint
                    </button>
                )}
            </div>

            {error && <div className="error-message">{error}</div>}

            <div className="filter-row" style={{ display: 'flex', gap: '1rem', marginBottom: '1rem' }}>
                <input
                    type="text"
                    className="modern-input"
                    placeholder="🔍 Search tasks or type..."
                    value={search}
                    onChange={e => setSearch(e.target.value)}
                    style={{ minWidth: '250px' }}
                />

                {canManage && (
                    <select
                        className="modern-select"
                        value={filterDepartment}
                        onChange={e => setFilterDepartment(e.target.value)}
                    >
                        <option value="">All Departments</option>
                        {departments.map(d => (
                            <option key={d.id} value={d.name}>{d.name}</option>
                        ))}
                    </select>
                )}

                <select
                    className="modern-select"
                    value={filterConstraintType}
                    onChange={e => setFilterConstraintType(e.target.value)}
                >
                    <option value="">All Constraint Types</option>
                    {constraintTypes.map(ct => (
                        <option key={ct.id} value={ct.name}>{ct.name}</option>
                    ))}
                </select>
            </div>

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
                            {canManage && <th>Actions</th>}
                        </tr>
                    </thead>
                    <tbody>
                        {filteredConstraints.map(c => (
                            <tr key={c.id}>
                                <td>{c.id}</td>
                                <td>{c.predecessorTaskTitle ?? `#${c.predecessorTaskId}`}</td>
                                <td>{c.successorTaskTitle ?? `#${c.successorTaskId}`}</td>
                                <td><span className="lookup-badge">{c.constraintTypeName ?? `#${c.constraintTypeId}`}</span></td>
                                <td>{c.lagMinutes ?? 0}</td>
                                {canManage && (
                                    <td>
                                        <button className="btn-delete" onClick={() => handleDelete(c.id)}>Delete</button>
                                    </td>
                                )}
                            </tr>
                        ))}
                        {filteredConstraints.length === 0 && (
                            <tr><td colSpan={canManage ? 6 : 5} style={{ textAlign: 'center', color: '#aaa' }}>No constraints found</td></tr>
                        )}
                    </tbody>
                </table>
            )}

            {showModal && canManage && (
                <TaskConstraintModal
                    tasks={tasks}
                    constraintTypes={constraintTypes}
                    onSubmit={handleCreate}
                    onClose={() => setShowModal(false)}
                />
            )}
        </div>
    )
}

export default TaskConstraints
