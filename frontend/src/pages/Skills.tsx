import { useState, useEffect, useCallback, useMemo } from "react";
import type { Skill } from "../types";
import { skillApi } from "../api";
import { Lightbulb, Pencil, Trash2, X, Check, Search } from "lucide-react";
import "./Skills.css";

const Skills = () => {
    const [items, setItems] = useState<Skill[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [newName, setNewName] = useState("");
    const [editId, setEditId] = useState<number | null>(null);
    const [editName, setEditName] = useState("");
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [searchQuery, setSearchQuery] = useState("");

    const fetchAll = useCallback(async () => {
        setIsLoading(true);
        setError(null);
        try {
            const res = await skillApi.getAll();
            setItems(res.data);
        } catch (err: any) {
            setError(
                err?.response?.data?.message ||
                    err.message ||
                    "Failed to load skills",
            );
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => {
        void fetchAll();
    }, [fetchAll]);

    async function handleCreate(e: React.FormEvent) {
        e.preventDefault();
        if (!newName.trim()) {
            setError("Name is required!");
            return;
        }
        try {
            await skillApi.create(newName.trim());
            setNewName("");
            setIsModalOpen(false);
            setError(null);
            await fetchAll();
        } catch (err: any) {
            setError(
                "Failed to add: " +
                    (err.response?.data?.message || err.message),
            );
        }
    }

    async function handleUpdate(id: number) {
        if (!editName.trim()) return;
        try {
            await skillApi.update(id, editName.trim());
            setEditId(null);
            await fetchAll();
        } catch (err: any) {
            setError(
                err?.response?.data?.message ||
                    err.message ||
                    "Failed to update skill",
            );
        }
    }

    async function handleDelete(id: number) {
        if (!window.confirm("Are you sure you want to delete this skill?"))
            return;
        try {
            await skillApi.delete(id);
            await fetchAll();
        } catch (err: any) {
            setError(
                err?.response?.data?.message ||
                    err.message ||
                    "Failed to delete skill",
            );
        }
    }

    const filteredItems = useMemo(() => {
        if (!searchQuery.trim()) return items;
        return items.filter((s) =>
            s.name.toLowerCase().includes(searchQuery.toLowerCase()),
        );
    }, [items, searchQuery]);

    return (
        <div className="skills-page">
            <div className="page-header">
                <div className="page-header-title">
                    <Lightbulb
                        className="text-primary"
                        size={28}
                        color="var(--primary-color)"
                    />
                    <h1>Skills Management</h1>
                </div>
                <button
                    className="btn-add-primary"
                    onClick={() => setIsModalOpen(true)}
                >
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
                        onChange={(e) => setSearchQuery(e.target.value)}
                    />
                </div>
            </div>

            {error && <div className="error-message">{error}</div>}

            <div className="table-container">
                {isLoading ? (
                    <div className="loading-state">Loading skills...</div>
                ) : filteredItems.length === 0 ? (
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
                            {filteredItems.map((item) => (
                                <tr key={item.id}>
                                    <td className="id-cell">{item.id}</td>
                                    <td className="name-cell">
                                        {editId === item.id ? (
                                            <input
                                                className="inline-edit-input"
                                                value={editName}
                                                onChange={(e) =>
                                                    setEditName(e.target.value)
                                                }
                                                autoFocus
                                            />
                                        ) : (
                                            <span className="static-name">
                                                {item.name}
                                            </span>
                                        )}
                                    </td>
                                    <td className="actions-cell">
                                        <div className="actions-container">
                                            {editId === item.id ? (
                                                <>
                                                    <button
                                                        className="btn-icon approve-btn"
                                                        onClick={() =>
                                                            handleUpdate(
                                                                item.id,
                                                            )
                                                        }
                                                    >
                                                        <Check size={18} />
                                                    </button>
                                                    <button
                                                        className="btn-icon delete-btn"
                                                        onClick={() =>
                                                            setEditId(null)
                                                        }
                                                    >
                                                        <X size={18} />
                                                    </button>
                                                </>
                                            ) : (
                                                <>
                                                    <button
                                                        className="btn-icon edit-btn"
                                                        onClick={() => {
                                                            setEditId(item.id);
                                                            setEditName(
                                                                item.name,
                                                            );
                                                        }}
                                                    >
                                                        <Pencil size={16} />
                                                    </button>
                                                    <button
                                                        className="btn-icon delete-btn"
                                                        onClick={() =>
                                                            handleDelete(
                                                                item.id,
                                                            )
                                                        }
                                                    >
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
                    <div
                        className="modern-modal-card"
                        style={{ maxWidth: "450px", width: "90%" }}
                    >
                        <div className="modal-header">
                            <h2>
                                <Lightbulb size={22} className="text-primary" />{" "}
                                Add New Skill
                            </h2>
                            <button
                                type="button"
                                className="modern-close-btn"
                                onClick={() => setIsModalOpen(false)}
                            >
                                <X size={24} />
                            </button>
                        </div>
                        <form
                            onSubmit={handleCreate}
                            className="modern-modal-form"
                        >
                            <div
                                className="modal-body"
                                style={{ padding: "2rem" }}
                            >
                                <div className="modern-form-group">
                                    <label>Skill Name *</label>
                                    <input
                                        type="text"
                                        className="modern-input"
                                        placeholder="e.g. Electrical Repair"
                                        value={newName}
                                        onChange={(e) =>
                                            setNewName(e.target.value)
                                        }
                                        required
                                        autoFocus
                                    />
                                </div>
                            </div>
                            <div className="modal-actions">
                                <button
                                    type="button"
                                    className="btn-cancel"
                                    onClick={() => setIsModalOpen(false)}
                                >
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