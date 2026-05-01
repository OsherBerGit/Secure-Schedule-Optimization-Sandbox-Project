import React from "react";
import { Search } from "lucide-react";
import type { Department, Skill } from "../../../types";

interface UserFiltersProps {
    search: string;
    onSearchChange: (val: string) => void;
    role: string;
    onRoleChange: (val: string) => void;
    department: string;
    onDepartmentChange: (val: string) => void;
    skill: string;
    onSkillChange: (val: string) => void;
    departments: Department[];
    skills: Skill[];
}

const UserFilters: React.FC<UserFiltersProps> = ({
    search,
    onSearchChange,
    role,
    onRoleChange,
    department,
    onDepartmentChange,
    skill,
    onSkillChange,
    departments,
    skills
}) => {
    return (
        <div className="filters-container">
            <div className="search-wrapper flex-1">
                <Search className="search-icon" size={18} />
                <input
                    type="text"
                    className="modern-input search-input"
                    placeholder="Search by name or ID..."
                    value={search}
                    onChange={e => onSearchChange(e.target.value)}
                />
            </div>
            <select className="modern-input width-150" value={role} onChange={e => onRoleChange(e.target.value)}>
                <option value="">All Roles</option>
                <option value="ADMIN">Admin</option>
                <option value="MANAGER">Manager</option>
                <option value="WORKER">Worker</option>
            </select>
            <select className="modern-input width-150" value={department} onChange={e => onDepartmentChange(e.target.value)}>
                <option value="">All Departments</option>
                {departments.map(d => (
                    <option key={d.id} value={d.name}>
                        {d.name}
                    </option>
                ))}
            </select>
            <select className="modern-input width-150" value={skill} onChange={e => onSkillChange(e.target.value)}>
                <option value="">All Skills</option>
                {skills.map(s => (
                    <option key={s.id} value={s.name}>
                        {s.name}
                    </option>
                ))}
            </select>
        </div>
    );
};

export default UserFilters;
