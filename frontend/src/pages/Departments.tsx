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

    async function handleCreate(e: React.FormEvent) {
        e.preventDefault()
        if (!newName.trim()) return
        try {
            await departmentApi.create(newName.trim())
            setNewName('')
            await fetchDepartments()
        } catch {
            setError('Failed to create department')
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
            </div>

            {error && <div className="error-message">{error}</div>}

            <form className="lookup-add-form" onSubmit={handleCreate}>
                <input
                    type="text"
                    placeholder="New department name (e.g. Engineering)"
                    value={newName}
                    onChange={e => setNewName(e.target.value)}
                    className="lookup-input"
                />
                <button type="submit" className="btn-add">+ Add</button>
            </form>

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
        </div>
    )
}

export default Departments

