import axiosInstance from "./axios";
import type {
    LoginRequest,
    LoginResponse,
    RefreshTokenResponse,
    User,
    CreateUserRequest,
    UpdateUserRequest,
    Department,
    Skill,
    Task,
    CreateTaskRequest,
    UpdateTaskRequest,
    Vacation,
    CreateVacationRequest,
    UpdateVacationRequest,
    VacationRequestDto,
    VacationStatusUpdateRequest,
    Settlement,
    CreateSettlementRequest,
    Status,
    SettlementStatus,
    Priority,
    ConstraintType,
    CreateConstraintTypeRequest,
    UpdateConstraintTypeRequest,
    TaskConstraint,
    CreateTaskConstraintRequest,
    ScheduleStrategy,
    ScheduleResult,
    SaveScheduleRequest
} from "../types";

export const authApi = {
    login: (data: LoginRequest) => axiosInstance.post<LoginResponse>("/auth/login", data),
    logout: () => axiosInstance.post("/auth/logout"),
    refresh: () => axiosInstance.post<RefreshTokenResponse>("/auth/refresh-token", {})
};

export const userApi = {
    getAll: () => axiosInstance.get<User[]>("/users"),
    getById: (id: number) => axiosInstance.get<User>(`/users/${id}`),
    getByNationalId: (nationalId: string) => axiosInstance.get<User>(`/users/national-id/${nationalId}`),
    create: (data: CreateUserRequest) => axiosInstance.post<User>("/users", data),
    update: (id: number, data: UpdateUserRequest) => axiosInstance.put<User>(`/users/${id}`, data),
    delete: (id: number) => axiosInstance.delete(`/users/${id}`),
    getByRole: (role: string) => axiosInstance.get<User[]>(`/users/role/${role}`),
    getByDepartment: (departmentId: number) => axiosInstance.get<User[]>(`/users/department/${departmentId}`),
    getByEmail: (email: string) => axiosInstance.get<User>(`/users/email/${email}`)
};

export const departmentApi = {
    getAll: () => axiosInstance.get<Department[]>("/departments"),
    getById: (id: number) => axiosInstance.get<Department>(`/departments/${id}`),
    create: (name: string) => axiosInstance.post<Department>("/departments", { name }),
    update: (id: number, name: string) => axiosInstance.put<Department>(`/departments/${id}`, { name }),
    delete: (id: number) => axiosInstance.delete(`/departments/${id}`)
};

export const skillApi = {
    getAll: () => axiosInstance.get<Skill[]>("/skill"),
    create: (name: string, description?: string) => axiosInstance.post<Skill>("/skill", { name, description }),
    update: (id: number, name: string, description?: string) => axiosInstance.put<Skill>(`/skill/${id}`, { name, description }),
    delete: (id: number) => axiosInstance.delete(`/skill/${id}`)
};

export const taskApi = {
    getAll: () => axiosInstance.get<Task[]>("/tasks"),
    getById: (id: number) => axiosInstance.get<Task>(`/tasks/${id}`),
    create: (data: CreateTaskRequest) => axiosInstance.post<Task>("/tasks", data),
    update: (id: number, data: UpdateTaskRequest) => axiosInstance.put<Task>(`/tasks/${id}`, data),
    delete: (id: number) => axiosInstance.delete(`/tasks/${id}`),
    getByUser: (userId: number) => axiosInstance.get<Task[]>(`/tasks/user/${userId}`),
    getValidPrerequisites: (taskId: number) => axiosInstance.get<Task[]>(`/tasks/${taskId}/valid-prerequisites`)
};

export const statusApi = {
    getAll: () => axiosInstance.get<Status[]>("/task-statuses"),
    getById: (id: number) => axiosInstance.get<Status>(`/task-statuses/${id}`),
    create: (data: { name: string }) => axiosInstance.post<Status>("/task-statuses", data),
    update: (id: number, data: { name: string }) => axiosInstance.put<Status>(`/task-statuses/${id}`, data),
    delete: (id: number) => axiosInstance.delete(`/task-statuses/${id}`)
};

export const settlementStatusApi = {
    getAll: () => axiosInstance.get<SettlementStatus[]>("/settlement-statuses")
};

export const priorityApi = {
    getAll: () => axiosInstance.get<Priority[]>("/priorities"),
    getById: (id: number) => axiosInstance.get<Priority>(`/priorities/${id}`),
    create: (data: { name: string }) => axiosInstance.post<Priority>("/priorities", data),
    update: (id: number, data: { name: string }) => axiosInstance.put<Priority>(`/priorities/${id}`, data),
    delete: (id: number) => axiosInstance.delete(`/priorities/${id}`)
};

