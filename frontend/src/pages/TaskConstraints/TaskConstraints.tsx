import { useState, useEffect, useCallback, useMemo } from "react";
import { GitMerge, List, Network, AlertCircle } from "lucide-react";
import { isAxiosError } from "axios";
import { taskConstraintApi, constraintTypeApi, taskApi, departmentApi } from "../../api";
import { usePermissions } from "../../hooks/usePermissions";
import TaskConstraintModal from "../../features/tasks/components/TaskConstraintModal";
import TaskGraph from "../../features/tasks/components/TaskGraph/TaskGraph";
import TaskConstraintFilters from "../../features/tasks/components/TaskConstraintFilters";
import TaskConstraintTable from "../../features/tasks/components/TaskConstraintTable";
import type { TaskConstraint, ConstraintType, Task, Department } from "../../types";
import "./TaskConstraints.css";

interface ConstraintsPageState {
    constraints: TaskConstraint[];
    tasks: Task[];
    types: ConstraintType[];
    departments: Department[];
    isLoading: boolean;
    error: string | null;
}

const TaskConstraints = () => {
    const { canEdit: canManage } = usePermissions();
    const [state, setState] = useState<ConstraintsPageState>({
        constraints: [],
        tasks: [],
        types: [],
        departments: [],
        isLoading: false,
        error: null
    });

    const [viewMode, setViewMode] = useState<"table" | "graph">("table");
    const [showModal, setShowModal] = useState(false);
    const [filters, setFilters] = useState({
        search: "",
        department: "",
        type: ""
    });

    const fetchAll = useCallback(async () => {
        setState(prev => ({ ...prev, isLoading: true, error: null }));
        try {
            const [cRes, tRes, ctRes, dRes] = await Promise.all([
                taskConstraintApi.getAll(),
                taskApi.getAll(),
                constraintTypeApi.getAll(),
                departmentApi.getAll()
            ]);
            setState({
                constraints: cRes.data,
                tasks: tRes.data,
                types: ctRes.data,
                departments: dRes.data,
                isLoading: false,
                error: null
            });
        } catch {
            setState(prev => ({
                ...prev,
                isLoading: false,
                error: "Failed to load task constraints"
            }));
        }
    }, []);

    useEffect(() => {
        fetchAll();
    }, [fetchAll]);

    const handleCreate = async (data: { predecessorTaskId: number; successorTaskId: number; constraintTypeId: number; lagMinutes?: number }) => {
        if (data.predecessorTaskId === data.successorTaskId) {
            setState(prev => ({ ...prev, error: "Predecessor and successor tasks cannot be the same" }));
            return;
        }
        try {
            await taskConstraintApi.create(data);
            setShowModal(false);
            await fetchAll();
        } catch (err: unknown) {
            const message = isAxiosError(err) ? err.response?.data?.message : "Failed to create constraint";
            setState(prev => ({ ...prev, error: message }));
        }
    };

    const handleDelete = async (id: number) => {
        if (!window.confirm("Are you sure you want to delete this task constraint?")) return;
        try {
            await taskConstraintApi.delete(id);
            await fetchAll();
        } catch {
            setState(prev => ({ ...prev, error: "Failed to delete task constraint" }));
        }
    };

    const filteredConstraints = useMemo(() => {
        return state.constraints.filter(c => {
            const searchLower = filters.search.toLowerCase();
            const matchesSearch =
                !filters.search ||
                (c.predecessorTaskTitle || "").toLowerCase().includes(searchLower) ||
                (c.successorTaskTitle || "").toLowerCase().includes(searchLower) ||
                (c.constraintTypeName || "").toLowerCase().includes(searchLower);

            const matchesDept =
                !filters.department ||
                (() => {
                    const p = state.tasks.find(t => t.id === c.predecessorTaskId);
                    const s = state.tasks.find(t => t.id === c.successorTaskId);
                    return p?.departmentName === filters.department || s?.departmentName === filters.department;
                })();

            const matchesType = !filters.type || c.constraintTypeName === filters.type;

            return matchesSearch && matchesDept && matchesType;
        });
    }, [state.constraints, state.tasks, filters]);

    const tasksForGraph = useMemo(() => {
        if (!filters.search && !filters.department && !filters.type) return state.tasks;
        const relevantIds = new Set<number>();
        filteredConstraints.forEach(c => {
            relevantIds.add(c.predecessorTaskId);
            relevantIds.add(c.successorTaskId);
        });
        return state.tasks.filter(t => relevantIds.has(t.id));
    }, [state.tasks, filteredConstraints, filters]);

    return (
        <div className="task-constraints-page">
            <div className="page-header">
                <div className="page-header-title">
                    <GitMerge className="text-primary" size={28} />
                    <h1>Task Constraints</h1>
                </div>
                <div className="header-actions">
                    <div className="view-toggle">
                        <button className={`toggle-btn ${viewMode === "table" ? "active" : ""}`} onClick={() => setViewMode("table")}>
                            <List size={16} /> Table
                        </button>
                        <button className={`toggle-btn ${viewMode === "graph" ? "active" : ""}`} onClick={() => setViewMode("graph")}>
                            <Network size={16} /> Graph
                        </button>
                    </div>
                    {canManage && (
                        <button className="btn-add-primary" onClick={() => setShowModal(true)}>
                            Add Constraint
                        </button>
                    )}
                </div>
            </div>

            <TaskConstraintFilters
                search={filters.search}
                onSearchChange={val => setFilters(f => ({ ...f, search: val }))}
                filterDepartment={filters.department}
                onDepartmentChange={val => setFilters(f => ({ ...f, department: val }))}
                filterType={filters.type}
                onTypeChange={val => setFilters(f => ({ ...f, type: val }))}
                departments={state.departments}
                constraintTypes={state.types}
                canManage={canManage}
            />

            {state.error && (
                <div className="error-banner banner-spacing">
                    <AlertCircle size={20} />
                    <span>{state.error}</span>
                </div>
            )}

            {state.isLoading ? (
                <div className="loading-state">Loading constraints...</div>
            ) : viewMode === "table" ? (
                filteredConstraints.length === 0 ? (
                    <div className="empty-state">No constraints found matching the criteria.</div>
                ) : (
                    <TaskConstraintTable constraints={filteredConstraints} canManage={canManage} onDelete={handleDelete} />
                )
            ) : (
                <TaskGraph tasks={tasksForGraph} constraints={filteredConstraints} />
            )}

            {showModal && canManage && (
                <TaskConstraintModal tasks={state.tasks} constraintTypes={state.types} onSubmit={handleCreate} onClose={() => setShowModal(false)} />
            )}
        </div>
    );
};

export default TaskConstraints;
