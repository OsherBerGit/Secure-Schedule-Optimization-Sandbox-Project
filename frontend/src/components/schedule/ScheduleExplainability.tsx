import type { UnscheduledTaskResult } from "../../types";
import { formatReason } from "../../utils/scheduleUtils";
import { AlertCircle, ChevronRight } from "lucide-react";
import "./ScheduleExplainability.css";

const ScheduleExplainability = ({
    failures,
}: {
    failures: UnscheduledTaskResult[];
}) => {
    if (!failures?.length) return null;

    return (
        <div className="ex-container">
            <div className="ex-header">
                <div className="ex-header-title-row">
                    <AlertCircle className="header-icon-purple" size={20} />
                    <h3>Optimization Insights</h3>
                </div>
                <p className="ex-subtitle">
                    Resource conflicts or constraints prevented these tasks from
                    being scheduled.
                </p>
            </div>
            <div className="ex-table-wrapper">
                <table className="ex-table">
                    <thead>
                        <tr>
                            <th>Target Task</th>
                            <th>Reason for Exclusion</th>
                        </tr>
                    </thead>
                    <tbody>
                        {failures.map((u) => (
                            <tr key={u.taskId} className="ex-tr">
                                <td className="ex-td">
                                    <div className="ex-task-box">
                                        <span className="ex-task-name">
                                            {u.taskName}
                                        </span>
                                        <span className="ex-task-id">
                                            ID: #{u.taskId}
                                        </span>
                                    </div>
                                </td>
                                <td className="ex-td">
                                    <div className="ex-reason-box">
                                        <ChevronRight
                                            size={14}
                                            className="ex-chevron"
                                        />
                                        <span className="ex-reason-text">
                                            {formatReason(u.reason)}
                                        </span>
                                    </div>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default ScheduleExplainability;