export const constraintTypeApi = {
    getAll: () => axiosInstance.get<ConstraintType[]>("/constraint-types"),
    getById: (id: number) => axiosInstance.get<ConstraintType>(`/constraint-types/${id}`),
    create: (data: CreateConstraintTypeRequest) => axiosInstance.post<ConstraintType>("/constraint-types", data),
    update: (id: number, data: UpdateConstraintTypeRequest) => axiosInstance.put<ConstraintType>(`/constraint-types/${id}`, data),
    delete: (id: number) => axiosInstance.delete(`/constraint-types/${id}`)
};

export const taskConstraintApi = {
    getAll: () => axiosInstance.get<TaskConstraint[]>("/task-constraints"),
    getById: (id: number) => axiosInstance.get<TaskConstraint>(`/task-constraints/${id}`),
    create: (data: CreateTaskConstraintRequest) => axiosInstance.post<TaskConstraint>("/task-constraints", data),
    update: (id: number, data: CreateTaskConstraintRequest) => axiosInstance.put<TaskConstraint>(`/task-constraints/${id}`, data),
    delete: (id: number) => axiosInstance.delete(`/task-constraints/${id}`),
    getByPredecessor: (taskId: number) => axiosInstance.get<TaskConstraint[]>(`/task-constraints/predecessor/${taskId}`),
    getBySuccessor: (taskId: number) => axiosInstance.get<TaskConstraint[]>(`/task-constraints/successor/${taskId}`)
};

export const settlementApi = {
    getAll: () => axiosInstance.get<Settlement[]>("/settlements"),
    getById: (id: number) => axiosInstance.get<Settlement>(`/settlements/${id}`),
    create: (data: CreateSettlementRequest) => axiosInstance.post<Settlement>("/settlements", data),
    delete: (id: number) => axiosInstance.delete(`/settlements/${id}`),
    getByUser: (userId: number) => axiosInstance.get<Settlement[]>(`/settlements/user/${userId}`),
    getMySettlements: () => axiosInstance.get<Settlement[]>("/settlements/user/me"),
    completeSettlement: (id: number) => axiosInstance.patch<Settlement>(`/settlements/${id}/complete`),
    getByTask: (taskId: number) => axiosInstance.get<Settlement[]>(`/settlements/task/${taskId}`)
};

export const vacationApi = {
    getAll: () => axiosInstance.get<Vacation[]>("/vacations"),
    getById: (id: number) => axiosInstance.get<Vacation>(`/vacations/${id}`),
    create: (data: CreateVacationRequest) => axiosInstance.post<Vacation>("/vacations", data),
    request: (data: VacationRequestDto) => axiosInstance.post<Vacation>("/vacations/request", data),
    updateStatus: (id: number, data: VacationStatusUpdateRequest) => axiosInstance.patch<Vacation>(`/vacations/${id}/status`, data),
    update: (id: number, data: UpdateVacationRequest) => axiosInstance.put<Vacation>(`/vacations/${id}`, data),
    delete: (id: number) => axiosInstance.delete(`/vacations/${id}`),
    getByUser: (userId: number) => axiosInstance.get<Vacation[]>(`/vacations/user/${userId}`),
    getByDateRange: (startDate: string, endDate: string) =>
        axiosInstance.get<Vacation[]>("/vacations/date-range", {
            params: { startDate, endDate }
        })
};

export const scheduleApi = {
    run: (strategy: ScheduleStrategy = "GREEDY", departmentId?: number | null) => {
        const params = new URLSearchParams({ strategy });
        if (departmentId != null) params.append("departmentId", String(departmentId));
        return axiosInstance.post<ScheduleResult>(`/schedule/run?${params.toString()}`);
    },

    save: (data: SaveScheduleRequest) => axiosInstance.post<void>("/schedule/save", data),

    runWithConfig: (strategy: ScheduleStrategy, configId: number, departmentId?: number | null) => {
        const params = new URLSearchParams({
            strategy,
            configId: String(configId)
        });
        if (departmentId != null) params.append("departmentId", String(departmentId));
        return axiosInstance.post<ScheduleResult>(`/schedule/run?${params.toString()}`);
    }
};

export const schedulingConfigApi = {
    getActive: () => axiosInstance.get<import("../types").SchedulingConfiguration>("/scheduling-configs/active"),

    create: (config: Omit<import("../types").SchedulingConfiguration, "id">) =>
        axiosInstance.post<import("../types").SchedulingConfiguration>("/scheduling-configs", config),

    getAll: () => axiosInstance.get<import("../types").SchedulingConfiguration[]>("/scheduling-configs")
};
