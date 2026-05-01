import React from "react";
import { Search } from "lucide-react";

interface VacationFiltersProps {
    canManage: boolean;
    filterUser: string;
    onUserChange: (val: string) => void;
    filterDept: string;
    onDeptChange: (val: string) => void;
    departments: string[];
}

const VacationFilters: React.FC<VacationFiltersProps> = ({ canManage, filterUser, onUserChange, filterDept, onDeptChange, departments }) => {
    if (!canManage) {
        return (
            <div className="filters-container">
                <div className="modern-input disabled-display">Viewing your requests</div>
            </div>
        );
    }

    return (
        <div className="filters-container">
            <div className="search-wrapper flex-1">
                <Search className="search-icon" size={18} />
                <input
                    type="text"
                    className="modern-input search-input"
                    placeholder="Search by user name..."
                    value={filterUser}
                    onChange={e => onUserChange(e.target.value)}
                />
            </div>

            <select className="modern-input width-200" value={filterDept} onChange={e => onDeptChange(e.target.value)}>
                <option value="">All Departments</option>
                {departments.map(dept => (
                    <option key={dept} value={dept}>
                        {dept}
                    </option>
                ))}
            </select>
        </div>
    );
};

export default VacationFilters;
