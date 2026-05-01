import React from "react";
import { Search } from "lucide-react";
import type { Department, ConstraintType } from "../../../types";

interface TaskConstraintFiltersProps {
    search: string;
    onSearchChange: (val: string) => void;
    filterDepartment: string;
    onDepartmentChange: (val: string) => void;
    filterType: string;
    onTypeChange: (val: string) => void;
    departments: Department[];
    constraintTypes: ConstraintType[];
    canManage: boolean;
}

const TaskConstraintFilters: React.FC<TaskConstraintFiltersProps> = ({
    search,
    onSearchChange,
    filterDepartment,
    onDepartmentChange,
    filterType,
    onTypeChange,
    departments,
    constraintTypes,
    canManage
}) => {
    return (
        <div className="filters-container">
            <div className="search-wrapper flex-1">
                <Search className="search-icon" size={18} />
                <input
                    type="text"
                    className="modern-input search-input"
                    placeholder="Search tasks or type..."
                    value={search}
                    onChange={e => onSearchChange(e.target.value)}
                />
            </div>
            {canManage && (
                <select className="modern-input width-180" value={filterDepartment} onChange={e => onDepartmentChange(e.target.value)}>
                    <option value="">All Departments</option>
                    {departments.map(dept => (
                        <option key={dept.id} value={dept.name}>
                            {dept.name}
                        </option>
                    ))}
                </select>
            )}
            <select className="modern-input width-180" value={filterType} onChange={e => onTypeChange(e.target.value)}>
                <option value="">All Types</option>
                {constraintTypes.map(type => (
                    <option key={type.id} value={type.name}>
                        {type.name}
                    </option>
                ))}
            </select>
        </div>
    );
};

export default TaskConstraintFilters;
