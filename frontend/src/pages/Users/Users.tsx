import React, { useEffect, useState, useMemo, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { Users as UsersIcon, AlertCircle } from "lucide-react";
import { isAxiosError } from "axios";
import { userApi, departmentApi, skillApi } from "../../api";
import { usePermissions } from "../../hooks/usePermissions";
import UserModal from "../../features/users/components/UserModal/UserModal";
import UserFilters from "../../features/users/components/UserFilters";
import UserTable from "../../features/users/components/UserTable";
import type { User, Department, Skill, CreateUserRequest, UpdateUserRequest } from "../../types";
import "./Users.css";

interface UserPageState {
    users: User[];
    departments: Department[];
    skills: Skill[];
    isLoading: boolean;
    error: string | null;
}

const Users: React.FC = () => {
    const navigate = useNavigate();
    const { canEdit, canDelete } = usePermissions();

    const [state, setState] = useState<UserPageState>({
        users: [],
        departments: [],
        skills: [],
        isLoading: false,
        error: null
    });

    const [filters, setFilters] = useState({
        search: "",
        role: "",
        department: "",
        skill: ""
    });

    const [modal, setModal] = useState<{ isOpen: boolean; user: User | null }>({
        isOpen: false,
        user: null
    });

    const fetchAllData = useCallback(async () => {
        setState(prev => ({ ...prev, isLoading: true, error: null }));
        try {
            const [uRes, dRes, sRes] = await Promise.all([
                userApi.getAll(),
                departmentApi.getAll().catch(() => ({ data: [] })),
                skillApi.getAll().catch(() => ({ data: [] }))
            ]);
            setState({
                users: uRes.data,
                departments: dRes.data,
                skills: sRes.data,
                isLoading: false,
                error: null
            });
        } catch (err) {
            setState(prev => ({
                ...prev,
                isLoading: false,
                error: "Security/Network Error: Failed to load user directory"
            }));
        }
    }, []);

    useEffect(() => {
        fetchAllData();
    }, [fetchAllData]);

    const handleDelete = async (nationalId: string) => {
        if (!window.confirm("Are you sure you want to permanently delete this user?")) return;
        try {
            const userToDelete = state.users.find(u => u.nationalId === nationalId);
            if (userToDelete) {
                await userApi.delete(userToDelete.id);
                await fetchAllData();
            }
        } catch (err) {
            setState(prev => ({
                ...prev,
                error: "Integrity Error: User cannot be deleted while assigned to active tasks"
            }));
        }
    };

    const handleFormSubmit = async (userData: CreateUserRequest | UpdateUserRequest) => {
        try {
            if (modal.user) await userApi.update(modal.user.id, userData as UpdateUserRequest);
            else await userApi.create(userData as CreateUserRequest);

            setModal({ isOpen: false, user: null });
            await fetchAllData();
        } catch (err: unknown) {
            const message = isAxiosError(err) ? err.response?.data?.message : "Failed to save user";
            setState(prev => ({ ...prev, error: message }));
            throw err;
        }
    };

    const filteredUsers = useMemo(() => {
        return state.users.filter(u => {
            const searchLower = filters.search.toLowerCase();
            const matchesSearch =
                !filters.search || `${u.firstName} ${u.lastName}`.toLowerCase().includes(searchLower) || u.nationalId.includes(filters.search);
            const matchesRole = !filters.role || u.role === filters.role;
            const matchesDept = !filters.department || u.departmentName === filters.department;
            const matchesSkill = !filters.skill || u.skills?.some(s => s.name === filters.skill);

            return matchesSearch && matchesRole && matchesDept && matchesSkill;
        });
    }, [state.users, filters]);

    return (
        <div className="users-page">
            <div className="page-header">
                <div className="page-header-title">
                    <UsersIcon className="header-icon-purple" size={28} />
                    <h1>Users Management</h1>
                </div>
                {canEdit && (
                    <button className="btn-add-primary" onClick={() => setModal({ isOpen: true, user: null })}>
                        Add New User
                    </button>
                )}
            </div>

            <UserFilters
                search={filters.search}
                onSearchChange={v => setFilters({ ...filters, search: v })}
                role={filters.role}
                onRoleChange={v => setFilters({ ...filters, role: v })}
                department={filters.department}
                onDepartmentChange={v => setFilters({ ...filters, department: v })}
                skill={filters.skill}
                onSkillChange={v => setFilters({ ...filters, skill: v })}
                departments={state.departments}
                skills={state.skills}
            />

            {state.error && (
                <div className="error-banner banner-spacing flex-center">
                    <AlertCircle size={18} />
                    <span>{state.error}</span>
                </div>
            )}

            {state.isLoading ? (
                <div className="loading-state">Syncing with Secure-Schedule Database...</div>
            ) : filteredUsers.length === 0 ? (
                <div className="empty-state">No users found matching the security context.</div>
            ) : (
                <UserTable
                    users={filteredUsers}
                    canEdit={canEdit}
                    canDelete={canDelete}
                    onEdit={u => setModal({ isOpen: true, user: u })}
                    onDelete={handleDelete}
                    onNavigate={(path, st) => navigate(path, { state: st })}
                />
            )}

            {modal.isOpen && (
                <UserModal
                    user={modal.user}
                    departments={state.departments}
                    skills={state.skills}
                    onClose={() => setModal({ isOpen: false, user: null })}
                    onSubmit={handleFormSubmit}
                />
            )}
        </div>
    );
};

export default Users;
