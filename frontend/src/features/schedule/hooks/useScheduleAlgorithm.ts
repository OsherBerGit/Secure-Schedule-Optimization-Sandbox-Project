import { useState, useCallback } from "react";
import type { Task, ScheduleStrategy, ScheduleResult, SaveTaskAssignment } from "../../../types";
import { scheduleApi } from "../../../api";
import { isAxiosError } from "axios";

interface AlgorithmState {
    result: ScheduleResult | null;
    isGenerating: boolean;
    isSaving: boolean;
    error: string | null;
    validationErrors: string[];
    successMsg: string | null;
}

export const useScheduleAlgorithm = () => {
    const [state, setState] = useState<AlgorithmState>({
        result: null,
        isGenerating: false,
        isSaving: false,
        error: null,
        validationErrors: [],
        successMsg: null
    });

    const clearMessages = useCallback(() => {
        setState(prev => ({
            ...prev,
            error: null,
            successMsg: null,
            validationErrors: []
        }));
    }, []);

    const runAlgorithm = async (strategy: ScheduleStrategy, departmentId: number | null, configId: number | null) => {
        setState(prev => ({
            ...prev,
            isGenerating: true,
            error: null,
            successMsg: null,
            validationErrors: [],
            result: null
        }));

        try {
            const res =
                strategy === "MEMETIC" && configId
                    ? await scheduleApi.runWithConfig(strategy, configId, departmentId)
                    : await scheduleApi.run(strategy, departmentId);

            const { assignedTasks, unassignedTasks, strategyUsed } = res.data;

            setState(prev => ({
                ...prev,
                isGenerating: false,
                result: res.data,
                successMsg: `Draft generated using ${strategyUsed} - ${assignedTasks} assigned, ${unassignedTasks} unassigned. Review below and click "Approve & Save" to persist.`
            }));

            return res.data;
        } catch (err: unknown) {
            const msg = isAxiosError(err) ? err.response?.data?.message || err.message : "Failed to generate schedule";

            setState(prev => ({ ...prev, isGenerating: false, error: msg }));
            throw new Error(msg);
        }
    };

    const saveSchedule = async (tasks: Task[]) => {
        if (!state.result) return;

        setState(prev => ({
            ...prev,
            isSaving: true,
            error: null,
            successMsg: null,
            validationErrors: []
        }));

        try {
            const assignments: SaveTaskAssignment[] = state.result.assignments.map(a => {
                const originalTask = tasks.find(t => t.id === a.taskId);

                if (!originalTask) {
                    throw new Error(`Task ID ${a.taskId} not found in local state.`);
                }

                if (originalTask.version == null) {
                    throw new Error(`Integrity Error: Task ID ${a.taskId} is missing a version number required for saving.`);
                }

                return {
                    taskId: a.taskId,
                    assignedUserId: a.assignedUserId ?? null,
                    scheduledStart: a.scheduledStart ?? null,
                    scheduledEnd: a.scheduledEnd ?? null,
                    version: originalTask.version
                };
            });

            await scheduleApi.save({ assignments });

            const msg = `Schedule approved and saved - ${state.result.assignedTasks} task(s) updated.`;

            setState(prev => ({
                ...prev,
                isSaving: false,
                result: null,
                successMsg: msg
            }));

            return msg;
        } catch (err: unknown) {
            let errorMsg = "An unexpected error occurred while saving the schedule";
            let validationErrs: string[] = [];

            if (isAxiosError(err)) {
                if (err.response?.status === 422 && Array.isArray(err.response.data?.details)) {
                    validationErrs = err.response.data.details;
                    errorMsg = "Batch validation failed. Please review the specific errors.";
                } else if (err.response?.status === 409) {
                    errorMsg = "Data Conflict: Tasks were modified by another user. Please refresh and try again.";
                } else {
                    errorMsg = err.response?.data?.message || err.message;
                }
            } else if (err instanceof Error) {
                errorMsg = err.message;
            }

            setState(prev => ({
                ...prev,
                isSaving: false,
                error: errorMsg,
                validationErrors: validationErrs
            }));

            throw err;
        }
    };

    return {
        scheduleResult: state.result,
        isGenerating: state.isGenerating,
        isSaving: state.isSaving,
        error: state.error,
        validationErrors: state.validationErrors,
        successMsg: state.successMsg,
        setValidationErrors: (errors: string[]) => setState(prev => ({ ...prev, validationErrors: errors })),
        runAlgorithm,
        saveSchedule,
        clearMessages
    };
};
