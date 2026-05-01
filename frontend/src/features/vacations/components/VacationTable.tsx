import React from "react";
import { Calendar, Check, X, Pencil, Trash2 } from "lucide-react";
import type { Vacation } from "../../../types";

interface VacationTableProps {
    vacations: Vacation[];
    canManage: boolean;
    onApprove: (id: number) => void;
    onReject: (id: number) => void;
    onEdit: (v: Vacation) => void;
    onDelete: (id: number) => void;
}

const calculateDuration = (start: string, end: string) => {
    const s = new Date(start);
    const e = new Date(end);
    return Math.ceil((e.getTime() - s.getTime()) / (1000 * 60 * 60 * 24)) + 1;
};

const VacationTable: React.FC<VacationTableProps> = ({ vacations, canManage, onApprove, onReject, onEdit, onDelete }) => {
    return (
        <div className="table-container">
            <table className="modern-table vacations-table">
                <thead>
                    <tr>
                        <th>User</th>
                        <th>Start Date</th>
                        <th>End Date</th>
                        <th>Duration</th>
                        <th>Status</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    {vacations.map(v => {
                        const days = calculateDuration(v.startDate, v.endDate);
                        const status = v.statusName ?? "PENDING";

                        return (
                            <tr key={v.id}>
                                <td className="font-medium">{v.userName}</td>
                                <td>
                                    <div className="date-cell">
                                        <Calendar size={14} className="opacity-50" />
                                        {new Date(v.startDate).toLocaleDateString()}
                                    </div>
                                </td>
                                <td>
                                    <div className="date-cell">
                                        <Calendar size={14} className="opacity-50" />
                                        {new Date(v.endDate).toLocaleDateString()}
                                    </div>
                                </td>
                                <td>
                                    <span className="duration-badge">
                                        {days} {days === 1 ? "day" : "days"}
                                    </span>
                                </td>
                                <td>
                                    <span className={`status-badge status-${status.toLowerCase()}`}>{status}</span>
                                </td>
                                <td className="actions-cell">
                                    {canManage && status === "PENDING" && (
                                        <>
                                            <button className="btn-icon approve-btn" onClick={() => onApprove(v.id)} title="Approve">
                                                <Check size={18} />
                                            </button>
                                            <button className="btn-icon reject-btn" onClick={() => onReject(v.id)} title="Reject">
                                                <X size={18} />
                                            </button>
                                        </>
                                    )}
                                    {canManage && (
                                        <button className="btn-icon edit-btn" onClick={() => onEdit(v)} title="Edit">
                                            <Pencil size={16} />
                                        </button>
                                    )}
                                    {(canManage || status === "PENDING") && (
                                        <button className="btn-icon delete-btn" onClick={() => onDelete(v.id)} title="Delete">
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

export default VacationTable;
