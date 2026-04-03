import { useState, useEffect, useCallback } from 'react'
import type { Skill } from '../types'
import { skillApi } from '../api'
import './LookupTable.css'

const Skills = () => {
    const [items, setItems] = useState<Skill[]>([])
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    // Create form state
    const [newName, setNewName] = useState('')
    const [newDesc, setNewDesc] = useState('')

    // Inline edit state
    const [editId, setEditId] = useState<number | null>(null)
    const [editName, setEditName] = useState('')
    const [editDesc, setEditDesc] = useState('')
    const [isModalOpen, setIsModalOpen] = useState(false)

    const fetchAll = useCallback(async () => {
        setIsLoading(true)
        setError(null)
        try {
            const res = await skillApi.getAll()
            setItems(res.data)
        } catch (err: any) {
            console.error("Failed to load skills:", err)
            setError(err?.response?.data?.message || err.message || 'Failed to load skills')
        } finally {
            setIsLoading(false)
        }
    }, [])

    useEffect(() => { void fetchAll() }, [fetchAll])

    async function handleCreate(e: React.FormEvent) {
        e.preventDefault();
        console.log("1. Button Clicked. Current input value:", newName);
        
        if (!newName.trim()) {
            setError("Input is empty!");
            return;
        }
        
        try {
            console.log("2. Sending API request...");
            const response = await skillApi.create(newName.trim(), newDesc.trim() || undefined);
            console.log("3. API Success:", response);
            setNewName('');
            setNewDesc('');
            setIsModalOpen(false);
            setError(null);
            await fetchAll();
        } catch (err: any) {
            console.error("4. API Error:", err);
            setError("Failed to add: " + (err.response?.data?.message || err.message));
        }
    }

    async function handleUpdate(id: number) {
        if (!editName.trim()) return
        setError(null)
        try {
            await skillApi.update(id, editName.trim(), editDesc.trim() || undefined)
            setEditId(null)
            await fetchAll()
        } catch (err: any) {
            console.error("Failed to update skill:", err)
            setError(err?.response?.data?.message || err.message || 'Failed to update skill')
        }
    }

    async function handleDelete(id: number) {
        if (!window.confirm('Delete this skill?')) return
        setError(null)
        try {
            await skillApi.delete(id)
            await fetchAll()
        } catch (err: any) {
            console.error("Failed to delete skill:", err)
            setError(err?.response?.data?.message || err.message || 'Failed to delete skill')
        }
    }

    return (
        <div className="lookup-container">
            <div className="lookup-header">
                <h1>💡 Skills</h1>
                <button className="btn-add" onClick={() => setIsModalOpen(true)}>+ Add Skill</button>
            </div>

            {error && <div className="error-message">{error}</div>}

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
                                        item.description ?? '-'
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

            {isModalOpen && (
                <div className="modal-overlay" onClick={() => setIsModalOpen(false)}>
                    <div className="modal" onClick={e => e.stopPropagation()}>
                        <div className="modal-header">
                            <h2>Add New Skill</h2>
                            <button className="btn-close" onClick={() => setIsModalOpen(false)}>×</button>
                        </div>
                        {error && <div className="error-message">{error}</div>}
                        <form className="modal-form" onSubmit={handleCreate}>
                            <div className="form-group">
                                <label>Name *</label>
                                <input
                                    type="text"
                                    placeholder="Enter the skill name..."
                                    value={newName}
                                    onChange={e => setNewName(e.target.value)}
                                    required
                                />
                            </div>
                            <div className="form-group">
                                <label>Description</label>
                                <input
                                    type="text"
                                    placeholder="Enter a short description..."
                                    value={newDesc}
                                    onChange={e => setNewDesc(e.target.value)}
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

export default Skills
