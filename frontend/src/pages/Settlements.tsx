import { useState, useEffect, useCallback, useMemo } from "react";
import { CheckCircle, Search, Trash2, CalendarDays, Plus } from "lucide-react";
import type { Settlement, Task, User, Status } from "../types";
import { settlementApi, taskApi, userApi, statusApi } from "../api";
import SettlementModal from "../components/SettlementModal";
import { useLocation, useSearchParams } from "react-router-dom";
import { usePermissions } from "../hooks/usePermissions";
import "./Settlements.css";

function formatDate(value: string | number[] | null | undefined): string {
    if (!value) return "-";
    if (Array.isArray(value)) {
        const [y, mo, d, h = 0, m = 0] = value as number[];
        return new Date(y, mo - 1, d, h, m).toLocaleString();
    }
    const d = new Date(value as string);
    return isNaN(d.getTime()) ? String(value) : d.toLocaleString();
}

const Settlements = () => {
    const location = useLocation();
    const { canAdd, canEdit, isAdmin } = usePermissions();

    const [settlements, setSettlements] = useState<Settlement[]>([]);
    const [tasks, setTasks] = useState<Task[]>([]);
    const [users, setUsers] = useState<User[]>([]);
    const [statuses, setStatuses] = useState<Status[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [showModal, setShowModal] = useState(false);

    const [filterUserName, setFilterUserName] = useState<string>("");
    const [filterTaskName, setFilterTaskName] = useState<string>("");
    const [filterStatus, setFilterStatus] = useState<string>("");

    const [searchParams, setSearchParams] = useSearchParams();

    const settlementStatusColors: { [key: string]: string } = {
        PENDING: "#6B7280",
        ASSIGNED: "#3B82F6",
        IN_PROGRESS: "#8B5CF6",
        COMPLETED: "#10B981",
        FAILED: "#EF4444",
    };

    useEffect(() => {
        if (location.state?.filterUserName)
            setFilterUserName(location.state.filterUserName);
    }, [location.state]);

    useEffect(() => {
        const userId = searchParams.get("userId");
        if (userId && users.length > 0) {
            const user = users.find((u) => u.id === Number(userId));
            if (user) setFilterUserName(`${user.firstName} ${user.lastName}`);
        }
    }, [searchParams, users]);

    const fetchSettlements = useCallback(async () => {
        setIsLoading(true);
        setError(null);
        try {
            const res = canEdit
                ? await settlementApi.getAll()
                : await settlementApi.getMySettlements();
            setSettlements(res.data);
        } catch (err: unknown) {
            setError(
                err instanceof Error
                    ? err.message
                    : "Failed to load settlements",
            );
        } finally {
            setIsLoading(false);
        }
    }, [canEdit]);

    useEffect(() => {
        void fetchSettlements();
        taskApi
            .getAll()
            .then((res) => setTasks(res.data))
            .catch(() => {});
        userApi
            .getByRole("WORKER")
            .then((res) => setUsers(res.data))
            .catch(() => {});
        statusApi
            .getAll()
            .then((res) => setStatuses(res.data))
            .catch(() => {
                setStatuses([
                    { id: 1, name: "SCHEDULED" },
                    { id: 2, name: "IN_PROGRESS" },
                    { id: 3, name: "COMPLETED" },
                    { id: 4, name: "CANCELLED" },
                ] as Status[]);
            });
    }, [fetchSettlements]);

    function handleDelete(id: number) {
        if (
            window.confirm("Are you sure you want to delete this settlement?")
        ) {
            settlementApi
                .delete(id)
                .then(() => fetchSettlements())
                .catch((err) =>
                    setError(
                        err instanceof Error ? err.message : "Failed to delete",
                    ),
                );
        }
    }

    function handleComplete(id: number) {
        settlementApi
            .completeSettlement(id)
            .then(() => fetchSettlements())
            .catch((err) =>
                setError(
                    err instanceof Error
                        ? err.message
                        : "Failed to mark as done",
                ),
            );
    }

    const displayedSettlements = useMemo(() => {
        return settlements.filter((s) => {
            const matchesUser =
                !canEdit ||
                !filterUserName ||
                s.userName.toLowerCase().includes(filterUserName.toLowerCase());
            const matchesTask =
                !filterTaskName ||
                (s.taskTitle &&
                    s.taskTitle
                        .toLowerCase()
                        .includes(filterTaskName.toLowerCase()));
            const matchesStatus =
                !filterStatus || s.statusName === filterStatus;
            return matchesUser && matchesTask && matchesStatus;
        });
    }, [settlements, filterUserName, filterTaskName, filterStatus, canEdit]);

    return (
        <div className="settlements-page">
            <div className="page-header">
                <div className="page-header-title">
                    <CalendarDays
                        className="text-primary"
                        size={28}
                        color="var(--primary-color)"
                    />
                    <h1>Settlements Management</h1>
                </div>
                {canAdd && (
                    <button
                        className="btn-add-primary"
                        onClick={() => setShowModal(true)}
                    >
                        Add Settlement
                    </button>
                )}
            </div>

            <div className="filters-container">
                {canEdit ? (
                    <div className="search-wrapper" style={{ flex: 1 }}>
                        <Search className="search-icon" size={18} />
                        <input
                            type="text"
                            className="modern-input search-input"
                            placeholder="Search by user name..."
                            value={filterUserName}
                            onChange={(e) => {
                                setFilterUserName(e.target.value);
                                if (searchParams.has("userId")) {
                                    searchParams.delete("userId");
                                    setSearchParams(searchParams);
                                }
                            }}
                        />
                    </div>
                ) : (
                    <div
                        className="modern-input disabled-display"
                        style={{ flex: 1 }}
                    >
                        Viewing your assigned tasks
                    </div>
                )}

                <div className="search-wrapper" style={{ flex: 1 }}>
                    <Search className="search-icon" size={18} />
                    <input
                        type="text"
                        className="modern-input search-input"
                        placeholder="Search by task name..."
                        value={filterTaskName}
                        onChange={(e) => setFilterTaskName(e.target.value)}
                    />
                </div>

                <select
                    className="modern-input"
                    style={{ flex: "0 0 150px" }}
                    value={filterStatus}
                    onChange={(e) => setFilterStatus(e.target.value)}
                >
                    <option value="">All Statuses</option>
                    {statuses.map((s) => (
                        <option key={s.id} value={s.name}>
                            {s.name}
                        </option>
                    ))}
                </select>
            </div>

            {error && <div className="error-message">{error}</div>}

            <div className="table-container">
                {isLoading ? (
                    <div className="loading-state">
                        Loading settlements data...
                    </div>
                ) : displayedSettlements.length === 0 ? (
                    <div className="empty-state">
                        No settlements found matching the criteria.
                    </div>
                ) : (
                    <table className="modern-table settlements-table">
                        <thead>
                            <tr>
                                <th>User</th>
                                <th>Task</th>
                                <th>Status</th>
                                <th>Settlement Date</th>
                                <th>Completion Date</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {displayedSettlements.map((s) => {
                                const isCompleted =
                                    s.statusName === "COMPLETED";
                                return (
                                    <tr key={s.id}>
                                        <td style={{ fontWeight: 500 }}>
                                            {s.userName}
                                        </td>
                                        <td
                                            style={{
                                                color: "var(--text-primary)",
                                            }}
                                        >
                                            {s.taskTitle}
                                        </td>
                                        <td>
                                            <span
                                                className={`status-badge status-${s.statusName?.toLowerCase()}`}
                                            >
                                                {s.statusName}
                                            </span>
                                        </td>
                                        <td>{formatDate(s.settlementDate)}</td>
                                        <td>
                                            {s.completionDate ? (
                                                formatDate(s.completionDate)
                                            ) : (
                                                <span className="duration-badge">
                                                    Pending
                                                </span>
                                            )}
                                        </td>
                                        <td className="actions-cell">
                                            {!isCompleted && (
                                                <button
                                                    className="btn-icon approve-btn"
                                                    title="Mark as Done"
                                                    onClick={() =>
                                                        handleComplete(s.id)
                                                    }
                                                    style={{
                                                        width: "auto",
                                                        padding: "0 0.75rem",
                                                        gap: "0.5rem",
                                                    }}
                                                >
                                                    <CheckCircle size={16} />
                                                    <span
                                                        style={{
                                                            fontSize: "0.8rem",
                                                            fontWeight: "600",
                                                        }}
                                                    >
                                                        Done
                                                    </span>
                                                </button>
                                            )}
                                            {isAdmin && (
                                                <button
                                                    className="btn-icon action-btn delete-btn"
                                                    onClick={() =>
                                                        handleDelete(s.id)
                                                    }
                                                    title="Delete"
                                                >
                                                    <Trash2 size={16} />
                                                </button>
                                            )}
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                )}
            </div>

            {showModal && (
                <SettlementModal
                    tasks={tasks}
                    users={users}
                    settlements={settlements}
                    onSuccess={() => {
                        setShowModal(false);
                        void fetchSettlements();
                    }}
                    onClose={() => setShowModal(false)}
                />
            )}
        </div>
    );
};

export default Settlements;