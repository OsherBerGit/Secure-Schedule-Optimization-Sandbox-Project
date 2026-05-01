import React from "react";
import { CheckCircle, Trash2 } from "lucide-react";
import type { Settlement } from "../../../types";

interface SettlementTableProps {
    settlements: Settlement[];
    isAdmin: boolean;
    onComplete: (id: number) => void;
    onDelete: (id: number) => void;
}

const formatDate = (value: string | number[] | null | undefined): string => {
    if (!value) return "-";
    if (Array.isArray(value)) {
        const [y, mo, d, h = 0, m = 0] = value;
        return new Date(y, mo - 1, d, h, m).toLocaleString();
    }
    const date = new Date(value);
    return isNaN(date.getTime()) ? String(value) : date.toLocaleString();
};

const SettlementTable: React.FC<SettlementTableProps> = ({ settlements, isAdmin, onComplete, onDelete }) => {
    return (
        <div className="table-container">
            <table className="modern-table settlements-table">
                <thead>
                    <tr>
                        <th>User</th>
                        <th>Task</th>
                        <th>Status</th>
                        <th>Settlement Date</th>
                        <th>Completion Date</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    {settlements.map(settlement => {
                        const isCompleted = settlement.statusName === "COMPLETED";
                        return (
                            <tr key={settlement.id}>
                                <td className="font-medium">{settlement.userName}</td>
                                <td className="text-secondary">{settlement.taskTitle}</td>
                                <td>
                                    <span className={`status-badge status-${settlement.statusName?.toLowerCase()}`}>{settlement.statusName}</span>
                                </td>
                                <td>{formatDate(settlement.settlementDate)}</td>
                                <td>{settlement.completionDate ? formatDate(settlement.completionDate) : <span className="duration-badge">Pending</span>}</td>
                                <td className="actions-cell">
                                    {!isCompleted && (
                                        <button className="btn-icon approve-btn btn-with-text" onClick={() => onComplete(settlement.id)}>
                                            <CheckCircle size={16} />
                                            <span>Done</span>
                                        </button>
                                    )}
                                    {isAdmin && (
                                        <button className="btn-icon delete-btn" onClick={() => onDelete(settlement.id)}>
                                            <Trash2 size={16} />
                                        </button>
                                    )}
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
};

export default SettlementTable;
