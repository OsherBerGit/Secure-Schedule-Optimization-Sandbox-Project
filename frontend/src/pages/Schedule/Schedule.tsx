import React, { useState, useEffect, useMemo } from "react";
import { Sparkles, Loader2, Info, AlertCircle, CheckCircle2 } from "lucide-react";
import { usePermissions } from "../../hooks/usePermissions";
import { useScheduleData } from "../../features/schedule/hooks/useScheduleData";
import { useScheduleAlgorithm } from "../../features/schedule/hooks/useScheduleAlgorithm";
import { useSchedulingConfig } from "../../features/schedule/hooks/useSchedulingConfig";
import SchedulingConfigurationModal from "../../features/schedule/components/SchedulingConfigurationModal/SchedulingConfigurationModal";
import ScheduleGantt from "../../features/schedule/components/ScheduleGantt/ScheduleGantt";
import ScheduleTable from "../../features/schedule/components/ScheduleTable/ScheduleTable";
import ScheduleExplainability from "../../features/schedule/components/ScheduleExplainability/ScheduleExplainability";
import ScheduleControls from "../../features/schedule/components/ScheduleControls";
import ScheduleDraftSummary from "../../features/schedule/components/ScheduleDraftSummary";
import type { ScheduleStrategy } from "../../types";
import "./Schedule.css";

const STRATEGY_DESCRIPTIONS: Record<ScheduleStrategy, string> = {
    GREEDY: "A fast, straightforward approach that makes the optimal choice at each step, prioritizing immediate constraints without looking ahead.",
    ROUND_ROBIN: "Assigns tasks to resources in a circular order, ensuring an equal distribution of workload without complex constraint evaluation.",
    CONSTRAINT_PROGRAMMING:
        "A rigorous mathematical approach that explores the solution space to find a mathematically valid schedule satisfying all hard requirements.",
    MEMETIC:
        "A powerful hybrid approach combining global evolutionary search with local optimization. It iteratively improves assignments to maximize resource utilization and meet strict deadlines."
};

const Schedule: React.FC = () => {
    const { canEdit: canManage } = usePermissions();
    const { tasks, users, departments, settlements, error: dataError } = useScheduleData();
    const { scheduleResult, isGenerating, isSaving, error: algoError, validationErrors, successMsg, runAlgorithm, saveSchedule } = useScheduleAlgorithm();
    const {
        configs,
        isConfigModalOpen,
        selectedConfigId,
        isLoading: isConfigLoading,
        error: configError,
        openConfigModal,
        closeConfigModal,
        selectConfig,
        createConfig,
        fetchConfigs
    } = useSchedulingConfig();

    const [viewMode, setViewMode] = useState<"gantt" | "table">("gantt");
    const [strategy, setStrategy] = useState<ScheduleStrategy>("GREEDY");
    const [selectedDepartmentId, setSelectedDepartmentId] = useState<number | null>(null);

    useEffect(() => {
        if (isConfigModalOpen) fetchConfigs();
    }, [isConfigModalOpen, fetchConfigs]);

    const mergedTasks = useMemo(() => {
        return tasks.map(task => {
            const assignment = scheduleResult?.assignments?.find(a => a.taskId === task.id);
            return assignment ? { ...task, startTime: assignment.scheduledStart ?? task.startTime } : task;
        });
    }, [tasks, scheduleResult]);

    const assignmentMap = useMemo(() => {
        const map = new Map<number, number | null>();
        settlements?.forEach(s => s.taskId && map.set(s.taskId, s.userId));
        scheduleResult?.assignments?.forEach(a => map.set(a.taskId, a.assignedUserId));
        return map;
    }, [settlements, scheduleResult]);

    const displayTasks = useMemo(() => {
        const deptName = selectedDepartmentId ? departments.find(d => d.id === selectedDepartmentId)?.name : null;
        return deptName ? mergedTasks.filter(t => t.departmentName === deptName) : mergedTasks;
    }, [mergedTasks, selectedDepartmentId, departments]);

    const scheduledTasks = useMemo(() => displayTasks.filter(t => t.startTime || t.taskStatusName === "SCHEDULED"), [displayTasks]);

    return (
        <div className="schedule-page">
            <ScheduleControls
                scheduledCount={scheduledTasks.length}
                totalCount={tasks.length}
                viewMode={viewMode}
                setViewMode={setViewMode}
                strategy={strategy}
                setStrategy={setStrategy}
                selectedDepartmentId={selectedDepartmentId}
                setSelectedDepartmentId={setSelectedDepartmentId}
                departments={departments}
                isGenerating={isGenerating}
                onRun={() => runAlgorithm(strategy, selectedDepartmentId, selectedConfigId)}
                onConfigOpen={openConfigModal}
                canManage={canManage}
            />

            {(algoError || dataError || configError) && (
                <div className="error-banner banner-spacing">
                    <AlertCircle size={24} className="flex-shrink-0" />
                    <div>
                        <span className="font-semibold block">{algoError || dataError || configError}</span>
                        {validationErrors?.length > 0 && (
                            <ul className="validation-list top-margin-small">
                                {validationErrors.map((err, idx) => (
                                    <li key={idx}>
                                        <strong>Violation:</strong> {err}
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                </div>
            )}

            {successMsg && (
                <div className="success-banner banner-spacing">
                    <CheckCircle2 size={20} />
                    <span>{successMsg}</span>
                </div>
            )}

            {isGenerating && (
                <div className="generating-status-card large">
                    <Loader2 className="spinner" size={48} />
                    <div className="generating-info">
                        <h3>Optimizing Schedule...</h3>
                        <span>Running {strategy} algorithm to find the best possible plan.</span>
                    </div>
                </div>
            )}

            {!isGenerating && !scheduleResult && scheduledTasks.length === 0 && (
                <div className="empty-state-card">
                    <div className="empty-icon-wrapper">
                        <Sparkles size={40} />
                    </div>
                    <h2>No Schedule Generated</h2>
                    <p>Select a strategy and click Generate to start the optimization process.</p>
                    <div className="strategy-info-box">
                        <Info size={20} className="strategy-icon" />
                        <div>
                            <strong>Algorithm Info</strong>
                            <br />
                            <span>{STRATEGY_DESCRIPTIONS[strategy]}</span>
                        </div>
                    </div>
                </div>
            )}

            {scheduleResult && (
                <ScheduleDraftSummary
                    result={scheduleResult}
                    selectedConfigId={selectedConfigId}
                    isSaving={isSaving}
                    onSave={() => saveSchedule(mergedTasks)}
                />
            )}

            {(scheduledTasks.length > 0 || scheduleResult) && (
                <div className="schedule-content top-margin">
                    {viewMode === "gantt" ? (
                        <ScheduleGantt tasks={scheduledTasks} users={users} assignmentMap={assignmentMap} onTaskClick={() => {}} />
                    ) : (
                        <ScheduleTable tasks={scheduledTasks} users={users} assignmentMap={assignmentMap} />
                    )}
                </div>
            )}

            {scheduleResult?.unscheduledTasks && <ScheduleExplainability failures={scheduleResult.unscheduledTasks} />}

            {isConfigModalOpen && (
                <SchedulingConfigurationModal
                    configs={configs}
                    isLoading={isConfigLoading}
                    error={configError}
                    onClose={closeConfigModal}
                    initialConfigId={selectedConfigId}
                    onSelectConfig={selectConfig}
                    onCreateConfig={createConfig}
                />
            )}
        </div>
    );
};

export default Schedule;
