import React, { useEffect, useState, useMemo, useCallback } from 'react';
import { taskApi, departmentApi, skillApi, statusApi, priorityApi } from '../api';
import type { Task, Department, Skill, Status, Priority } from '../types';
import TaskModal from '../components/TaskModal';
import { useNavigate } from 'react-router-dom';
import { Search, Pencil, Trash2, ShieldAlert, ClipboardList } from 'lucide-react';
import { usePermissions } from '../hooks/usePermissions';
import './Tasks.css';

const Tasks: React.FC = () => {
    const { canEdit: canManage } = usePermissions();

    const [tasks, setTasks] = useState<Task[]>([]);
    const [departments, setDepartments] = useState<Department[]>([]);
    const [skills, setSkills] = useState<Skill[]>([]);
    const [statuses, setStatuses] = useState<Status[]>([]);
    const [priorities, setPriorities] = useState<Priority[]>([]);

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [selectedTask, setSelectedTask] = useState<Task | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(false);

    const [searchQuery, setSearchQuery] = useState('');
    const [statusFilter, setStatusFilter] = useState('');
    const [priorityFilter, setPriorityFilter] = useState('');
    const [skillFilter, setSkillFilter] = useState('');

    const navigate = useNavigate();

    const fetchTasks = useCallback(async () => {
        setIsLoading(true);
        try {
            const res = await taskApi.getAll();
            setTasks(res.data);
        } catch (err: any) {
            setError(err.message || 'Failed to fetch tasks');
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchTasks();

        statusApi.getAll().then(res => {
            setStatuses(res.data);
        }).catch(() => {
            setStatuses([
                { id: 1, name: 'OPEN' },
                { id: 2, name: 'LOCKED' },
                { id: 3, name: 'SCHEDULED' },
                { id: 4, name: 'CLOSED' }
            ] as Status[]);
        });

        priorityApi.getAll().then(res => setPriorities(res.data)).catch(() => {
            setPriorities([
                { id: 1, name: 'LOW' },
                { id: 2, name: 'NORMAL' },
                { id: 3, name: 'HIGH' },
                { id: 4, name: 'URGENT' }
            ] as Priority[]);
        });

        departmentApi.getAll().then(res => setDepartments(res.data)).catch(() => {});
        skillApi.getAll().then(res => setSkills(res.data)).catch(() => {});
    }, [fetchTasks]);

    const handleOpenModal = (task?: Task) => {
        setSelectedTask(task || null);
        setIsModalOpen(true);
    };

    const handleDelete = async (id: number) => {
        if (window.confirm('Are you sure you want to delete this task?')) {
            try {
                await taskApi.delete(id);
                fetchTasks();
            } catch (err: any) {
                setError(err.message || 'Failed to delete task');
            }
        }
    };

    const handleModalClose = () => {
        setIsModalOpen(false);
        setSelectedTask(null);
    };

    const handleTaskSubmit = async (taskData: import('../types').CreateTaskRequest | import('../types').UpdateTaskRequest) => {
        try {
            if (selectedTask?.id)
                await taskApi.update(selectedTask.id, taskData as import('../types').UpdateTaskRequest);
            else
                await taskApi.create(taskData as import('../types').CreateTaskRequest);
            fetchTasks();
            handleModalClose();
        } catch (err: any) {
            setError(err?.response?.data?.message || err.message || 'Failed to submit task');
            throw err;
        }
    };

    const filteredTasks = useMemo(() => {
        return tasks.filter(t => {
            const matchesSearch = !searchQuery || t.title.toLowerCase().includes(searchQuery.toLowerCase());
            const matchesStatus = !statusFilter || (t.taskStatusName || '') === statusFilter;
            const matchesPriority = !priorityFilter || (t.priorityName || '') === priorityFilter;
            const matchesSkill = !skillFilter || t.requiredSkills?.some(s => s.name === skillFilter);
            return matchesSearch && matchesStatus && matchesPriority && matchesSkill;
        });
    }, [tasks, searchQuery, statusFilter, priorityFilter, skillFilter]);

    return (
        <div className="tasks-page">
            <div className="page-header">
                <div className="page-header-title">
                    <ClipboardList className="text-primary" size={28} color="var(--primary-color)" />
                    <h1>Tasks Management</h1>
                </div>
                {canManage && (
                    <button className="btn-add-primary" onClick={() => handleOpenModal()}>
                        Add New Task
                    </button>
                )}
            </div>

            <div className="filters-container">
                <div className="search-wrapper" style={{ flex: 1 }}>
                    <Search className="search-icon" size={18} />
                    <input
                        type="text"
                        className="modern-input search-input"
                        placeholder="Search by title..."
                        value={searchQuery}
                        onChange={e => setSearchQuery(e.target.value)}
                    />
                </div>
                <select className="modern-input" style={{ flex: '0 0 150px' }} value={statusFilter} onChange={e => setStatusFilter(e.target.value)}>
                    <option value="">All Statuses</option>
                    {statuses.map(s => <option key={s.id} value={s.name}>{s.name}</option>)}
                </select>
                <select className="modern-input" style={{ flex: '0 0 150px' }} value={priorityFilter} onChange={e => setPriorityFilter(e.target.value)}>
                    <option value="">All Priorities</option>
                    {priorities.map(p => <option key={p.id} value={p.name}>{p.name}</option>)}
                </select>
                <select className="modern-input" style={{ flex: '0 0 150px' }} value={skillFilter} onChange={e => setSkillFilter(e.target.value)}>
                    <option value="">All Skills</option>
                    {skills.map(s => <option key={s.id} value={s.name}>{s.name}</option>)}
                </select>
            </div>

            {error && <div className="error-message">{error}</div>}

            <div className="table-container">
                {isLoading ? (
                    <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-secondary)' }}>Loading tasks...</div>
                ) : filteredTasks.length === 0 ? (
                    <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-secondary)' }}>No tasks found matching the filters.</div>
                ) : (
                    <table className="modern-table tasks-table">
                        <thead>
                        <tr>
                            <th>ID</th>
                            <th>Title</th>
                            <th>Priority</th>
                            <th>Department</th>
                            <th>Skills</th>
                            <th>Status</th>
                            {canManage && <th>Actions</th>}
                        </tr>
                        </thead>
                        <tbody>
                        {filteredTasks.map(t => {
                            const statusName = t.taskStatusName || 'OPEN';
                            const priorityName = t.priorityName || 'NORMAL';
                            const canEditTask = statusName === 'OPEN' || statusName === 'LOCKED';

                            return (
                                <tr key={t.id}>
                                    <td>{t.id}</td>
                                    <td style={{ fontWeight: 500, color: 'var(--text-primary)' }}>{t.title}</td>
                                    <td>
                                        <span className={`priority-badge priority-${priorityName.toLowerCase()}`}>
                                            {priorityName}
                                        </span>
                                    </td>
                                    <td>
                                        <span className="department-badge">
                                            {t.departmentName || 'General'}
                                        </span>
                                    </td>
                                    <td>
                                        <div className="skills-container-table">
                                            {t.requiredSkills && t.requiredSkills.length > 0 ? (
                                                t.requiredSkills.map(s => (
                                                    <span key={s.id} className="skill-badge">{s.name}</span>
                                                ))
                                            ) : (
                                                <span style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', fontStyle: 'italic' }}>None</span>
                                            )}
                                        </div>
                                    </td>
                                    <td>
                                        <span className={`status-badge status-${statusName.toLowerCase()}`}>
                                            {statusName}
                                        </span>
                                    </td>
                                    {canManage && (
                                        <td className="actions-cell">
                                            <button
                                                className={`btn-icon edit-btn ${!canEditTask ? 'disabled-btn' : ''}`}
                                                onClick={() => canEditTask && handleOpenModal(t)}
                                                title={canEditTask ? "Edit Task" : "Cannot edit a scheduled or closed task"}
                                                disabled={!canEditTask}
                                                style={{ opacity: canEditTask ? 1 : 0.4, cursor: canEditTask ? 'pointer' : 'not-allowed' }}
                                            >
                                                <Pencil size={16} />
                                            </button>
                                            <button className="btn-icon constraint-btn" onClick={() => navigate(`/task-constraints?taskId=${t.id}`)} title="Task Constraints">
                                                <ShieldAlert size={16} />
                                            </button>
                                            <button className="btn-icon delete-btn" onClick={() => handleDelete(t.id!)} title="Delete Task">
                                                <Trash2 size={16} />
                                            </button>
                                        </td>
                                    )}
                                </tr>
                            );
                        })}
                        </tbody>
                    </table>
                )}
            </div>

            {isModalOpen && (
                <TaskModal
                    task={selectedTask}
                    departments={departments}
                    skills={skills}
                    statuses={statuses}
                    priorities={priorities}
                    onSubmit={handleTaskSubmit}
                    onClose={handleModalClose}
                />
            )}
        </div>
    );
};

export default Tasks;