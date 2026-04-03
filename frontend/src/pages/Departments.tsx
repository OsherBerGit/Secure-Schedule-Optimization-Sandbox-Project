import { useState, useEffect, useCallback } from 'react'
import type { Department } from '../types'
import { departmentApi } from '../api'
import './LookupTable.css'

const Departments = () => {
    const [departments, setDepartments] = useState<Department[]>([])
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [newName, setNewName] = useState('')
    const [editId, setEditId] = useState<number | null>(null)
    const [editName, setEditName] = useState('')
    const [isModalOpen, setIsModalOpen] = useState(false)

    const fetchDepartments = useCallback(async () => {
        setIsLoading(true)
        try {
            const res = await departmentApi.getAll()
            setDepartments(res.data)
        } catch {
            setError('Failed to load departments')
        } finally {
            setIsLoading(false)
        }
    }, [])

    useEffect(() => { void fetchDepartments() }, [fetchDepartments])

    async function handleAddDepartment(e: React.FormEvent) {
        e.preventDefault();
        console.log("1. Button Clicked. Current input value:", newName);

        if (!newName.trim()) {
            setError("Input is empty!");
            return;
        }

        try {
            console.log("2. Sending API request...");
            const response = await departmentApi.create(newName.trim());
            console.log("3. API Success:", response);
            setNewName('');
            setIsModalOpen(false);
            setError(null);
            await fetchDepartments();
        } catch (err: any) {
            console.error("4. API Error:", err);
            setError("Failed to add: " + (err.response?.data?.message || err.message));
        }
    }

    async function handleUpdate(id: number) {
        if (!editName.trim()) return
        try {
            await departmentApi.update(id, editName.trim())
            setEditId(null)
            setEditName('')
            await fetchDepartments()
        } catch {
            setError('Failed to update department')
        }
    }

    async function handleDelete(id: number) {
        if (!window.confirm('Delete this department?')) return
        try {
            await departmentApi.delete(id)
            await fetchDepartments()
        } catch {
            setError('Failed to delete department')
        }
    }

    return (
        <div className="lookup-container">
            <div className="lookup-header">
                <h1>🏢 Departments</h1>
                <button className="btn-add" onClick={() => setIsModalOpen(true)}>+ Add Department</button>
            </div>

            {error && <div className="error-message">{error}</div>}

            {isLoading ? (
                <div className="loading">Loading...</div>
            ) : departments.length === 0 ? (
                <p className="lookup-empty">No departments yet. Add one above.</p>
            ) : (
                <table className="lookup-table">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Name</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {departments.map(d => (
                            <tr key={d.id}>
                                <td>{d.id}</td>
                                <td>
                                    {editId === d.id ? (
                                        <input
                                            className="lookup-input-inline"
                                            value={editName}
                                            onChange={e => setEditName(e.target.value)}
                                        />
                                    ) : (
                                        <span className="lookup-badge">{d.name}</span>
                                    )}
                                </td>
                                <td>
                                    {editId === d.id ? (
                                        <>
                                            <button className="btn-save" onClick={() => handleUpdate(d.id)}>Save</button>
                                            <button className="btn-cancel" onClick={() => setEditId(null)}>Cancel</button>
                                        </>
                                    ) : (
                                        <>
                                            <button className="btn-edit" onClick={() => { setEditId(d.id); setEditName(d.name) }}>Edit</button>
                                            <button className="btn-delete" onClick={() => handleDelete(d.id)}>Delete</button>
                                        </>
                                    )}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            )}

            {isModalOpen && (
                <div className="modal-overlay" onClick={() => setIsModalOpen(false)}>
                    <div className="modal" onClick={e => e.stopPropagation()}>
                        <div className="modal-header">
                            <h2>Add New Department</h2>
                            <button className="btn-close" onClick={() => setIsModalOpen(false)}>×</button>
                        </div>
                        {error && <div className="error-message">{error}</div>}
                        <form className="modal-form" onSubmit={handleAddDepartment}>
                            <div className="form-group">
                                <label>New department name *</label>
                                <input
                                    type="text"
                                    placeholder="Enter the department name..."
                                    value={newName}
                                    onChange={e => setNewName(e.target.value)}
                                    required
                                />
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn-cancel" onClick={() => setIsModalOpen(false)}>Cancel</button>
                                <button type="submit" className="btn-save">Save</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    )
}

export default Departments
