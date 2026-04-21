import React, { useEffect, useState, useMemo, useCallback } from 'react';
import { userApi, departmentApi, skillApi } from '../api';
import type { User, Department, Skill } from '../types';
import UserModal from '../components/UserModal';
import { Search, Pencil, Trash2, Users as UsersIcon, Plane, CalendarDays } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import './Users.css';

const Users: React.FC = () => {
  const navigate = useNavigate();
  const [users, setUsers] = useState<User[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [skills, setSkills] = useState<Skill[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const [searchQuery, setSearchQuery] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [departmentFilter, setDepartmentFilter] = useState('');
  const [skillFilter, setSkillFilter] = useState('');

  const fetchUsers = useCallback(async () => {
    setIsLoading(true);
    try {
      const response = await userApi.getAll();
      setUsers(response.data);
    } catch {
      setError('Failed to fetch users');
    } finally {
      setIsLoading(false);
    }
  }, []);

  const fetchDepartments = async () => {
    try {
      const response = await departmentApi.getAll();
      setDepartments(response.data);
    } catch {
      setError('Failed to fetch departments');
    }
  };

  const fetchSkills = async () => {
    try {
      const response = await skillApi.getAll();
      setSkills(response.data);
    } catch {
      setError('Failed to fetch skills');
    }
  };

  useEffect(() => {
    fetchUsers();
    fetchDepartments();
    fetchSkills();
  }, [fetchUsers]);

  const handleOpenModal = (user?: User) => {
    setSelectedUser(user || null);
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setSelectedUser(null);
  };

  const handleDelete = async (nationalId: string) => {
    if (window.confirm('Are you sure you want to delete this user?')) {
      try {
        const u = users.find(u => u.nationalId === nationalId);
        if (u) await userApi.delete(u.id);
        fetchUsers();
      } catch {
        setError('Failed to delete user');
      }
    }
  };

  const handleSubmit = async (userData: any) => {
    try {
      if (selectedUser) {
        await userApi.update(selectedUser.id, userData);
      } else {
        await userApi.create(userData);
      }
      fetchUsers();
      handleCloseModal();
    } catch {
      setError('Failed to save user');
      throw new Error('Submit failed');
    }
  };

  const filteredUsers = useMemo(() => {
    return users.filter(u => {
      const fullName = `${u.firstName} ${u.lastName}`.toLowerCase();
      const matchesSearch = !searchQuery ||
          fullName.includes(searchQuery.toLowerCase()) ||
          u.nationalId.includes(searchQuery);
      const matchesRole = !roleFilter || u.role === roleFilter;
      const matchesDept = !departmentFilter || u.departmentName === departmentFilter;
      const matchesSkill = !skillFilter || u.skills?.some(s => s.name === skillFilter);
      return matchesSearch && matchesRole && matchesDept && matchesSkill;
    });
  }, [users, searchQuery, roleFilter, departmentFilter, skillFilter]);

  return (
      <div className="users-page">
        <div className="page-header">
          <div className="page-header-title">
            <UsersIcon className="text-primary" size={28} color="var(--primary-color)" />
            <h1>Users Management</h1>
          </div>
          <button className="btn-add-primary" onClick={() => handleOpenModal()}>
             Add New User
          </button>
        </div>

        <div className="filters-container">
          <div className="search-wrapper" style={{ flex: 1 }}>
            <Search className="search-icon" size={18} />
            <input
                type="text"
                className="modern-input search-input"
                placeholder="Search by name or ID..."
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
            />
          </div>
          <select className="modern-input" style={{ flex: '0 0 150px' }} value={roleFilter} onChange={e => setRoleFilter(e.target.value)}>
            <option value="">All Roles</option>
            <option value="ADMIN">Admin</option>
            <option value="MANAGER">Manager</option>
            <option value="WORKER">User</option>
          </select>
          <select className="modern-input" style={{ flex: '0 0 150px' }} value={departmentFilter} onChange={e => setDepartmentFilter(e.target.value)}>
            <option value="">All Departments</option>
            {departments.map(d => (
                <option key={d.id} value={d.name}>{d.name}</option>
            ))}
          </select>
          <select className="modern-input" style={{ flex: '0 0 150px' }} value={skillFilter} onChange={e => setSkillFilter(e.target.value)}>
            <option value="">All Skills</option>
            {skills.map(s => (
                <option key={s.id} value={s.name}>{s.name}</option>
            ))}
          </select>
        </div>

        {error && <div className="error-message">{error}</div>}

        <div className="table-container">
          {isLoading ? (
              <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-secondary)' }}>Loading users...</div>
          ) : filteredUsers.length === 0 ? (
              <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-secondary)' }}>No users found matching the filters.</div>
          ) : (
              <table className="modern-table users-table">
                <thead>
                <tr>
                  <th>National ID</th>
                  <th>Full Name</th>
                  <th>Email</th>
                  <th>Phone</th>
                  <th>Role</th>
                  <th>Department</th>
                  <th>Skills</th>
                  <th>Actions</th>
                </tr>
                </thead>
                <tbody>
                {filteredUsers.map(u => (
                    <tr key={u.nationalId}>
                      <td>{u.nationalId}</td>
                      <td style={{ fontWeight: 500, color: 'var(--text-primary)' }}>{u.firstName} {u.lastName}</td>
                      <td>{u.email}</td>
                      <td>{u.phoneNumber}</td>
                      <td>
                      <span className={`role-badge role-${u.role.toLowerCase()}`}>
                        {u.role}
                      </span>
                      </td>
                      <td>
                      <span className="department-badge">
                        {u.departmentName || 'General'}
                      </span>
                      </td>
                      <td>
                        <div className="skills-container-table">
                          {u.skills && u.skills.length > 0 ? (
                              u.skills.map(s => (
                                  <span key={s.id} className="skill-badge">{s.name}</span>
                              ))
                          ) : (
                              <span style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', fontStyle: 'italic' }}>None</span>
                          )}
                        </div>
                      </td>
                      <td className="actions-cell">
                        <button className="btn-icon edit-btn" onClick={() => handleOpenModal(u)} title="Edit User">
                          <Pencil size={16} />
                        </button>
                        <button className="btn-icon vacation-btn" onClick={() => navigate(`/vacations?userId=${u.id}`, { state: { filterUserName: `${u.firstName} ${u.lastName}` } })} title="Manage Vacations">
                          <Plane size={16} />
                        </button>
                        <button className="btn-icon settlement-btn" onClick={() => navigate(`/settlements?userId=${u.id}`, { state: { filterUserName: `${u.firstName} ${u.lastName}` } })} title="View Schedule">
                          <CalendarDays size={16} />
                        </button>
                        <button className="btn-icon delete-btn" onClick={() => handleDelete(u.nationalId)} title="Delete User">
                          <Trash2 size={16} />
                        </button>
                      </td>
                    </tr>
                ))}
                </tbody>
              </table>
          )}
        </div>

        {isModalOpen && (
            <UserModal
                onClose={handleCloseModal}
                onSubmit={handleSubmit}
                user={selectedUser}
                departments={departments}
                skills={skills}
            />
        )}
      </div>
  );
};

export default Users;