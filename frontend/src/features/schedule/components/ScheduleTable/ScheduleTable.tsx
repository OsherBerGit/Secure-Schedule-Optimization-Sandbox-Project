import type { Task, User } from "../../../../types";
import { Calendar, Clock } from "lucide-react";
import "./ScheduleTable.css";

interface ScheduleTableProps {
    tasks: Task[];
    users: User[];
    assignmentMap: Map<number, number | null>;
}

const formatTaskDate = (timestamp?: number | string | null): string => {
    if (!timestamp) return "-";
    return new Date(timestamp).toLocaleDateString();
};

const formatTaskTime = (timestamp?: number | string | null): string => {
    if (!timestamp) return "-";
    return new Date(timestamp).toLocaleTimeString([], {
        hour: "2-digit",
        minute: "2-digit"
    });
};

const ScheduleTable = ({ tasks, users, assignmentMap }: ScheduleTableProps) => {
    return (
        <div className="st-fixed-container">
            <table className="st-table">
                <thead>
                    <tr>
                        <th className="st-th">Task Details</th>
                        <th className="st-th">Assigned User</th>
                        <th className="st-th">Scheduled Start</th>
                        <th className="st-th">Deadline</th>
                    </tr>
                </thead>
                <tbody>
                    {tasks.map(task => {
                        const userId = assignmentMap.get(task.id);
                        const assignedUser = users.find(userItem => userItem.id === userId);

                        return (
                            <tr key={task.id} className="st-tr">
                                <td className="st-td">
                                    <div className="st-task-info">
                                        <span className="st-task-title">{task.title}</span>
                                        <span className="st-task-id">ID: #{task.id}</span>
                                    </div>
                                </td>
                                <td className="st-td">
                                    <span className="st-user-name-plain">
                                        {assignedUser ? `${assignedUser.firstName} ${assignedUser.lastName}` : "Unassigned"}
                                    </span>
                                </td>
                                <td className="st-td">
                                    <div className="st-time-box">
                                        <Calendar size={16} />
                                        <span>{formatTaskDate(task.startTime)}</span>
                                    </div>
                                </td>
                                <td className="st-td">
                                    <div className="st-time-box">
                                        <Clock size={16} />
                                        <span>{formatTaskTime(task.deadline)}</span>
                                    </div>
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
};

export default ScheduleTable;
