import React from "react";
import { LayoutDashboard, Database } from "lucide-react";
import FitnessChart from "./FitnessChart/FitnessChart";
import type { ScheduleResult } from "../../../types";
import { isMemeticResult } from "../../../types";

interface ScheduleDraftSummaryProps {
    result: ScheduleResult;
    selectedConfigId: number | null;
    isSaving: boolean;
    onSave: () => void;
}

const ScheduleDraftSummary: React.FC<ScheduleDraftSummaryProps> = ({ result, selectedConfigId, isSaving, onSave }) => {
    return (
        <div className="result-layout-vertical">
            <div className="draft-preview-card">
                <div className="card-header">
                    <div className="header-title-row">
                        <LayoutDashboard size={20} />
                        <h3>Draft Preview: {result.strategyUsed}</h3>
                    </div>
                    {result.strategyUsed === "MEMETIC" && <span className="config-tag">Config ID: {selectedConfigId || "Default"}</span>}
                </div>
                <div className="stats-grid">
                    <div className="stat-box">
                        <span className="stat-label">Total</span>
                        <span className="stat-number">{result.totalTasks}</span>
                    </div>
                    <div className="stat-box success">
                        <span className="stat-label">Assigned</span>
                        <span className="stat-number">{result.assignedTasks}</span>
                    </div>
                    <div className="stat-box warning">
                        <span className="stat-label">Unassigned</span>
                        <span className="stat-number">{result.unassignedTasks}</span>
                    </div>
                </div>
                <div className="draft-actions">
                    <p>Review the assignments below before saving to calendars.</p>
                    <button className="btn-save-schedule" onClick={onSave} disabled={isSaving}>
                        <Database size={18} /> {isSaving ? "Saving..." : "Approve & Save Schedule"}
                    </button>
                </div>
            </div>
            {isMemeticResult(result) && result.fitnessHistory && result.fitnessHistory.length > 0 && (
                <div className="fitness-section">
                    <div className="fitness-header">
                        <LayoutDashboard size={18} />
                        <span>Convergence Analysis (Fitness Score)</span>
                    </div>
                    <FitnessChart data={result.fitnessHistory} />
                </div>
            )}
        </div>
    );
};

export default ScheduleDraftSummary;
