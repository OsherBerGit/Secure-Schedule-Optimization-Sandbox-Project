import { useState, useEffect, useCallback, useMemo } from "react";
import { CalendarDays } from "lucide-react";
import type { Settlement, Task, User, Status } from "../../types";
import { settlementApi, taskApi, userApi, statusApi } from "../../api";
import SettlementModal from "../../features/settlements/components/SettlementModal/SettlementModal";
import SettlementFilters from "../../features/settlements/components/SettlementFilters";
import SettlementTable from "../../features/settlements/components/SettlementTable";
import { useLocation, useSearchParams } from "react-router-dom";
import { usePermissions } from "../../hooks/usePermissions";
import "./Settlements.css";

interface PageState {
    settlements: Settlement[];
    tasks: Task[];
    users: User[];
    statuses: Status[];
    isLoading: boolean;
    error: string | null;
}

const Settlements = () => {
    const location = useLocation();
    const { canAdd, canEdit, isAdmin } = usePermissions();
    const [searchParams] = useSearchParams();
    const [showModal, setShowModal] = useState(false);

    const [state, setState] = useState<PageState>({
        settlements: [],
        tasks: [],
        users: [],
        statuses: [],
        isLoading: false,
        error: null
    });

    const [filters, setFilters] = useState({
        userName: "",
        taskName: "",
        status: ""
    });

    const fetchMainData = useCallback(async () => {
        setState(prev => ({ ...prev, isLoading: true, error: null }));
        try {
            const [settlementsRes, tasksRes, usersRes, statusesRes] = await Promise.all([
                canEdit ? settlementApi.getAll() : settlementApi.getMySettlements(),
                taskApi.getAll(),
                userApi.getByRole("WORKER"),
                statusApi.getAll().catch(() => ({
                    data: [
                        { id: 1, name: "SCHEDULED" },
                        { id: 2, name: "IN_PROGRESS" },
                        { id: 3, name: "COMPLETED" },
                        { id: 4, name: "CANCELLED" }
                    ]
                }))
            ]);

            setState(prev => ({
                ...prev,
                settlements: settlementsRes.data,
                tasks: tasksRes.data,
                users: usersRes.data,
                statuses: statusesRes.data,
                isLoading: false
            }));
        } catch (err: unknown) {
            setState(prev => ({
                ...prev,
                isLoading: false,
                error: "Failed to load data"
            }));
        }
    }, [canEdit]);

    useEffect(() => {
        fetchMainData();
    }, [fetchMainData]);

    useEffect(() => {
        if (location.state?.filterUserName) {
            setFilters(prevFilters => ({
                ...prevFilters,
                userName: location.state.filterUserName
            }));
        }
        const qUserId = searchParams.get("userId");
        if (qUserId && state.users.length > 0) {
            const user = state.users.find(u => u.id === Number(qUserId));
            if (user)
                setFilters(prevFilters => ({
                    ...prevFilters,
                    userName: `${user.firstName} ${user.lastName}`
                }));
        }
    }, [location.state, searchParams, state.users]);

    const handleComplete = async (id: number) => {
        try {
            await settlementApi.completeSettlement(id);
            await fetchMainData();
        } catch (err: unknown) {
            setState(prev => ({ ...prev, error: "Failed to mark as done" }));
        }
    };

    const handleDelete = async (id: number) => {
        if (!window.confirm("Are you sure you want to delete this settlement?")) return;
        try {
            await settlementApi.delete(id);
            await fetchMainData();
        } catch (err: unknown) {
            setState(prev => ({ ...prev, error: "Failed to delete" }));
        }
    };

    const displayedSettlements = useMemo(() => {
        return state.settlements.filter(s => {
            const matchesUser = !canEdit || !filters.userName || s.userName.toLowerCase().includes(filters.userName.toLowerCase());
            const matchesTask = !filters.taskName || s.taskTitle?.toLowerCase().includes(filters.taskName.toLowerCase());
            const matchesStatus = !filters.status || s.statusName === filters.status;
            return matchesUser && matchesTask && matchesStatus;
        });
    }, [state.settlements, filters, canEdit]);

    return (
        <div className="settlements-page">
            <div className="page-header">
                <div className="page-header-title">
                    <CalendarDays className="text-primary" size={28} />
                    <h1>Settlements Management</h1>
                </div>
                {canAdd && (
                    <button className="btn-add-primary" onClick={() => setShowModal(true)}>
                        Add Settlement
                    </button>
                )}
            </div>

            <SettlementFilters
                canEdit={canEdit}
                filterUserName={filters.userName}
                setFilterUserName={val => setFilters({ ...filters, userName: val })}
                filterTaskName={filters.taskName}
                setFilterTaskName={val => setFilters({ ...filters, taskName: val })}
                filterStatus={filters.status}
                setFilterStatus={val => setFilters({ ...filters, status: val })}
                statuses={state.statuses}
            />

            {state.error && <div className="error-banner banner-spacing">{state.error}</div>}

            {state.isLoading ? (
                <div className="loading-state">Loading settlements data...</div>
            ) : displayedSettlements.length === 0 ? (
                <div className="empty-state">No settlements found matching the criteria.</div>
            ) : (
                <SettlementTable settlements={displayedSettlements} isAdmin={isAdmin} onComplete={handleComplete} onDelete={handleDelete} />
            )}

            {showModal && (
                <SettlementModal
                    tasks={state.tasks}
                    users={state.users}
                    settlements={state.settlements}
                    onSuccess={() => {
                        setShowModal(false);
                        fetchMainData();
                    }}
                    onClose={() => setShowModal(false)}
                />
            )}
        </div>
    );
};

export default Settlements;
