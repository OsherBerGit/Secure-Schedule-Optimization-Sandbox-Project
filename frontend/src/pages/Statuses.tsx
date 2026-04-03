import { useState, useEffect, useCallback } from 'react'
import type { Status, SettlementStatus } from '../types'
import { statusApi, settlementStatusApi } from '../api'
import './LookupTable.css'
import './Statuses.css'

const Statuses = () => {
    // ── Task Statuses (full CRUD) ─────────────────────────────────────────────
    const [statuses, setStatuses] = useState<Status[]>([])
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [newName, setNewName] = useState('')
    const [editId, setEditId] = useState<number | null>(null)
    const [editName, setEditName] = useState('')

    // ── Assignment / Settlement Statuses (read-only) ──────────────────────────
    const [settlementStatuses, setSettlementStatuses] = useState<SettlementStatus[]>([])
    const [settlLoading, setSettlLoading] = useState(false)
    const [settlError, setSettlError] = useState<string | null>(null)

    const fetchStatuses = useCallback(async () => {
        setIsLoading(true)
        try {
            const res = await statusApi.getAll()
            setStatuses(res.data)
        } catch {
            setError('Failed to load task statuses')
        } finally {
            setIsLoading(false)
        }
    }, [])

    const fetchSettlementStatuses = useCallback(async () => {
        setSettlLoading(true)
        try {
            const res = await settlementStatusApi.getAll()
            setSettlementStatuses(res.data)
        } catch {
            setSettlError('Failed to load assignment statuses')
        } finally {
            setSettlLoading(false)
        }
    }, [])

    useEffect(() => {
        void fetchStatuses()
        void fetchSettlementStatuses()
    }, [fetchStatuses, fetchSettlementStatuses])

    async function handleCreate(e: React.FormEvent) {
        e.preventDefault()
        if (!newName.trim()) return
        try {
            await statusApi.create({ name: newName.trim() })
            setNewName('')
            await fetchStatuses()
        } catch {
            setError('Failed to create status')
        }
    }

    async function handleUpdate(id: number) {
        if (!editName.trim()) return
        try {
            await statusApi.update(id, { name: editName.trim() })
            setEditId(null)
            setEditName('')
            await fetchStatuses()
        } catch {
            setError('Failed to update status')
        }
    }

    async function handleDelete(id: number) {
        if (!window.confirm('Delete this status?')) return
        try {
            await statusApi.delete(id)
            await fetchStatuses()
        } catch {
            setError('Failed to delete status')
        }
    }

    return (
        <div className="lookup-container">
            <div className="lookup-header">
                <h1>📊 Statuses</h1>
            </div>

            <div className="statuses-split-grid">

                {/* ── LEFT: Task Statuses (editable) ── */}
                <section className="statuses-panel">
                    <h2 className="statuses-panel-title">🗂️ Task Statuses</h2>
                    <p className="statuses-panel-subtitle">Lifecycle states for Tasks (e.g. OPEN, LOCKED, CLOSED)</p>

                    {error && <div className="error-message">{error}</div>}

                    <form className="lookup-add-form" onSubmit={handleCreate}>
                        <input
                            type="text"
                            placeholder="New status name (e.g. OPEN)"
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
                                {statuses.length === 0 ? (
                                    <tr><td colSpan={3} className="lookup-empty">No task statuses yet.</td></tr>
                                ) : statuses.map(s => (
                                    <tr key={s.id}>
                                        <td>{s.id}</td>
                                        <td>
                                            {editId === s.id ? (
                                                <input
                                                    className="lookup-input-inline"
                                                    value={editName}
                                                    onChange={e => setEditName(e.target.value)}
                                                />
                                            ) : (
                                                <span className="lookup-badge">{s.name}</span>
                                            )}
                                        </td>
                                        <td>
                                            {editId === s.id ? (
                                                <>
                                                    <button className="btn-save" onClick={() => handleUpdate(s.id)}>Save</button>
                                                    <button className="btn-cancel" onClick={() => setEditId(null)}>Cancel</button>
                                                </>
                                            ) : (
                                                <>
                                                    <button className="btn-edit" onClick={() => { setEditId(s.id); setEditName(s.name) }}>Edit</button>
                                                    <button className="btn-delete" onClick={() => handleDelete(s.id)}>Delete</button>
                                                </>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                </section>

                {/* ── RIGHT: Assignment Statuses (read-only, system-seeded) ── */}
                <section className="statuses-panel statuses-panel--readonly">
                    <h2 className="statuses-panel-title">📋 Assignment Statuses</h2>
                    <p className="statuses-panel-subtitle">Execution states for Settlements - system-managed, read-only</p>

                    {settlError && <div className="error-message">{settlError}</div>}

                    {settlLoading ? (
                        <div className="loading">Loading...</div>
                    ) : (
                        <table className="lookup-table">
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>Name</th>
                                    <th>Colour</th>
                                </tr>
                            </thead>
                            <tbody>
                                {settlementStatuses.length === 0 ? (
                                    <tr><td colSpan={3} className="lookup-empty">No assignment statuses found.</td></tr>
                                ) : settlementStatuses.map(s => (
                                    <tr key={s.id}>
                                        <td>{s.id}</td>
                                        <td>
                                            <span
                                                className="lookup-badge"
                                                style={s.colorCode ? {
                                                    background: s.colorCode + '22',
                                                    color: s.colorCode,
                                                    border: `1px solid ${s.colorCode}55`,
                                                } : undefined}
                                            >
                                                {s.name}
                                            </span>
                                        </td>
                                        <td>
                                            {s.colorCode ? (
                                                <span className="color-swatch-cell">
                                                    <span
                                                        className="color-swatch"
                                                        style={{ background: s.colorCode }}
                                                    />
                                                    {s.colorCode}
                                                </span>
                                            ) : '-'}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                    <p className="statuses-readonly-note">
                        ℹ️ These values are seeded by the system and cannot be edited here.
                    </p>
                </section>

            </div>
        </div>
    )
}

export default Statuses

