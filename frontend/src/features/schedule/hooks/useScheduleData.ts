import { useState, useCallback, useEffect } from "react";
import type { Task, User, Department, Settlement } from "../../../types";
import { taskApi, userApi, departmentApi, settlementApi } from "../../../api";

interface ScheduleDataState {
    tasks: Task[];
    users: User[];
    departments: Department[];
    settlements: Settlement[];
    isLoading: boolean;
    error: string | null;
}

export const useScheduleData = () => {
    const [state, setState] = useState<ScheduleDataState>({
        tasks: [],
        users: [],
        departments: [],
        settlements: [],
        isLoading: false,
        error: null
    });

    const fetchData = useCallback(async () => {
        setState(prev => ({ ...prev, isLoading: true, error: null }));

        try {
            // שימוש ב-Promise.all הוא קריטי כאן לביצועים במערכת Secure-Schedule
            const [tasksRes, usersRes, departmentsRes, settlementsRes] = await Promise.all([
                taskApi.getAll(),
                userApi.getByRole("WORKER"),
                departmentApi.getAll().catch(() => ({ data: [] as Department[] })), // Safe fallback הנדסי
                settlementApi.getAll().catch(() => ({ data: [] as Settlement[] }))
            ]);

            setState({
                tasks: tasksRes.data,
                users: usersRes.data,
                departments: departmentsRes.data || [],
                settlements: settlementsRes.data || [],
                isLoading: false,
                error: null
            });
        } catch (err: unknown) {
            setState(prev => ({
                ...prev,
                isLoading: false,
                error: err instanceof Error ? err.message : "Failed to load schedule data"
            }));
        }
    }, []);

    useEffect(() => {
        fetchData();
    }, [fetchData]);

    return {
        ...state,
        refreshData: fetchData
    };
};
