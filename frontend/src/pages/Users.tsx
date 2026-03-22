import { useState, useEffect, useCallback, useMemo } from 'react'
import type { User, CreateUserRequest, UpdateUserRequest, Department } from '../types'
import { userApi, departmentApi } from '../api'
import { useAuth } from '../context/useAuth'
import UserModal from '../components/UserModal'
import './Users.css'

const Users = () => {
    const { user: currentUser } = useAuth()
    const [users, setUsers] = useState<User[]>([])
    const [departments, setDepartments] = useState<Department[]>([])
    
    // Filters
    const [search, setSearch] = useState('')
    const [filterDepartment, setFilterDepartment] = useState<string>('')
    const [filterRole, setFilterRole] = useState<string>('')

    // Sorting
    const [sortField, setSortField] = useState<'id' | 'name' | 'nationalId'>('name')
    const [sortDir, setSortDir] = useState<'asc' | 'desc'>('asc')

    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [showModal, setShowModal] = useState(false)
    const [selectedUser, setSelectedUser] = useState<User | null>(null)

    const fetchUsers = useCallback(async () => {
        setIsLoading(true)
        try {
            const [usersRes, deptsRes] = await Promise.all([
                userApi.getAll(),
                departmentApi.getAll()
            ])
            setUsers(usersRes.data)
            setDepartments(deptsRes.data)
        } catch (err: unknown) {
            setError(err instanceof Error ? err.message : 'Failed to load users')
        } finally {
            setIsLoading(false)
        }
    }, [])

    useEffect(() => { void fetchUsers() }, [fetchUsers])

    // Filter & Sort Logic
    const filteredUsers = useMemo(() => {
        return users.filter(u => {
            // Text Search
            const searchLower = search.toLowerCase()
            const matchesSearch = 
                (u.firstName?.toLowerCase() + ' ' + u.lastName?.toLowerCase()).includes(searchLower) ||
                u.nationalId.includes(searchLower) ||
                u.email?.toLowerCase().includes(searchLower) ||
                (u.departmentName?.toLowerCase() || '').includes(searchLower) ||
                u.roles.some(r => r.toLowerCase().includes(searchLower))

            // Department Filter
            const matchesDept = filterDepartment ? u.departmentName === filterDepartment : true

            // Role Filter
            const matchesRole = filterRole ? u.roles.includes(filterRole) : true

            return matchesSearch && matchesDept && matchesRole
        }).sort((a, b) => {
            let valA = ''
            let valB = ''

            if (sortField === 'name') {
                valA = (a.firstName + ' ' + a.lastName).toLowerCase()
                valB = (b.firstName + ' ' + b.lastName).toLowerCase()
            } else if (sortField === 'nationalId') {
                valA = a.nationalId
                valB = b.nationalId
            } else {
                return 0
            } // Default sort is effectively by id via DB order usually, but here we sort explicitly if field set.

            if (valA < valB) return sortDir === 'asc' ? -1 : 1
            if (valA > valB) return sortDir === 'asc' ? 1 : -1
            return 0
        })
    }, [users, search, filterDepartment, filterRole, sortField, sortDir])

    function toggleSort(field: 'name' | 'nationalId') {
        if (sortField === field) {
            setSortDir(prev => prev === 'asc' ? 'desc' : 'asc')
        } else {
            setSortField(field)
            setSortDir('asc')
        }
    }

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

            <div className="filter-row">
                <input
                    type="text"
                    className="modern-input"
                    placeholder="🔍 Search name, role, etc..."
                    value={search}
                    onChange={e => setSearch(e.target.value)}
                    style={{ minWidth: '300px' }}
                />
                
                <select 
                    className="modern-select"
                    value={filterDepartment}
                    onChange={e => setFilterDepartment(e.target.value)}
                >
                    <option value="">All Departments</option>
                    {departments.map(d => (
                        <option key={d.id} value={d.name}>{d.name}</option>
                    ))}
                </select>

                <select 
                    className="modern-select"
                    value={filterRole}
                    onChange={e => setFilterRole(e.target.value)}
                >
                    <option value="">All Roles</option>
                    <option value="ADMIN">Admin</option>
                    <option value="MANAGER">Manager</option>
                    <option value="WORKER">Worker</option>
                </select>
            </div>

            {isLoading ? (
                <div className="loading">Loading...</div>
            ) : (
                <table className="users-table">
                    <thead>
                        <tr>
                            <th onClick={() => toggleSort('nationalId')}>National ID</th>
                            <th onClick={() => toggleSort('name')}>First Name</th>
                            <th>Last Name</th>
                            <th>Email</th>
                            <th>Phone</th>
                            <th>Role</th>
                            <th>Department</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {filteredUsers.map(user => {
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
