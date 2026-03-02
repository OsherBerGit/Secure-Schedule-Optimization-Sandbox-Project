import { useState, useEffect, useCallback } from 'react'
import type { Task, CreateTaskRequest, UpdateTaskRequest, Status, Priority, User } from '../types'
import { taskApi, statusApi, priorityApi, userApi } from '../api'
import { useAuth } from '../context/useAuth'
import TaskModal from '../components/TaskModal'
import './Tasks.css'

const Tasks = () => {
    const { user: currentUser } = useAuth()
    const [tasks, setTasks] = useState<Task[]>([])
    const [statuses, setStatuses] = useState<Status[]>([])
    const [priorities, setPriorities] = useState<Priority[]>([])
    const [workers, setWorkers] = useState<User[]>([])
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [showModal, setShowModal] = useState(false)
    const [selectedTask, setSelectedTask] = useState<Task | null>(null)

    const fetchTasks = useCallback(async () => {
        setIsLoading(true)
        try {
            const res = await taskApi.getAll()
            setTasks(res.data)
        } catch (err: unknown) {
            setError(err instanceof Error ? err.message : 'Failed to load tasks')
        } finally {
            setIsLoading(false)
        }
    }, [])

    useEffect(() => {
        void fetchTasks()
        statusApi.getAll().then(res => setStatuses(res.data)).catch(() => {})
        priorityApi.getAll().then(res => setPriorities(res.data)).catch(() => {})
        userApi.getByRole('WORKER').then(res => setWorkers(res.data)).catch(() => {})
    }, [fetchTasks])

    function handleEdit(task: Task) {
        setSelectedTask(task)
        setShowModal(true)
    }

    function handleDelete(id: number) {
        taskApi.delete(id)
            .then(() => fetchTasks())
            .catch(err => setError(err.message))
    }

    function handleSubmit(formData: CreateTaskRequest | UpdateTaskRequest) {
        if (selectedTask) {
            taskApi.update(selectedTask.id, formData as UpdateTaskRequest)
                .then(() => { setShowModal(false); setSelectedTask(null); fetchTasks() })
                .catch(err => setError(err.message))
        } else {
            taskApi.create(formData as CreateTaskRequest)
                .then(() => { setShowModal(false); fetchTasks() })
                .catch(err => setError(err.message))
        }
    }

    function handleAddTask() {
        setSelectedTask(null)
        setShowModal(true)
    }

    return (
        <div className="tasks-container">
            <div className="tasks-header">
                <h1>📋 Tasks</h1>
                {currentUser?.role === 'ADMIN' && (
                    <button className="btn-add" onClick={handleAddTask}>+ Add Task</button>
                )}
            </div>

            {error && <div className="error-message">{error}</div>}

            {isLoading ? (
                <div className="loading">Loading...</div>
            ) : (
                <table className="tasks-table">
                    <thead>
                        <tr>
                            <th>Title</th>
                            <th>Status</th>
                            <th>Priority</th>
                            <th>Assigned To</th>
                            <th>Deadline</th>
                            <th>Duration</th>
                            {currentUser?.role === 'ADMIN' && <th>Actions</th>}
                        </tr>
                    </thead>
                    <tbody>
                        {tasks.length === 0 ? (
                            <tr>
                                <td colSpan={7} className="no-data">No tasks found</td>
                            </tr>
                        ) : (
                            tasks.map(task => (
                                <tr key={task.id}>
                                    <td className="task-title">{task.title}</td>
                                    <td>
                                        <span className={`status-badge status-${task.statusName?.toLowerCase().replace(' ', '-')}`}>
                                            {task.statusName ?? '—'}
                                        </span>
                                    </td>
                                    <td>
                                        <span className={`priority-badge priority-${task.priorityName?.toLowerCase()}`}>
                                            {task.priorityName ?? '—'}
                                        </span>
                                    </td>
                                    <td>{task.assignedWorkerName ?? <span className="unassigned">Unassigned</span>}</td>
                                    <td>{task.deadline ? new Date(task.deadline).toLocaleDateString() : '—'}</td>
                                    <td>{task.durationHours != null ? `${task.durationHours}h` : '—'}</td>
                                    {currentUser?.role === 'ADMIN' && (
                                        <td>
                                            <button className="btn-edit" onClick={() => handleEdit(task)}>Edit</button>
                                            <button className="btn-delete" onClick={() => handleDelete(task.id)}>Delete</button>
                                        </td>
                                    )}
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            )}

            {showModal && (
                <TaskModal
                    task={selectedTask}
                    statuses={statuses}
                    priorities={priorities}
                    workers={workers}
                    onSubmit={handleSubmit}
                    onClose={() => { setShowModal(false); setSelectedTask(null) }}
                />
            )}
        </div>
    )
}

export default Tasks
