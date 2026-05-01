import React, { useState, useEffect, useCallback, useMemo } from "react";
import type { Department } from "../../types";
import { departmentApi } from "../../api";
import { Building2, Pencil, Trash2, X, Check, Search } from "lucide-react";
import { isAxiosError } from "axios";
import "./Departments.css";

interface DepartmentState {
    items: Department[];
    isLoading: boolean;
    error: string | null;
}

const Departments = () => {
    const [state, setState] = useState<DepartmentState>({
        items: [],
        isLoading: false,
        error: null
    });

    const [searchQuery, setSearchQuery] = useState("");
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [newName, setNewName] = useState("");

    const [editId, setEditId] = useState<number | null>(null);
    const [editName, setEditName] = useState("");

    const fetchDepartments = useCallback(async () => {
        setState(prev => ({ ...prev, isLoading: true, error: null }));
        try {
            const response = await departmentApi.getAll();
            setState(prev => ({
                ...prev,
                items: response.data,
                isLoading: false
            }));
        } catch {
            setState(prev => ({
                ...prev,
                error: "Failed to load departments",
                isLoading: false
            }));
        }
    }, []);

    useEffect(() => {
        fetchDepartments();
    }, [fetchDepartments]);

    const handleAddDepartment = async (e: React.FormEvent) => {
        e.preventDefault();
        const trimmedName = newName.trim();
        if (!trimmedName) return;

        try {
            await departmentApi.create(trimmedName);
            setNewName("");
            setIsModalOpen(false);
            await fetchDepartments();
        } catch (err: unknown) {
            let message = "Failed to add department";
            if (isAxiosError(err)) {
                message = err.response?.data?.message || err.message;
            }
            setState(prev => ({ ...prev, error: message }));
        }
    };

    const handleUpdate = async (id: number) => {
        const trimmedName = editName.trim();
        if (!trimmedName) return;

        try {
            await departmentApi.update(id, trimmedName);
            setEditId(null);
            setEditName("");
            await fetchDepartments();
        } catch {
            setState(prev => ({
                ...prev,
                error: "Failed to update department"
            }));
        }
    };

    const handleDelete = async (id: number) => {
        if (!window.confirm("Are you sure you want to delete this department?")) return;

        try {
            await departmentApi.delete(id);
            await fetchDepartments();
        } catch {
            setState(prev => ({
                ...prev,
                error: "Failed to delete department"
            }));
        }
    };

    const filteredDepartments = useMemo(() => {
        const query = searchQuery.toLowerCase().trim();
        if (!query) return state.items;
        return state.items.filter(dept => dept.name.toLowerCase().includes(query));
    }, [state.items, searchQuery]);

    return (
        <div className="departments-page">
            <div className="page-header">
                <div className="page-header-title">
                    <Building2 className="text-primary" size={28} />
                    <h1>Departments Management</h1>
                </div>
                <button className="btn-add-primary" onClick={() => setIsModalOpen(true)}>
                    Add Department
                </button>
            </div>

            <div className="filters-container">
                <div className="search-wrapper">
                    <Search className="search-icon" size={18} />
                    <input
                        type="text"
                        className="modern-input search-input"
                        placeholder="Search departments..."
                        value={searchQuery}
                        onChange={e => setSearchQuery(e.target.value)}
                    />
                </div>
            </div>

            {state.error && <div className="error-banner banner-spacing">{state.error}</div>}

            <div className="table-container">
                {state.isLoading ? (
                    <div className="loading-state">Loading departments...</div>
                ) : filteredDepartments.length === 0 ? (
                    <div className="empty-state">No departments found.</div>
                ) : (
                    <table className="modern-table departments-table">
                        <thead>
                            <tr>
                                <th className="th-id">ID</th>
                                <th className="th-name">Department Name</th>
                                <th className="th-actions">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {filteredDepartments.map(dept => (
                                <tr key={dept.id}>
                                    <td className="id-cell">{dept.id}</td>
                                    <td className="name-cell">
                                        {editId === dept.id ? (
                                            <input className="inline-edit-input" value={editName} onChange={e => setEditName(e.target.value)} autoFocus />
                                        ) : (
                                            <span className="static-name">{dept.name}</span>
                                        )}
                                    </td>
                                    <td className="actions-cell">
                                        <div className="actions-container">
                                            {editId === dept.id ? (
                                                <>
                                                    <button className="btn-icon approve-btn" onClick={() => handleUpdate(dept.id)}>
                                                        <Check size={18} />
                                                    </button>
                                                    <button className="btn-icon delete-btn" onClick={() => setEditId(null)}>
                                                        <X size={18} />
                                                    </button>
                                                </>
                                            ) : (
                                                <>
                                                    <button
                                                        className="btn-icon edit-btn"
                                                        onClick={() => {
                                                            setEditId(dept.id);
                                                            setEditName(dept.name);
                                                        }}>
                                                        <Pencil size={16} />
                                                    </button>
                                                    <button className="btn-icon delete-btn" onClick={() => handleDelete(dept.id)}>
                                                        <Trash2 size={16} />
                                                    </button>
                                                </>
                                            )}
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </div>

            {isModalOpen && (
                <div className="modal-overlay">
                    <div className="modern-modal-card responsive-modal">
                        <div className="modal-header">
                            <h2>
                                <Building2 size={22} className="text-primary" />
                                Add New Department
                            </h2>
                            <button type="button" className="modern-close-btn" onClick={() => setIsModalOpen(false)}>
                                <X size={24} />
                            </button>
                        </div>
                        <form onSubmit={handleAddDepartment} className="modern-modal-form">
                            <div className="modal-body padded-body">
                                <div className="modern-form-group">
                                    <label>Department Name *</label>
                                    <input
                                        type="text"
                                        className="modern-input"
                                        placeholder="e.g. Engineering"
                                        value={newName}
                                        onChange={e => setNewName(e.target.value)}
                                        required
                                        autoFocus
                                    />
                                </div>
                            </div>
                            <div className="modal-actions modal-actions-footer">
                                <button type="button" className="btn-cancel" onClick={() => setIsModalOpen(false)}>
                                    Cancel
                                </button>
                                <button type="submit" className="btn-submit">
                                    Save Department
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Departments;
