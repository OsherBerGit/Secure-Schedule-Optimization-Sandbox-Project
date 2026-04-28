import { useState, useEffect, useCallback, useMemo } from "react";
import type {
    TaskConstraint,
    ConstraintType,
    Task,
    Department,
} from "../types";
import {
    taskConstraintApi,
    constraintTypeApi,
    taskApi,
    departmentApi,
} from "../api";
import { usePermissions } from "../hooks/usePermissions";
import TaskConstraintModal from "../components/TaskConstraintModal";
import TaskGraph from "../components/TaskGraph";
import { GitMerge, Search, Trash2, List, Network } from "lucide-react";
import "./TaskConstraints.css";

type ViewMode = "table" | "graph";

const TaskConstraints = () => {
    const { canEdit: canManage } = usePermissions();
    const [constraints, setConstraints] = useState<TaskConstraint[]>([]);
    const [tasks, setTasks] = useState<Task[]>([]);
    const [constraintTypes, setConstraintTypes] = useState<ConstraintType[]>(
        [],
    );
    const [departments, setDepartments] = useState<Department[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [showModal, setShowModal] = useState(false);
    const [viewMode, setViewMode] = useState<ViewMode>("table");

    const [search, setSearch] = useState("");
    const [filterDepartment, setFilterDepartment] = useState<string>("");
    const [filterConstraintType, setFilterConstraintType] =
        useState<string>("");

    const fetchAll = useCallback(async () => {
        setIsLoading(true);
        try {
            const [cRes, tRes, ctRes, dRes] = await Promise.all([
                taskConstraintApi.getAll(),
                taskApi.getAll(),
                constraintTypeApi.getAll(),
                departmentApi.getAll(),
            ]);
            setConstraints(cRes.data);
            setTasks(tRes.data);
            setConstraintTypes(ctRes.data);
            setDepartments(dRes.data);
        } catch {
            setError("Failed to load task constraints");
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => {
        void fetchAll();
    }, [fetchAll]);

    async function handleCreate(data: {
        predecessorTaskId: number;
        successorTaskId: number;
        constraintTypeId: number;
        lagMinutes?: number;
    }) {
        if (data.predecessorTaskId === data.successorTaskId)
            return setError(
                "Predecessor and successor tasks cannot be the same",
            );
        try {
            await taskConstraintApi.create(data);
            setShowModal(false);
            await fetchAll();
        } catch {
            setError("Failed to create task constraint");
        }
    }

    async function handleDelete(id: number) {
        if (
            !window.confirm(
                "Are you sure you want to delete this task constraint?",
            )
        )
            return;
        try {
            await taskConstraintApi.delete(id);
            await fetchAll();
        } catch {
            setError("Failed to delete task constraint");
        }
    }

    const filteredConstraints = useMemo(() => {
        return constraints.filter((c) => {
            const searchLower = search.toLowerCase();
            const matchesSearch =
                search === "" ||
                (c.predecessorTaskTitle || "")
                    .toLowerCase()
                    .includes(searchLower) ||
                (c.successorTaskTitle || "")
                    .toLowerCase()
                    .includes(searchLower) ||
                (c.constraintTypeName || "")
                    .toLowerCase()
                    .includes(searchLower);
            const matchesDept =
                filterDepartment === "" ||
                (() => {
                    const p = tasks.find((t) => t.id === c.predecessorTaskId),
                        s = tasks.find((t) => t.id === c.successorTaskId);
                    return (
                        p?.departmentName === filterDepartment ||
                        s?.departmentName === filterDepartment
                    );
                })();
            const matchesType =
                filterConstraintType === "" ||
                c.constraintTypeName === filterConstraintType;
            return matchesSearch && matchesDept && matchesType;
        });
    }, [constraints, tasks, search, filterDepartment, filterConstraintType]);

    const tasksForGraph = useMemo(() => {
        if (!search && !filterDepartment && !filterConstraintType) return tasks;
        const relevant = new Set<number>();
        filteredConstraints.forEach((c) => {
            relevant.add(c.predecessorTaskId);
            relevant.add(c.successorTaskId);
        });
        return tasks.filter((t) => relevant.has(t.id));
    }, [
        tasks,
        filteredConstraints,
        search,
        filterDepartment,
        filterConstraintType,
    ]);

    return (
        <div className="task-constraints-page">
            <div className="page-header">
                <div className="page-header-title">
                    <GitMerge
                        className="text-primary"
                        size={28}
                        color="var(--primary-color)"
                    />
                    <h1>Task Constraints</h1>
                </div>
                <div className="header-actions">
                    <div className="view-toggle">
                        <button
                            className={`toggle-btn ${viewMode === "table" ? "active" : ""}`}
                            onClick={() => setViewMode("table")}
                        >
                            <List size={16} /> Table
                        </button>
                        <button
                            className={`toggle-btn ${viewMode === "graph" ? "active" : ""}`}
                            onClick={() => setViewMode("graph")}
                        >
                            <Network size={16} /> Graph
                        </button>
                    </div>
                    {canManage && (
                        <button
                            className="btn-add-primary"
                            onClick={() => setShowModal(true)}
                        >
                            Add Constraint
                        </button>
                    )}
                </div>
            </div>
            <div className="filters-container">
                <div className="search-wrapper" style={{ flex: 1 }}>
                    <Search className="search-icon" size={18} />
                    <input
                        type="text"
                        className="modern-input search-input"
                        placeholder="Search tasks or type..."
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                    />
                </div>
                {canManage && (
                    <select
                        className="modern-input"
                        style={{ flex: "0 0 180px" }}
                        value={filterDepartment}
                        onChange={(e) => setFilterDepartment(e.target.value)}
                    >
                        <option value="">All Departments</option>
                        {departments.map((d) => (
                            <option key={d.id} value={d.name}>
                                {d.name}
                            </option>
                        ))}
                    </select>
                )}
                <select
                    className="modern-input"
                    style={{ flex: "0 0 180px" }}
                    value={filterConstraintType}
                    onChange={(e) => setFilterConstraintType(e.target.value)}
                >
                    <option value="">All Types</option>
                    {constraintTypes.map((ct) => (
                        <option key={ct.id} value={ct.name}>
                            {ct.name}
                        </option>
                    ))}
                </select>
            </div>
            {error && <div className="error-message">{error}</div>}
            {viewMode === "table" ? (
                <div className="table-container">
                    {isLoading ? (
                        <div className="loading-state">
                            Loading constraints...
                        </div>
                    ) : filteredConstraints.length === 0 ? (
                        <div className="empty-state">
                            No constraints found matching the criteria.
                        </div>
                    ) : (
                        <table className="modern-table constraints-table">
                            <thead>
                                <tr>
                                    <th className="th-id">ID</th>
                                    <th>Predecessor Task</th>
                                    <th>Successor Task</th>
                                    <th>Type</th>
                                    <th>Lag (min)</th>
                                    {canManage && (
                                        <th className="th-actions">Actions</th>
                                    )}
                                </tr>
                            </thead>
                            <tbody>
                                {filteredConstraints.map((c) => (
                                    <tr key={c.id}>
                                        <td className="id-cell">{c.id}</td>
                                        <td className="task-title-cell">
                                            {c.predecessorTaskTitle ??
                                                `#${c.predecessorTaskId}`}
                                        </td>
                                        <td className="task-title-cell">
                                            {c.successorTaskTitle ??
                                                `#${c.successorTaskId}`}
                                        </td>
                                        <td>
                                            <span className="constraint-badge">
                                                {c.constraintTypeName ??
                                                    `#${c.constraintTypeId}`}
                                            </span>
                                        </td>
                                        <td>
                                            {c.lagMinutes ? (
                                                <span className="lag-badge">
                                                    {c.lagMinutes}m
                                                </span>
                                            ) : (
                                                <span className="lag-empty">
                                                    -
                                                </span>
                                            )}
                                        </td>
                                        {canManage && (
                                            <td className="actions-cell">
                                                <div className="actions-container">
                                                    <button
                                                        className="btn-icon delete-btn"
                                                        onClick={() =>
                                                            handleDelete(c.id)
                                                        }
                                                        title="Delete"
                                                    >
                                                        <Trash2 size={16} />
                                                    </button>
                                                </div>
                                            </td>
                                        )}
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                </div>
            ) : (
                <TaskGraph
                    tasks={tasksForGraph}
                    constraints={filteredConstraints}
                />
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
    );
};

export default TaskConstraints;