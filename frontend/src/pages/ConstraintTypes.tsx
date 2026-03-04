import { useState, useEffect, useCallback } from 'react'
import type { ConstraintType } from '../types'
import { constraintTypeApi } from '../api'
import './LookupTable.css'

const ConstraintTypes = () => {
    const [items, setItems] = useState<ConstraintType[]>([])
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    // Create form state
    const [newName, setNewName] = useState('')
    const [newDesc, setNewDesc] = useState('')

    // Inline edit state
    const [editId, setEditId] = useState<number | null>(null)
    const [editName, setEditName] = useState('')
    const [editDesc, setEditDesc] = useState('')

    const fetchAll = useCallback(async () => {
        setIsLoading(true)
        try {
            const res = await constraintTypeApi.getAll()
            setItems(res.data)
        } catch {
            setError('Failed to load constraint types')
        } finally {
            setIsLoading(false)
        }
    }, [])

    useEffect(() => { void fetchAll() }, [fetchAll])

    async function handleCreate(e: React.FormEvent) {
        e.preventDefault()
        if (!newName.trim()) return
        try {
            await constraintTypeApi.create({ name: newName.trim(), description: newDesc.trim() || undefined })
            setNewName('')
            setNewDesc('')
            await fetchAll()
        } catch {
            setError('Failed to create constraint type')
        }
    }

    async function handleUpdate(id: number) {
        if (!editName.trim()) return
        try {
            await constraintTypeApi.update(id, { name: editName.trim(), description: editDesc.trim() || undefined })
            setEditId(null)
            await fetchAll()
        } catch {
            setError('Failed to update constraint type')
        }
    }

    async function handleDelete(id: number) {
        if (!window.confirm('Delete this constraint type?')) return
        try {
            await constraintTypeApi.delete(id)
            await fetchAll()
        } catch {
            setError('Failed to delete constraint type')
        }
    }

    return (
        <div className="lookup-container">
            <div className="lookup-header">
                <h1>🔗 Constraint Types</h1>
            </div>

            {error && <div className="error-message">{error}</div>}

            <form className="lookup-add-form" onSubmit={handleCreate}>
                <input
                    type="text"
                    placeholder="Name (e.g. FINISH_TO_START)"
                    value={newName}
                    onChange={e => setNewName(e.target.value)}
                    className="lookup-input"
                />
                <input
                    type="text"
                    placeholder="Description (optional)"
                    value={newDesc}
                    onChange={e => setNewDesc(e.target.value)}
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
                            <th>Description</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {items.map(item => (
                            <tr key={item.id}>
                                <td>{item.id}</td>
                                <td>
                                    {editId === item.id ? (
                                        <input className="lookup-input-inline" value={editName}
                                            onChange={e => setEditName(e.target.value)} />
                                    ) : (
                                        <span className="lookup-badge">{item.name}</span>
                                    )}
                                </td>
                                <td>
                                    {editId === item.id ? (
                                        <input className="lookup-input-inline" value={editDesc}
                                            onChange={e => setEditDesc(e.target.value)} />
                                    ) : (
                                        item.description ?? '—'
                                    )}
                                </td>
                                <td>
                                    {editId === item.id ? (
                                        <>
                                            <button className="btn-save" onClick={() => handleUpdate(item.id)}>Save</button>
                                            <button className="btn-cancel" onClick={() => setEditId(null)}>Cancel</button>
                                        </>
                                    ) : (
                                        <>
                                            <button className="btn-edit" onClick={() => {
                                                setEditId(item.id)
                                                setEditName(item.name)
                                                setEditDesc(item.description ?? '')
                                            }}>Edit</button>
                                            <button className="btn-delete" onClick={() => handleDelete(item.id)}>Delete</button>
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

export default ConstraintTypes
