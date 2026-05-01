import { useState, useEffect, useCallback, useMemo } from "react";
import { Plane, AlertCircle } from "lucide-react";
import { useLocation, useSearchParams } from "react-router-dom";
import { isAxiosError } from "axios";
import { vacationApi, userApi } from "../../api";
import { useAuth } from "../../context/useAuth";
import { usePermissions } from "../../hooks/usePermissions";
import VacationModal from "../../features/vacations/components/VacationModal/VacationModal";
import VacationFilters from "../../features/vacations/components/VacationFilters";
import VacationTable from "../../features/vacations/components/VacationTable";
import type { Vacation, User, CreateVacationRequest, UpdateVacationRequest, VacationRequestDto } from "../../types";
import "./Vacations.css";

interface VacationPageState {
    vacations: Vacation[];
    users: User[];
    isLoading: boolean;
    error: string | null;
}

const Vacations = () => {
    const { user: currentUser } = useAuth();
    const { canEdit: canManage, isWorker } = usePermissions();
    const location = useLocation();
    const [searchParams] = useSearchParams();

    const [state, setState] = useState<VacationPageState>({
        vacations: [],
        users: [],
        isLoading: false,
        error: null
    });

    const [filters, setFilters] = useState({ user: "", department: "" });
    const [modal, setModal] = useState<{ isOpen: boolean; vacation: Vacation | null }>({
        isOpen: false,
        vacation: null
    });

    const fetchAllData = useCallback(async () => {
        setState(prev => ({ ...prev, isLoading: true, error: null }));
        try {
            const [vacRes, usersRes] = await Promise.all([vacationApi.getAll(), canManage ? userApi.getAll() : Promise.resolve({ data: [] })]);
            setState({
                vacations: vacRes.data,
                users: usersRes.data,
                isLoading: false,
                error: null
            });
        } catch (err) {
            setState(prev => ({ ...prev, isLoading: false, error: "Failed to load vacations" }));
        }
    }, [canManage]);

    useEffect(() => {
        fetchAllData();
    }, [fetchAllData]);

    useEffect(() => {
        if (location.state?.filterUserName) {
            setFilters(f => ({ ...f, user: location.state.filterUserName }));
        }
        const qUserId = searchParams.get("userId");
        if (qUserId && state.users.length > 0) {
            const user = state.users.find(u => u.id === Number(qUserId));
            if (user) setFilters(f => ({ ...f, user: `${user.firstName} ${user.lastName}` }));
        }
    }, [location.state, searchParams, state.users]);

    const handleStatusUpdate = async (id: number, status: "APPROVED" | "REJECTED") => {
        try {
            await vacationApi.updateStatus(id, { status });
            await fetchAllData();
        } catch (err) {
            setState(prev => ({ ...prev, error: `Failed to ${status.toLowerCase()} request` }));
        }
    };

    const handleDelete = async (id: number) => {
        if (!window.confirm("Are you sure you want to delete this record?")) return;
        try {
            await vacationApi.delete(id);
            await fetchAllData();
        } catch (err) {
            setState(prev => ({ ...prev, error: "Delete failed" }));
        }
    };

    const handleFormSubmit = async (formData: CreateVacationRequest | UpdateVacationRequest | VacationRequestDto) => {
        try {
            if (modal.vacation) {
                await vacationApi.update(modal.vacation.id, formData as UpdateVacationRequest);
            } else if (canManage) {
                await vacationApi.create(formData as CreateVacationRequest);
            } else {
                const requestDto: VacationRequestDto = {
                    startDate: new Date(formData.startDate).toISOString().split("T")[0],
                    endDate: new Date(formData.endDate).toISOString().split("T")[0]
                };
                await vacationApi.request(requestDto);
            }
            setModal({ isOpen: false, vacation: null });
            await fetchAllData();
        } catch (err: unknown) {
            const msg = isAxiosError(err) ? err.response?.data?.message : "Submit failed";
            setState(prev => ({ ...prev, error: msg }));
        }
    };

    const availableDepartments = useMemo(() => {
        const depts = state.vacations.map(v => state.users.find(u => u.id === v.userId)?.departmentName).filter(Boolean);
        return Array.from(new Set(depts)) as string[];
    }, [state.vacations, state.users]);

    const displayedVacations = useMemo(() => {
        let list = isWorker ? state.vacations.filter(v => v.userId === currentUser?.id) : state.vacations;

        list = list.filter(v => {
            const userDept = state.users.find(u => u.id === v.userId)?.departmentName || "";
            const matchesDept = !filters.department || userDept === filters.department;
            const matchesUser = !filters.user || v.userName.toLowerCase().includes(filters.user.toLowerCase());
            return matchesDept && matchesUser;
        });

        return list.sort((a, b) => new Date(b.startDate).getTime() - new Date(a.startDate).getTime());
    }, [state.vacations, state.users, filters, isWorker, currentUser?.id]);

    return (
        <div className="vacations-page">
            <div className="page-header">
                <div className="page-header-title">
                    <Plane className="text-primary" size={28} />
                    <h1>Vacations Management</h1>
                </div>
                <button className="btn-add-primary" onClick={() => setModal({ isOpen: true, vacation: null })}>
                    {canManage ? "Add Vacation" : "Request Vacation"}
                </button>
            </div>

            <VacationFilters
                canManage={canManage}
                filterUser={filters.user}
                onUserChange={v => setFilters(f => ({ ...f, user: v }))}
                filterDept={filters.department}
                onDeptChange={v => setFilters(f => ({ ...f, department: v }))}
                departments={availableDepartments}
            />

            {state.error && (
                <div className="error-banner banner-spacing flex-center">
                    <AlertCircle size={18} />
                    <span>{state.error}</span>
                </div>
            )}

            {state.isLoading ? (
                <div className="loading-state">Syncing with Secure-Schedule Database...</div>
            ) : displayedVacations.length === 0 ? (
                <div className="empty-state">No vacation records found.</div>
            ) : (
                <VacationTable
                    vacations={displayedVacations}
                    canManage={canManage}
                    onApprove={id => handleStatusUpdate(id, "APPROVED")}
                    onReject={id => handleStatusUpdate(id, "REJECTED")}
                    onEdit={v => setModal({ isOpen: true, vacation: v })}
                    onDelete={handleDelete}
                />
            )}

            {modal.isOpen && (
                <VacationModal
                    vacation={modal.vacation}
                    isAdmin={!!canManage}
                    users={state.users}
                    onSubmit={handleFormSubmit}
                    onClose={() => setModal({ isOpen: false, vacation: null })}
                />
            )}
        </div>
    );
};

export default Vacations;
