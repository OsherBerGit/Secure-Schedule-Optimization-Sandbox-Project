import { useState, useEffect, useCallback } from 'react'
import type { User, CreateUserRequest, UpdateUserRequest } from '../types'
import { userApi } from '../api'
import { useAuth } from '../context/useAuth'
import UserModal from '../components/UserModal'
import './Users.css'

const Users = () => {
    const { user: currentUser } = useAuth()
    const [users, setUsers] = useState<User[]>([])
    const [search, setSearch] = useState('')
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [showModal, setShowModal] = useState(false)
    const [selectedUser, setSelectedUser] = useState<User | null>(null)

    const fetchUsers = useCallback(async () => {
        setIsLoading(true)
        try {
            const response = await userApi.getAll()
            setUsers(response.data)
        } catch (err: unknown) {
            setError(err instanceof Error ? err.message : 'Failed to load users')
        } finally {
            setIsLoading(false)
        }
    }, [])

    useEffect(() => { void fetchUsers() }, [fetchUsers])

    function handleEdit(user: User) {
        setSelectedUser(user)
        setShowModal(true)
    }

    function handleDelete(id: number) {
        userApi.delete(id)
            .then(() => {
                fetchUsers()
            })
            .catch(error => {
                setError(error.message)
            })
    }

    function handleSubmit(formData: CreateUserRequest | UpdateUserRequest) {
        if (selectedUser) {
            userApi.update(selectedUser.id, formData as UpdateUserRequest)
                .then(() => {
                    setShowModal(false)
                    setSelectedUser(null)
                    fetchUsers()
                })
                .catch(error => {
                    setError(error.message)
                })
        } else {
            userApi.create(formData as CreateUserRequest)
                .then(() => {
                    setShowModal(false)
                    fetchUsers()
                })
                .catch(error => {
                    setError(error.message)
                })
        }
    }

    function handleAddUser() {
        setSelectedUser(null)
        setShowModal(true)
    }

    return (
        <div className="users-container">
            <div className="users-header">
                <h1>Users Management</h1>
                {currentUser?.role === 'ADMIN' && (
                    <button className="btn-add" onClick={handleAddUser}>+ Add User</button>
                )}
            </div>

            {error && <div className="error-message">{error}</div>}

            <div className="users-search-bar">
                <input
                    type="text"
                    className="users-search-input"
                    placeholder="🔍 Search by name, role, or department…"
                    value={search}
                    onChange={e => setSearch(e.target.value)}
                />
            </div>

            {isLoading ? (
                <div className="loading">Loading...</div>
            ) : (
                <table className="users-table">
                    <thead>
                        <tr>
                            <th>National ID</th>
                            <th>First Name</th>
                            <th>Last Name</th>
                            <th>Email</th>
                            <th>Phone</th>
                            <th>Role</th>
                            <th>Department</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {users
                            .filter(u => {
                                if (!search.trim()) return true
                                const q = search.toLowerCase()
                                const role = u.role ?? (u.roles?.includes('ADMIN') ? 'ADMIN' : 'WORKER')
                                return (
                                    (u.firstName ?? '').toLowerCase().includes(q) ||
                                    (u.lastName ?? '').toLowerCase().includes(q) ||
                                    role.toLowerCase().includes(q) ||
                                    (u.departmentName ?? '').toLowerCase().includes(q)
                                )
                            })
                            .map(user => {
                                const role = user.role ?? (user.roles?.includes('ADMIN') ? 'ADMIN' : 'WORKER')
                                return (
                                    <tr key={user.id}>
                                        <td>{user.nationalId}</td>
                                        <td>{user.firstName ?? '—'}</td>
                                        <td>{user.lastName ?? '—'}</td>
                                        <td>{user.email ?? '—'}</td>
                                        <td>{user.phoneNumber ?? '—'}</td>
                                        <td><span className={`role-badge role-${role.toLowerCase()}`}>{role}</span></td>
                                        <td>{user.departmentName ?? <span className="dept-unassigned">—</span>}</td>
                                        <td>
                                            <button className="btn-edit" onClick={() => handleEdit(user)}>Edit</button>
                                            <button className="btn-delete" onClick={() => handleDelete(user.id)}>Delete</button>
                                        </td>
                                    </tr>
                                )
                            })}
                    </tbody>
                </table>
            )}

            {showModal && (
                <UserModal
                    user={selectedUser}
                    onSubmit={handleSubmit}
                    onClose={() => { setShowModal(false); setSelectedUser(null) }}
                />
            )}
        </div>
    )
}

export default Users
