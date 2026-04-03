import { useState, useEffect, useCallback, useMemo } from 'react'
import type { User, CreateUserRequest, UpdateUserRequest, Department, Skill } from '../types'
import { userApi, departmentApi, skillApi } from '../api'
import { useAuth } from '../context/useAuth'
import UserModal from '../components/UserModal'
import './Users.css'

const Users = () => {
    const { user: currentUser } = useAuth()

    const isManager = currentUser?.role === 'MANAGER'

    const [users, setUsers] = useState<User[]>([])
    const [departments, setDepartments] = useState<Department[]>([])
    const [allSkills, setAllSkills] = useState<Skill[]>([])

    // Filters
    const [search, setSearch] = useState('')
    const [filterDepartment, setFilterDepartment] = useState<string>('')
    const [filterRole, setFilterRole] = useState<string>('')
    const [filterSkill, setFilterSkill] = useState<string>('')

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
            // Managers should only fetch their department users, unless departmentId is null
            // In a better real world scenario, the backend secures this but here we respect user's scope
            const usersPromise = isManager && currentUser?.departmentId
                ? userApi.getByDepartment(currentUser.departmentId)
                : userApi.getAll()

            const [usersRes, deptsRes] = await Promise.all([
                usersPromise,
                departmentApi.getAll()
            ])
            setUsers(usersRes.data)
            setDepartments(deptsRes.data)

            const skillsRes = await skillApi.getAll()
            setAllSkills(skillsRes.data)
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
                (u.role && u.role.toLowerCase().includes(searchLower))

            // Department Filter
            const matchesDept = filterDepartment ? u.departmentName === filterDepartment : true

            // Role Filter
            const matchesRole = filterRole ? u.role === filterRole : true

            // Skill Filter
            const matchesSkill = filterSkill ? u.skills?.some(s => s.name === filterSkill) : true

            return matchesSearch && matchesDept && matchesRole && matchesSkill
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
    }, [users, search, filterDepartment, filterRole, filterSkill, sortField, sortDir])

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
            return userApi.update(selectedUser.id, formData as UpdateUserRequest)
                .then(() => {
                    setShowModal(false)
                    setSelectedUser(null)
                    fetchUsers()
                })
        } else {
            return userApi.create(formData as CreateUserRequest)
                .then(() => {
                    setShowModal(false)
                    fetchUsers()
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
                
                {(currentUser?.role === 'ADMIN' || currentUser?.role === 'MANAGER') && (
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
                )}

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

                <select
                    className="modern-select"
                    value={filterSkill}
                    onChange={e => setFilterSkill(e.target.value)}
                >
                    <option value="">All Skills</option>
                    {allSkills.map(s => (
                        <option key={s.id} value={s.name}>{s.name}</option>
                    ))}
                </select>
            </div>

            {isLoading ? (
                <div className="loading">Loading...</div>
            ) : filteredUsers.length === 0 ? (
                <div className="empty-state" style={{ textAlign: 'center', padding: '2rem', color: '#64748b' }}>No records found matching the selected filters.</div>
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
                            <th>Skills</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {filteredUsers.map(user => {
                            const role = user.role ?? 'WORKER'
                            return (
                                <tr key={user.id}>
                                    <td>{user.nationalId}</td>
                                    <td>{user.firstName ?? '-'}</td>
                                    <td>{user.lastName ?? '-'}</td>
                                    <td>{user.email ?? '-'}</td>
                                    <td>{user.phoneNumber ?? '-'}</td>
                                    <td><span className={`role-badge role-${role.toLowerCase()}`}>{role}</span></td>
                                    <td>
                                        {user.departmentName
                                            ? <span className="dept-badge">{user.departmentName}</span>
                                            : <span className="dept-unassigned">-</span>}
                                    </td>
                                    <td>
                                        {user.skills && user.skills.length > 0 ? (
                                            <div style={{ display: 'flex', gap: '4px', flexWrap: 'wrap' }}>
                                                {user.skills.map(s => (
                                                    <span key={s.id} className="role-badge role-worker" style={{ fontSize: '0.75rem', backgroundColor: '#e2e8f0', color: '#4a5568', padding: '0.2rem 0.5rem', borderRadius: '12px' }}>{s.name}</span>
                                                ))}
                                            </div>
                                        ) : (
                                            <span style={{ color: '#888', fontStyle: 'italic', fontSize: '0.85rem' }}>No skills</span>
                                        )}
                                    </td>
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
