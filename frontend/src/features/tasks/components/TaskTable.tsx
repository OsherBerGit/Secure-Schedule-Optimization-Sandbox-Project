import React from "react";
import { Pencil, ShieldAlert, Trash2 } from "lucide-react";
import type { Task } from "../../../types";

interface TaskTableProps {
    tasks: Task[];
    canManage: boolean;
    onEdit: (task: Task) => void;
    onDelete: (id: number) => void;
    onViewConstraints: (id: number) => void;
}

const TaskTable: React.FC<TaskTableProps> = ({ tasks, canManage, onEdit, onDelete, onViewConstraints }) => {
    return (
        <div className="table-container">
            <table className="modern-table tasks-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Title</th>
                        <th>Priority</th>
                        <th>Department</th>
                        <th>Skills</th>
                        <th>Status</th>
                        {canManage && <th>Actions</th>}
                    </tr>
                </thead>
                <tbody>
                    {tasks.map(task => {
                        const status = task.taskStatusName || "OPEN";
                        const canEditTask = status === "OPEN" || status === "LOCKED";

                        return (
                            <tr key={task.id}>
                                <td>{task.id}</td>
                                <td className="font-medium text-primary-dark">{task.title}</td>
                                <td>
                                    <span className={`priority-badge priority-${task.priorityName?.toLowerCase()}`}>{task.priorityName}</span>
                                </td>
                                <td>
                                    <span className="department-badge">{task.departmentName || "General"}</span>
                                </td>
                                <td>
                                    <div className="skills-container-table">
                                        {task.requiredSkills?.length ? (
                                            task.requiredSkills.map(s => (
                                                <span key={s.id} className="skill-badge">
                                                    {s.name}
                                                </span>
                                            ))
                                        ) : (
                                            <span className="text-italic text-small">None</span>
                                        )}
                                    </div>
                                </td>
                                <td>
                                    <span className={`status-badge status-${status.toLowerCase()}`}>{status}</span>
                                </td>
                                {canManage && (
                                    <td className="actions-cell">
                                        <button
                                            className={`btn-icon edit-btn ${!canEditTask ? "disabled-btn" : ""}`}
                                            onClick={() => canEditTask && onEdit(task)}
                                            disabled={!canEditTask}
                                            title={canEditTask ? "Edit Task" : "Locked"}>
                                            <Pencil size={16} />
                                        </button>
                                        <button className="btn-icon constraint-btn" onClick={() => onViewConstraints(task.id!)} title="Task Constraints">
                                            <ShieldAlert size={16} />
                                        </button>
                                        <button className="btn-icon delete-btn" onClick={() => onDelete(task.id!)} title="Delete Task">
                                            <Trash2 size={16} />
                                        </button>
                                    </td>
                                )}
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
};

export default TaskTable;
