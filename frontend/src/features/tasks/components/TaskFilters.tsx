import React from "react";
import { Search } from "lucide-react";
import type { Status, Priority, Skill } from "../../../types";

interface TaskFiltersProps {
    search: string;
    onSearchChange: (val: string) => void;
    status: string;
    onStatusChange: (val: string) => void;
    priority: string;
    onPriorityChange: (val: string) => void;
    skill: string;
    onSkillChange: (val: string) => void;
    metadata: {
        statuses: Status[];
        priorities: Priority[];
        skills: Skill[];
    };
}

const TaskFilters: React.FC<TaskFiltersProps> = ({
    search,
    onSearchChange,
    status,
    onStatusChange,
    priority,
    onPriorityChange,
    skill,
    onSkillChange,
    metadata
}) => {
    return (
        <div className="filters-container">
            <div className="search-wrapper flex-1">
                <Search className="search-icon" size={18} />
                <input
                    type="text"
                    className="modern-input search-input"
                    placeholder="Search by title..."
                    value={search}
                    onChange={e => onSearchChange(e.target.value)}
                />
            </div>
            <select className="modern-input width-150" value={status} onChange={e => onStatusChange(e.target.value)}>
                <option value="">All Statuses</option>
                {metadata.statuses.map(s => (
                    <option key={s.id} value={s.name}>
                        {s.name}
                    </option>
                ))}
            </select>
            <select className="modern-input width-150" value={priority} onChange={e => onPriorityChange(e.target.value)}>
                <option value="">All Priorities</option>
                {metadata.priorities.map(p => (
                    <option key={p.id} value={p.name}>
                        {p.name}
                    </option>
                ))}
            </select>
            <select className="modern-input width-150" value={skill} onChange={e => onSkillChange(e.target.value)}>
                <option value="">All Skills</option>
                {metadata.skills.map(s => (
                    <option key={s.id} value={s.name}>
                        {s.name}
                    </option>
                ))}
            </select>
        </div>
    );
};

export default TaskFilters;
