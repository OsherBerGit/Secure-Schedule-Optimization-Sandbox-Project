import { useState, useEffect, useCallback } from 'react'
import type { Priority } from '../types'
import { priorityApi } from '../api'
import './LookupTable.css'

const Priorities = () => {
    const [priorities, setPriorities] = useState<Priority[]>([])
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [newName, setNewName] = useState('')
    const [editId, setEditId] = useState<number | null>(null)
    const [editName, setEditName] = useState('')

    const fetchPriorities = useCallback(async () => {
        setIsLoading(true)
        try {
            const res = await priorityApi.getAll()
            setPriorities(res.data)
        } catch {
            setError('Failed to load priorities')
        } finally {
            setIsLoading(false)
        }
    }, [])

    useEffect(() => { void fetchPriorities() }, [fetchPriorities])

    async function handleCreate(e: React.FormEvent) {
        e.preventDefault()
        if (!newName.trim()) return
        try {
            await priorityApi.create({ name: newName.trim() })
            setNewName('')
            await fetchPriorities()
        } catch {
            setError('Failed to create priority')
        }
    }

    async function handleUpdate(id: number) {
        if (!editName.trim()) return
        try {
            await priorityApi.update(id, { name: editName.trim() })
            setEditId(null)
            setEditName('')
            await fetchPriorities()
        } catch {
            setError('Failed to update priority')
        }
    }

    async function handleDelete(id: number) {
        if (!window.confirm('Delete this priority?')) return
        try {
            await priorityApi.delete(id)
            await fetchPriorities()
        } catch {
            setError('Failed to delete priority')
        }
    }

    return (
        <div className="lookup-container">
            <div className="lookup-header">
                <h1>⭐ Task Priorities</h1>
            </div>

            {error && <div className="error-message">{error}</div>}

            <form className="lookup-add-form" onSubmit={handleCreate}>
                <input
                    type="text"
                    placeholder="New priority name (e.g. HIGH)"
                    value={newName}
                    onChange={e => setNewName(e.target.value)}
                    className="lookup-input"
                />
                <button type="submit" className="btn-add">+ Add</button>
            </form>

            {isLoading ? (
                <div className="loading">Loading...</div>
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
                        {priorities.map(p => (
                            <tr key={p.id}>
                                <td>{p.id}</td>
                                <td>
                                    {editId === p.id ? (
                                        <input
                                            className="lookup-input-inline"
                                            value={editName}
                                            onChange={e => setEditName(e.target.value)}
                                        />
                                    ) : (
                                        <span className="lookup-badge">{p.name}</span>
                                    )}
                                </td>
                                <td>
                                    {editId === p.id ? (
                                        <>
                                            <button className="btn-save" onClick={() => handleUpdate(p.id)}>Save</button>
                                            <button className="btn-cancel" onClick={() => setEditId(null)}>Cancel</button>
                                        </>
                                    ) : (
                                        <>
                                            <button className="btn-edit" onClick={() => { setEditId(p.id); setEditName(p.name) }}>Edit</button>
                                            <button className="btn-delete" onClick={() => handleDelete(p.id)}>Delete</button>
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

export default Priorities

