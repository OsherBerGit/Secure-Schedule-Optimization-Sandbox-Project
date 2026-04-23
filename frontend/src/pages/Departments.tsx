import { useState, useEffect, useCallback, useMemo } from 'react';
import type { Department } from '../types';
import { departmentApi } from '../api';
import { Building2, Pencil, Trash2, X, Check, Search } from 'lucide-react';
import './Departments.css';

const Departments = () => {
    const [departments, setDepartments] = useState<Department[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [newName, setNewName] = useState('');
    const [editId, setEditId] = useState<number | null>(null);
    const [editName, setEditName] = useState('');
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');

    const fetchDepartments = useCallback(async () => {
        setIsLoading(true);
        try {
            const res = await departmentApi.getAll();
            setDepartments(res.data);
        } catch { setError('Failed to load departments'); } finally { setIsLoading(false); }
    }, []);

    useEffect(() => { void fetchDepartments(); }, [fetchDepartments]);

    async function handleAddDepartment(e: React.FormEvent) {
        e.preventDefault(); if (!newName.trim()) return setError("Input is empty!");
        try {
            await departmentApi.create(newName.trim());
            setNewName(''); setIsModalOpen(false); setError(null); await fetchDepartments();
        } catch (err: any) { setError("Failed to add: " + (err.response?.data?.message || err.message)); }
    }

    async function handleUpdate(id: number) {
        if (!editName.trim()) return;
        try { await departmentApi.update(id, editName.trim()); setEditId(null); setEditName(''); await fetchDepartments(); }
        catch { setError('Failed to update department'); }
    }

    async function handleDelete(id: number) {
        if (!window.confirm('Are you sure you want to delete this department?')) return;
        try { await departmentApi.delete(id); await fetchDepartments(); } catch { setError('Failed to delete department'); }
    }

    const filteredDepartments = useMemo(() => {
        if (!searchQuery.trim()) return departments;
        return departments.filter(d => d.name.toLowerCase().includes(searchQuery.toLowerCase()));
    }, [departments, searchQuery]);

    return (
        <div className="departments-page">
            <div className="page-header">
                <div className="page-header-title"><Building2 className="text-primary" size={28} color="var(--primary-color)" /><h1>Departments Management</h1></div>
                <button className="btn-add-primary" onClick={() => setIsModalOpen(true)}>Add Department</button>
            </div>
            <div className="filters-container">
                <div className="search-wrapper"><Search className="search-icon" size={18} /><input type="text" className="modern-input search-input" placeholder="Search departments..." value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} /></div>
            </div>
            {error && <div className="error-message">{error}</div>}
            <div className="table-container">
                {isLoading ? <div className="loading-state">Loading departments...</div> : filteredDepartments.length === 0 ? <div className="empty-state">No departments found.</div> : (
                    <table className="modern-table departments-table">
                        <thead><tr><th className="th-id">ID</th><th className="th-name">Department Name</th><th className="th-actions">Actions</th></tr></thead>
                        <tbody>
                        {filteredDepartments.map(d => (
                            <tr key={d.id}>
                                <td className="id-cell">{d.id}</td>
                                <td className="name-cell">{editId === d.id ? <input className="inline-edit-input" value={editName} onChange={e => setEditName(e.target.value)} autoFocus /> : <span className="static-name">{d.name}</span>}</td>
                                <td className="actions-cell">
                                    <div className="actions-container">
                                        {editId === d.id ? (
                                            <><button className="btn-icon approve-btn" onClick={() => handleUpdate(d.id)}><Check size={18} /></button><button className="btn-icon delete-btn" onClick={() => setEditId(null)}><X size={18} /></button></>
                                        ) : (
                                            <><button className="btn-icon edit-btn" onClick={() => { setEditId(d.id); setEditName(d.name); }}><Pencil size={16} /></button><button className="btn-icon delete-btn" onClick={() => handleDelete(d.id)}><Trash2 size={16} /></button></>
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
                    <div className="modern-modal-card" style={{ maxWidth: '450px', width: '90%' }}>
                        <div className="modal-header"><h2><Building2 size={22} className="text-primary" /> Add New Department</h2><button type="button" className="modern-close-btn" onClick={() => setIsModalOpen(false)}><X size={24} /></button></div>
                        <form onSubmit={handleAddDepartment} className="modern-modal-form">
                            <div className="modal-body" style={{ padding: '2rem' }}><div className="modern-form-group"><label>Department Name *</label><input type="text" className="modern-input" placeholder="e.g. Engineering" value={newName} onChange={e => setNewName(e.target.value)} required autoFocus /></div></div>
                            <div className="modal-actions"><button type="button" className="btn-cancel" onClick={() => setIsModalOpen(false)}>Cancel</button><button type="submit" className="btn-submit">Save Department</button></div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Departments;