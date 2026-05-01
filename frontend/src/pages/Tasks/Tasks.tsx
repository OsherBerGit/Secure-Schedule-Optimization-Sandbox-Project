import React, { useEffect, useState, useMemo, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { ClipboardList, AlertCircle } from "lucide-react";
import { isAxiosError } from "axios";
import { taskApi, departmentApi, skillApi, statusApi, priorityApi } from "../../api";
import type { Task, Department, Skill, Status, Priority, CreateTaskRequest, UpdateTaskRequest } from "../../types";
import { usePermissions } from "../../hooks/usePermissions";
import TaskModal from "../../features/tasks/components/TaskModal/TaskModal";
import TaskFilters from "../../features/tasks/components/TaskFilters";
import TaskTable from "../../features/tasks/components/TaskTable";
import "./Tasks.css";

interface TasksPageState {
    tasks: Task[];
    departments: Department[];
    skills: Skill[];
    statuses: Status[];
    priorities: Priority[];
    isLoading: boolean;
    error: string | null;
}

const Tasks: React.FC = () => {
    const { canEdit: canManage } = usePermissions();
    const navigate = useNavigate();

    const [state, setState] = useState<TasksPageState>({
        tasks: [],
        departments: [],
        skills: [],
        statuses: [],
        priorities: [],
        isLoading: false,
        error: null
    });

    const [filters, setFilters] = useState({
        search: "",
        status: "",
        priority: "",
        skill: ""
    });

    const [modal, setModal] = useState<{ isOpen: boolean; task: Task | null }>({
        isOpen: false,
        task: null
    });

    const fetchAllData = useCallback(async () => {
        setState(prev => ({ ...prev, isLoading: true, error: null }));
        try {
            const [tRes, dRes, sRes, stRes, pRes] = await Promise.all([
                taskApi.getAll(),
                departmentApi.getAll().catch(() => ({ data: [] })),
                skillApi.getAll().catch(() => ({ data: [] })),
                statusApi.getAll().catch(() => ({
                    data: [
                        { id: 1, name: "OPEN" },
                        { id: 2, name: "LOCKED" },
                        { id: 3, name: "SCHEDULED" },
                        { id: 4, name: "CLOSED" }
                    ]
                })),
                priorityApi.getAll().catch(() => ({
                    data: [
                        { id: 1, name: "LOW" },
                        { id: 2, name: "NORMAL" },
                        { id: 3, name: "HIGH" },
                        { id: 4, name: "URGENT" }
                    ]
                }))
            ]);

            setState({
                tasks: tRes.data,
                departments: dRes.data,
                skills: sRes.data,
                statuses: stRes.data,
                priorities: pRes.data,
                isLoading: false,
                error: null
            });
        } catch (err) {
            setState(prev => ({
                ...prev,
                isLoading: false,
                error: "Failed to load system data"
            }));
        }
    }, []);

    useEffect(() => {
        fetchAllData();
    }, [fetchAllData]);

    const handleTaskSubmit = async (taskData: CreateTaskRequest | UpdateTaskRequest) => {
        try {
            if (modal.task?.id) {
                await taskApi.update(modal.task.id, taskData as UpdateTaskRequest);
            } else {
                await taskApi.create(taskData as CreateTaskRequest);
            }
            setModal({ isOpen: false, task: null });
            fetchAllData();
        } catch (err: unknown) {
            const message = isAxiosError(err) ? err.response?.data?.message : "Submission failed";
            setState(prev => ({ ...prev, error: message }));
            throw err;
        }
    };

    const handleDelete = async (id: number) => {
        if (!window.confirm("Are you sure you want to delete this task?")) return;
        try {
            await taskApi.delete(id);
            fetchAllData();
        } catch (err) {
            setState(prev => ({ ...prev, error: "Delete failed" }));
        }
    };

    const filteredTasks = useMemo(() => {
        return state.tasks.filter(t => {
            const matchesSearch = !filters.search || t.title.toLowerCase().includes(filters.search.toLowerCase());
            const matchesStatus = !filters.status || t.taskStatusName === filters.status;
            const matchesPriority = !filters.priority || t.priorityName === filters.priority;
            const matchesSkill = !filters.skill || t.requiredSkills?.some(s => s.name === filters.skill);
            return matchesSearch && matchesStatus && matchesPriority && matchesSkill;
        });
    }, [state.tasks, filters]);

    return (
        <div className="tasks-page">
            <div className="page-header">
                <div className="page-header-title">
                    <ClipboardList className="text-primary" size={28} />
                    <h1>Tasks Management</h1>
                </div>
                {canManage && (
                    <button className="btn-add-primary" onClick={() => setModal({ isOpen: true, task: null })}>
                        Add New Task
                    </button>
                )}
            </div>

            <TaskFilters
                search={filters.search}
                onSearchChange={v => setFilters({ ...filters, search: v })}
                status={filters.status}
                onStatusChange={v => setFilters({ ...filters, status: v })}
                priority={filters.priority}
                onPriorityChange={v => setFilters({ ...filters, priority: v })}
                skill={filters.skill}
                onSkillChange={v => setFilters({ ...filters, skill: v })}
                metadata={{
                    statuses: state.statuses,
                    priorities: state.priorities,
                    skills: state.skills
                }}
            />

            {state.error && (
                <div className="error-banner banner-spacing flex-center">
                    <AlertCircle size={18} />
                    <span>{state.error}</span>
                </div>
            )}

            {state.isLoading ? (
                <div className="loading-state">Loading tasks...</div>
            ) : filteredTasks.length === 0 ? (
                <div className="empty-state">No tasks found matching the criteria.</div>
            ) : (
                <TaskTable
                    tasks={filteredTasks}
                    canManage={canManage}
                    onEdit={t => setModal({ isOpen: true, task: t })}
                    onDelete={handleDelete}
                    onViewConstraints={id => navigate(`/task-constraints?taskId=${id}`)}
                />
            )}

            {modal.isOpen && (
                <TaskModal
                    task={modal.task}
                    departments={state.departments}
                    skills={state.skills}
                    statuses={state.statuses}
                    priorities={state.priorities}
                    onSubmit={handleTaskSubmit}
                    onClose={() => setModal({ isOpen: false, task: null })}
                />
            )}
        </div>
    );
};

export default Tasks;
