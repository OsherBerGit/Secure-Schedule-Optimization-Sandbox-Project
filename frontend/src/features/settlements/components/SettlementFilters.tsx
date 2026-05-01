import React from "react";
import { Search } from "lucide-react";
import type { Status } from "../../../types";

interface SettlementFiltersProps {
    canEdit: boolean;
    filterUserName: string;
    setFilterUserName: (val: string) => void;
    filterTaskName: string;
    setFilterTaskName: (val: string) => void;
    filterStatus: string;
    setFilterStatus: (val: string) => void;
    statuses: Status[];
}

const SettlementFilters: React.FC<SettlementFiltersProps> = ({
    canEdit,
    filterUserName,
    setFilterUserName,
    filterTaskName,
    setFilterTaskName,
    filterStatus,
    setFilterStatus,
    statuses
}) => {
    return (
        <div className="filters-container">
            {canEdit ? (
                <div className="search-wrapper flex-1">
                    <Search className="search-icon" size={18} />
                    <input
                        type="text"
                        className="modern-input search-input"
                        placeholder="Search by user name..."
                        value={filterUserName}
                        onChange={e => setFilterUserName(e.target.value)}
                    />
                </div>
            ) : (
                <div className="modern-input disabled-display flex-1">Viewing your assigned tasks</div>
            )}

            <div className="search-wrapper flex-1">
                <Search className="search-icon" size={18} />
                <input
                    type="text"
                    className="modern-input search-input"
                    placeholder="Search by task name..."
                    value={filterTaskName}
                    onChange={e => setFilterTaskName(e.target.value)}
                />
            </div>

            <select className="modern-input status-select" value={filterStatus} onChange={e => setFilterStatus(e.target.value)}>
                <option value="">All Statuses</option>
                {statuses.map(status => (
                    <option key={status.id} value={status.name}>
                        {status.name}
                    </option>
                ))}
            </select>
        </div>
    );
};

export default SettlementFilters;
