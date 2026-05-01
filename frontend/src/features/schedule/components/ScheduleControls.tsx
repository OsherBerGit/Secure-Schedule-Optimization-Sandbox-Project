import React from "react";
import { Calendar, LayoutGrid, List, Settings, Play, Loader2 } from "lucide-react";
import type { Department, ScheduleStrategy } from "../../../types";

interface ScheduleControlsProps {
    scheduledCount: number;
    totalCount: number;
    viewMode: "gantt" | "table";
    setViewMode: (mode: "gantt" | "table") => void;
    strategy: ScheduleStrategy;
    setStrategy: (s: ScheduleStrategy) => void;
    selectedDepartmentId: number | null;
    setSelectedDepartmentId: (id: number | null) => void;
    departments: Department[];
    isGenerating: boolean;
    onRun: () => void;
    onConfigOpen: () => void;
    canManage: boolean;
}

const ScheduleControls: React.FC<ScheduleControlsProps> = ({
    scheduledCount,
    totalCount,
    viewMode,
    setViewMode,
    strategy,
    setStrategy,
    selectedDepartmentId,
    setSelectedDepartmentId,
    departments,
    isGenerating,
    onRun,
    onConfigOpen,
    canManage
}) => {
    return (
        <div className="schedule-header">
            <div className="header-left">
                <div className="header-icon-wrapper">
                    <Calendar className="header-icon-purple" size={32} />
                </div>
                <div className="header-title-wrapper">
                    <h1>Schedule Management</h1>
                    <p className="header-subtitle">
                        {scheduledCount} of {totalCount} tasks scheduled
                    </p>
                </div>
            </div>
            <div className="header-right">
                <div className="view-switcher">
                    <button className={viewMode === "gantt" ? "active" : ""} onClick={() => setViewMode("gantt")}>
                        <LayoutGrid size={16} /> Gantt
                    </button>
                    <button className={viewMode === "table" ? "active" : ""} onClick={() => setViewMode("table")}>
                        <List size={16} /> Table
                    </button>
                </div>
                {canManage && (
                    <div className="algo-controls">
                        <select
                            className="modern-select"
                            value={selectedDepartmentId ?? ""}
                            onChange={e => setSelectedDepartmentId(e.target.value ? Number(e.target.value) : null)}>
                            <option value="">Global (All Depts)</option>
                            {departments.map(dept => (
                                <option key={dept.id} value={dept.id}>
                                    {dept.name}
                                </option>
                            ))}
                        </select>
                        <select className="modern-select" value={strategy} onChange={e => setStrategy(e.target.value as ScheduleStrategy)}>
                            <option value="GREEDY">Greedy</option>
                            <option value="ROUND_ROBIN">Round Robin</option>
                            <option value="CONSTRAINT_PROGRAMMING">Constraint Programming</option>
                            <option value="MEMETIC">Memetic</option>
                        </select>
                        <button className="icon-btn-settings" onClick={onConfigOpen} disabled={strategy !== "MEMETIC"}>
                            <Settings size={20} />
                        </button>
                        <button className="btn-generate-main" onClick={onRun} disabled={isGenerating}>
                            {isGenerating ? <Loader2 className="spinner" size={18} /> : <Play size={18} fill="currentColor" />}
                            <span>{isGenerating ? "Running..." : "Generate"}</span>
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
};

export default ScheduleControls;
