import React, { useState, useEffect, useCallback, useMemo } from "react";
import type { Skill } from "../../types";
import { skillApi } from "../../api";
import { Lightbulb, Pencil, Trash2, X, Check, Search } from "lucide-react";
import { isAxiosError } from "axios";
import "./Skills.css";

interface SkillPageState {
    skills: Skill[];
    isLoading: boolean;
    error: string | null;
}

const Skills = () => {
    const [state, setState] = useState<SkillPageState>({
        skills: [],
        isLoading: false,
        error: null
    });

    const [searchQuery, setSearchQuery] = useState("");
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [newName, setNewName] = useState("");
    const [editId, setEditId] = useState<number | null>(null);
    const [editName, setEditName] = useState("");

    const fetchAll = useCallback(async () => {
        setState(prev => ({ ...prev, isLoading: true, error: null }));
        try {
            const response = await skillApi.getAll();
            setState(prev => ({
                ...prev,
                skills: response.data,
                isLoading: false
            }));
        } catch (err: unknown) {
            let message = "Failed to load skills";
            if (isAxiosError(err)) message = err.response?.data?.message || err.message;
            setState(prev => ({ ...prev, error: message, isLoading: false }));
        }
    }, []);

    useEffect(() => {
        fetchAll();
    }, [fetchAll]);

    const handleCreate = async (e: React.FormEvent) => {
        e.preventDefault();
        const trimmedName = newName.trim();
        if (!trimmedName) return;

        try {
            await skillApi.create(trimmedName);
            setNewName("");
            setIsModalOpen(false);
            await fetchAll();
        } catch (err: unknown) {
            let message = "Failed to add skill";
            if (isAxiosError(err)) message = err.response?.data?.message || err.message;
            setState(prev => ({ ...prev, error: message }));
        }
    };

    const handleUpdate = async (id: number) => {
        const trimmedName = editName.trim();
        if (!trimmedName) return;

        try {
            await skillApi.update(id, trimmedName);
            setEditId(null);
            setEditName("");
            await fetchAll();
        } catch (err: unknown) {
            setState(prev => ({ ...prev, error: "Failed to update skill" }));
        }
    };

    const handleDelete = async (id: number) => {
        if (!window.confirm("Are you sure you want to delete this skill?")) return;

        try {
            await skillApi.delete(id);
            await fetchAll();
        } catch (err: unknown) {
            setState(prev => ({ ...prev, error: "Failed to delete skill" }));
        }
    };

    const filteredSkills = useMemo(() => {
        const query = searchQuery.toLowerCase().trim();
        if (!query) return state.skills;
        return state.skills.filter(skill => skill.name.toLowerCase().includes(query));
    }, [state.skills, searchQuery]);

    return (
        <div className="skills-page">
            <div className="page-header">
                <div className="page-header-title">
                    <Lightbulb className="text-primary" size={28} />
                    <h1>Skills Management</h1>
                </div>
                <button className="btn-add-primary" onClick={() => setIsModalOpen(true)}>
                    Add Skill
                </button>
            </div>

            <div className="filters-container">
                <div className="search-wrapper">
                    <Search className="search-icon" size={18} />
                    <input
                        type="text"
                        className="modern-input search-input"
                        placeholder="Search skills..."
                        value={searchQuery}
                        onChange={e => setSearchQuery(e.target.value)}
                    />
                </div>
            </div>

            {state.error && <div className="error-banner banner-spacing">{state.error}</div>}

            <div className="table-container">
                {state.isLoading ? (
                    <div className="loading-state">Loading skills...</div>
                ) : filteredSkills.length === 0 ? (
                    <div className="empty-state">No skills found.</div>
                ) : (
                    <table className="modern-table skills-table">
                        <thead>
                            <tr>
                                <th className="th-id">ID</th>
                                <th className="th-name">Skill Name</th>
                                <th className="th-actions">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {filteredSkills.map(skill => (
                                <tr key={skill.id}>
                                    <td className="id-cell">{skill.id}</td>
                                    <td className="name-cell">
                                        {editId === skill.id ? (
                                            <input className="inline-edit-input" value={editName} onChange={e => setEditName(e.target.value)} autoFocus />
                                        ) : (
                                            <span className="static-name">{skill.name}</span>
                                        )}
                                    </td>
                                    <td className="actions-cell">
                                        <div className="actions-container">
                                            {editId === skill.id ? (
                                                <>
                                                    <button className="btn-icon approve-btn" onClick={() => handleUpdate(skill.id)}>
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
                                                            setEditId(skill.id);
                                                            setEditName(skill.name);
                                                        }}>
                                                        <Pencil size={16} />
                                                    </button>
                                                    <button className="btn-icon delete-btn" onClick={() => handleDelete(skill.id)}>
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
                                <Lightbulb size={22} className="text-primary" />
                                Add New Skill
                            </h2>
                            <button type="button" className="modern-close-btn" onClick={() => setIsModalOpen(false)}>
                                <X size={24} />
                            </button>
                        </div>
                        <form onSubmit={handleCreate} className="modern-modal-form">
                            <div className="modal-body padded-body">
                                <div className="modern-form-group">
                                    <label>Skill Name *</label>
                                    <input
                                        type="text"
                                        className="modern-input"
                                        placeholder="e.g. Electrical Repair"
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
                                    Save Skill
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Skills;
