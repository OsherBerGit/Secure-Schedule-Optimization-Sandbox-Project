import React from "react";
import { Trash2 } from "lucide-react";
import type { TaskConstraint } from "../../../types";

interface TaskConstraintTableProps {
    constraints: TaskConstraint[];
    canManage: boolean;
    onDelete: (id: number) => void;
}

const TaskConstraintTable: React.FC<TaskConstraintTableProps> = ({ constraints, canManage, onDelete }) => {
    return (
        <div className="table-container">
            <table className="modern-table constraints-table">
                <thead>
                    <tr>
                        <th className="th-id">ID</th>
                        <th>Predecessor Task</th>
                        <th>Successor Task</th>
                        <th>Type</th>
                        <th>Lag (min)</th>
                        {canManage && <th className="th-actions">Actions</th>}
                    </tr>
                </thead>
                <tbody>
                    {constraints.map(constraint => (
                        <tr key={constraint.id}>
                            <td className="id-cell">{constraint.id}</td>
                            <td className="task-title-cell">{constraint.predecessorTaskTitle ?? `#${constraint.predecessorTaskId}`}</td>
                            <td className="task-title-cell">{constraint.successorTaskTitle ?? `#${constraint.successorTaskId}`}</td>
                            <td>
                                <span className="constraint-badge">{constraint.constraintTypeName ?? `#${constraint.constraintTypeId}`}</span>
                            </td>
                            <td>
                                {constraint.lagMinutes ? <span className="lag-badge">{constraint.lagMinutes}m</span> : <span className="lag-empty">-</span>}
                            </td>
                            {canManage && (
                                <td className="actions-cell">
                                    <div className="actions-container">
                                        <button className="btn-icon delete-btn" onClick={() => onDelete(constraint.id)} title="Delete">
                                            <Trash2 size={16} />
                                        </button>
                                    </div>
                                </td>
                            )}
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
};

export default TaskConstraintTable;
