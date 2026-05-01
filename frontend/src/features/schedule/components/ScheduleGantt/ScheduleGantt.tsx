import React, { useMemo } from "react";
import type { Task, User } from "../../../../types";
import { getPriorityColor } from "../../../../utils/scheduleUtils";
import { Users } from "lucide-react";
import "./ScheduleGantt.css";

interface ScheduleGanttProps {
    tasks: Task[];
    users: User[];
    assignmentMap: Map<number, number | null>;
    onTaskClick?: (task: Task) => void;
}

const calculateTaskEndDate = (task: Task): Date => {
    if (task.deadline) return new Date(task.deadline);
    const start = new Date(task.startTime!).getTime();
    return new Date(start + (task.durationHours || 1) * 3600000);
};

const getTimelineBounds = (scheduledTasks: Task[]) => {
    const allDates = scheduledTasks.flatMap(task => [new Date(task.startTime!), calculateTaskEndDate(task)]);

    const min = new Date(Math.min(...allDates.map(d => d.getTime())));
    min.setHours(0, 0, 0, 0);

    const max = new Date(Math.max(...allDates.map(d => d.getTime())));
    max.setHours(23, 59, 59, 999);

    return { minDate: min, maxDate: max };
};

const generateTimelineTicks = (minDate: Date, maxDate: Date) => {
    const ticks = [];
    const curr = new Date(minDate);
    while (curr <= maxDate) {
        ticks.push(new Date(curr));
        curr.setDate(curr.getDate() + 1);
    }
    return ticks;
};

const calculateBarDimensions = (task: Task, minDate: Date, totalMs: number) => {
    const startTs = new Date(task.startTime!).getTime();
    const endTs = calculateTaskEndDate(task).getTime();

    const leftPercent = ((startTs - minDate.getTime()) / totalMs) * 100;
    const widthPercent = Math.max(((endTs - startTs) / totalMs) * 100, 2);

    return { leftPercent, widthPercent };
};

const ScheduleGantt: React.FC<ScheduleGanttProps> = ({ tasks, users, assignmentMap, onTaskClick }) => {
    const { minDate, totalMs, userSchedules, timelineTicks } = useMemo(() => {
        const scheduledTasks = tasks.filter(task => task.startTime);

        if (scheduledTasks.length === 0) {
            return {
                minDate: new Date(),
                maxDate: new Date(),
                totalMs: 1,
                userSchedules: [],
                timelineTicks: []
            };
        }

        const { minDate, maxDate } = getTimelineBounds(scheduledTasks);
        const ticks = generateTimelineTicks(minDate, maxDate);
        const total = maxDate.getTime() - minDate.getTime() || 1;

        const schedules = users
            .map(user => ({
                user,
                userTasks: scheduledTasks.filter(task => assignmentMap.get(task.id) === user.id)
            }))
            .filter(schedule => schedule.userTasks.length > 0);

        return {
            minDate,
            maxDate,
            totalMs: total,
            userSchedules: schedules,
            timelineTicks: ticks
        };
    }, [tasks, users, assignmentMap]);

    if (userSchedules.length === 0) return null;

    return (
        <div className="gn-container">
            <div className="gn-toolbar">
                <div className="gn-legend">
                    <div className="gn-legend-item">
                        <span className="gn-dot" style={{ backgroundColor: "#ef4444" }}></span> High
                    </div>
                    <div className="gn-legend-item">
                        <span className="gn-dot" style={{ backgroundColor: "#f59e0b" }}></span> Medium
                    </div>
                    <div className="gn-legend-item">
                        <span className="gn-dot" style={{ backgroundColor: "#10b981" }}></span> Low
                    </div>
                </div>
            </div>

            <div className="gn-scroll-area">
                <div
                    className="gn-canvas"
                    style={{
                        minWidth: Math.max(timelineTicks.length * 120, 1000),
                        position: "relative"
                    }}>
                    <div
                        className="gn-header-row"
                        style={{
                            display: "flex",
                            position: "sticky",
                            top: 0,
                            zIndex: 30
                        }}>
                        <div className="gn-user-label-header">
                            <Users size={14} /> Users
                        </div>
                        <div className="gn-timeline-header" style={{ display: "flex", flex: 1 }}>
                            {timelineTicks.map((date, i) => (
                                <div key={i} className="gn-tick-cell">
                                    <div className="gn-tick-day">{date.toLocaleDateString("en-US", { weekday: "short" })}</div>
                                    <div className="gn-tick-date">
                                        {date.getDate()}/{date.getMonth() + 1}
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>

                    <div className="gn-body">
                        {userSchedules.map(userSchedule => (
                            <div
                                key={userSchedule.user.id}
                                className="gn-row"
                                style={{
                                    display: "flex",
                                    position: "relative",
                                    borderBottom: "1px solid #f1f5f9",
                                    minHeight: "60px"
                                }}>
                                <div className="gn-user-side-info">
                                    <div className="gn-user-name-main">
                                        {userSchedule.user.firstName} {userSchedule.user.lastName}
                                    </div>
                                    <div className="gn-user-task-count">{userSchedule.userTasks.length} tasks</div>
                                </div>

                                <div
                                    className="gn-bars-container"
                                    style={{
                                        position: "relative",
                                        flex: 1,
                                        display: "flex"
                                    }}>
                                    {timelineTicks.map((_, i) => (
                                        <div
                                            key={i}
                                            className="gn-grid-line"
                                            style={{
                                                flex: 1,
                                                borderRight: "1px solid #f8fafc"
                                            }}
                                        />
                                    ))}

                                    {userSchedule.userTasks.map(task => {
                                        const { leftPercent, widthPercent } = calculateBarDimensions(task, minDate, totalMs);

                                        return (
                                            <div
                                                key={task.id}
                                                className="gn-task-bar"
                                                style={{
                                                    position: "absolute",
                                                    left: `${leftPercent}%`,
                                                    width: `${widthPercent}%`,
                                                    backgroundColor: getPriorityColor(task.priorityName),
                                                    top: "50%",
                                                    transform: "translateY(-50%)",
                                                    zIndex: 10,
                                                    height: "32px"
                                                }}
                                                onClick={() => onTaskClick?.(task)}>
                                                <span className="gn-bar-label">{task.title}</span>
                                            </div>
                                        );
                                    })}
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ScheduleGantt;
