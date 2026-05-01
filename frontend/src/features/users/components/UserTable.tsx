import React from "react";
import { Pencil, Trash2, Plane, CalendarDays } from "lucide-react";
import type { User } from "../../../types";

interface UserTableProps {
    users: User[];
    canEdit: boolean;
    canDelete: boolean;
    onEdit: (user: User) => void;
    onDelete: (nationalId: string) => void;
    onNavigate: (path: string, state: any) => void;
}

const UserTable: React.FC<UserTableProps> = ({ users, canEdit, canDelete, onEdit, onDelete, onNavigate }) => {
    return (
        <div className="table-container">
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
                    {users.map(user => (
                        <tr key={user.nationalId}>
                            <td>{user.nationalId}</td>
                            <td className="font-medium text-primary-dark">
                                {user.firstName} {user.lastName}
                            </td>
                            <td>{user.email}</td>
                            <td>{user.phoneNumber}</td>
                            <td>
                                <span className={`role-badge role-${user.role.toLowerCase()}`}>{user.role}</span>
                            </td>
                            <td>
                                <span className="department-badge">{user.departmentName || "General"}</span>
                            </td>
                            <td>
                                <div className="skills-container-table">
                                    {user.skills?.length ? (
                                        user.skills.map(s => (
                                            <span key={s.id} className="skill-badge">
                                                {s.name}
                                            </span>
                                        ))
                                    ) : (
                                        <span className="text-italic text-small">None</span>
                                    )}
                                </div>
                            </td>
                            <td className="actions-cell">
                                {canEdit && (
                                    <button className="btn-icon edit-btn" onClick={() => onEdit(user)} title="Edit User">
                                        <Pencil size={16} />
                                    </button>
                                )}
                                <button
                                    className="btn-icon vacation-btn"
                                    onClick={() =>
                                        onNavigate(`/vacations?userId=${user.id}`, {
                                            filterUserName: `${user.firstName} ${user.lastName}`
                                        })
                                    }
                                    title="Vacations">
                                    <Plane size={16} />
                                </button>
                                <button
                                    className="btn-icon settlement-btn"
                                    onClick={() =>
                                        onNavigate(`/settlements?userId=${user.id}`, {
                                            filterUserName: `${user.firstName} ${user.lastName}`
                                        })
                                    }
                                    title="Schedule">
                                    <CalendarDays size={16} />
                                </button>
                                {canDelete && (
                                    <button className="btn-icon delete-btn" onClick={() => onDelete(user.nationalId)} title="Delete User">
                                        <Trash2 size={16} />
                                    </button>
                                )}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
};

export default UserTable;
