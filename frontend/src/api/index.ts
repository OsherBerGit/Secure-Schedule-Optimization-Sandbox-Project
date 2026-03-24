import axiosInstance from './axios';
import type {
  LoginRequest,
  LoginResponse,
  RefreshTokenRequest,
  RefreshTokenResponse,
  User,
  CreateUserRequest,
  UpdateUserRequest,
  Department,
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
  SaveScheduleRequest,
} from '../types';

// Auth API
export const authApi = {
  login: (data: LoginRequest) =>
    axiosInstance.post<LoginResponse>('/auth/login', data),

  logout: () =>
    axiosInstance.post('/auth/logout'),

  refresh: (data: RefreshTokenRequest) =>
    axiosInstance.post<RefreshTokenResponse>('/auth/refresh-token', data),
};

// User API
export const userApi = {
  getAll: () =>
    axiosInstance.get<User[]>('/users'),

  getById: (id: number) =>
    axiosInstance.get<User>(`/users/${id}`),

  getByNationalId: (nationalId: string) =>
    axiosInstance.get<User>(`/users/national-id/${nationalId}`),

  create: (data: CreateUserRequest) =>
    axiosInstance.post<User>('/users', data),

  // Backend PUT /users/{id} accepts UserDto — forward the full shape
  update: (id: number, data: UpdateUserRequest) =>
    axiosInstance.put<User>(`/users/${id}`, data),

  delete: (id: number) =>
    axiosInstance.delete(`/users/${id}`),

  getByRole: (role: string) =>
    axiosInstance.get<User[]>(`/users/role/${role}`),

  getByEmail: (email: string) =>
    axiosInstance.get<User>(`/users/email/${email}`),
};

// Department API
export const departmentApi = {
  getAll: () =>
    axiosInstance.get<Department[]>('/departments'),

  getById: (id: number) =>
    axiosInstance.get<Department>(`/departments/${id}`),

  create: (name: string) =>
    axiosInstance.post<Department>('/departments', { name }),

  update: (id: number, name: string) =>
    axiosInstance.put<Department>(`/departments/${id}`, { name }),

  delete: (id: number) =>
    axiosInstance.delete(`/departments/${id}`),
};

// Role API
export const roleApi = {
  getAll: () => axiosInstance.get<import('../types').Role[]>('/roles'),
};

// Task API
export const taskApi = {
  getAll: () =>
    axiosInstance.get<Task[]>('/tasks'),

  getById: (id: number) =>
    axiosInstance.get<Task>(`/tasks/${id}`),

  create: (data: CreateTaskRequest) =>
    axiosInstance.post<Task>('/tasks', data),

  update: (id: number, data: UpdateTaskRequest) =>
    axiosInstance.put<Task>(`/tasks/${id}`, data),

  delete: (id: number) =>
    axiosInstance.delete(`/tasks/${id}`),

  getByWorker: (workerId: number) =>
    axiosInstance.get<Task[]>(`/tasks/worker/${workerId}`),
  // getByStatus removed — status is now on Settlement
};

// Status API
export const statusApi = {
  getAll: () =>
    axiosInstance.get<Status[]>('/statuses'),
  getById: (id: number) =>
    axiosInstance.get<Status>(`/statuses/${id}`),
  create: (data: { name: string }) =>
    axiosInstance.post<Status>('/statuses', data),
  update: (id: number, data: { name: string }) =>
    axiosInstance.put<Status>(`/statuses/${id}`, data),
  delete: (id: number) =>
    axiosInstance.delete(`/statuses/${id}`),
};

// Settlement Status API — read-only, values are system-seeded
export const settlementStatusApi = {
  getAll: () =>
    axiosInstance.get<SettlementStatus[]>('/settlement-statuses'),
};

// Priority API
export const priorityApi = {
  getAll: () =>
    axiosInstance.get<Priority[]>('/priorities'),
  getById: (id: number) =>
    axiosInstance.get<Priority>(`/priorities/${id}`),
  create: (data: { name: string }) =>
    axiosInstance.post<Priority>('/priorities', data),
  update: (id: number, data: { name: string }) =>
    axiosInstance.put<Priority>(`/priorities/${id}`, data),
  delete: (id: number) =>
    axiosInstance.delete(`/priorities/${id}`),
};

// Constraint Type API
export const constraintTypeApi = {
  getAll: () =>
    axiosInstance.get<ConstraintType[]>('/constraint-types'),
  getById: (id: number) =>
    axiosInstance.get<ConstraintType>(`/constraint-types/${id}`),
  create: (data: CreateConstraintTypeRequest) =>
    axiosInstance.post<ConstraintType>('/constraint-types', data),
  update: (id: number, data: UpdateConstraintTypeRequest) =>
    axiosInstance.put<ConstraintType>(`/constraint-types/${id}`, data),
  delete: (id: number) =>
    axiosInstance.delete(`/constraint-types/${id}`),
};

// Task Constraint API
export const taskConstraintApi = {
  getAll: () =>
    axiosInstance.get<TaskConstraint[]>('/task-constraints'),
  getById: (id: number) =>
    axiosInstance.get<TaskConstraint>(`/task-constraints/${id}`),
  create: (data: CreateTaskConstraintRequest) =>
    axiosInstance.post<TaskConstraint>('/task-constraints', data),
  update: (id: number, data: CreateTaskConstraintRequest) =>
    axiosInstance.put<TaskConstraint>(`/task-constraints/${id}`, data),
  delete: (id: number) =>
    axiosInstance.delete(`/task-constraints/${id}`),
  getByPredecessor: (taskId: number) =>
    axiosInstance.get<TaskConstraint[]>(`/task-constraints/predecessor/${taskId}`),
  getBySuccessor: (taskId: number) =>
    axiosInstance.get<TaskConstraint[]>(`/task-constraints/successor/${taskId}`),
};

// Settlement API
export const settlementApi = {
  getAll: () =>
    axiosInstance.get<Settlement[]>('/settlements'),

  getById: (id: number) =>
    axiosInstance.get<Settlement>(`/settlements/${id}`),

  create: (data: CreateSettlementRequest) =>
    axiosInstance.post<Settlement>('/settlements', data),

  delete: (id: number) =>
    axiosInstance.delete(`/settlements/${id}`),

  getByWorker: (workerId: number) =>
    axiosInstance.get<Settlement[]>(`/settlements/worker/${workerId}`),

  /** Returns settlements for the currently authenticated worker (JWT-based). */
  getMySettlements: () =>
    axiosInstance.get<Settlement[]>('/settlements/worker/me'),

  /** Marks a settlement as COMPLETED. Worker must own the settlement. */
  completeSettlement: (id: number) =>
    axiosInstance.patch<Settlement>(`/settlements/${id}/complete`),

  getByTask: (taskId: number) =>
    axiosInstance.get<Settlement[]>(`/settlements/task/${taskId}`),
};

// Vacation API
export const vacationApi = {
  getAll: () =>
    axiosInstance.get<Vacation[]>('/vacations'),

  getById: (id: number) =>
    axiosInstance.get<Vacation>(`/vacations/${id}`),

  // ADMIN: create vacation directly (auto-approved)
  create: (data: CreateVacationRequest) =>
    axiosInstance.post<Vacation>('/vacations', data),

  // WORKER: submit a vacation request (starts as PENDING)
  request: (data: VacationRequestDto) =>
    axiosInstance.post<Vacation>('/vacations/request', data),

  // ADMIN: approve or reject a PENDING vacation
  updateStatus: (id: number, data: VacationStatusUpdateRequest) =>
    axiosInstance.patch<Vacation>(`/vacations/${id}/status`, data),

  update: (id: number, data: UpdateVacationRequest) =>
    axiosInstance.put<Vacation>(`/vacations/${id}`, data),

  delete: (id: number) =>
    axiosInstance.delete(`/vacations/${id}`),

  getByWorker: (workerId: number) =>
    axiosInstance.get<Vacation[]>(`/vacations/worker/${workerId}`),

  getByDateRange: (startDate: string, endDate: string) =>
    axiosInstance.get<Vacation[]>('/vacations/date-range', {
      params: { startDate, endDate },
    }),
};

// Schedule API — calls main-backend which forwards to the algorithm service
export const scheduleApi = {
  /** PHASE 1: Generates a draft schedule preview. Nothing is saved to the DB.
   *  @param strategy      "GREEDY" (default) | "ROUND_ROBIN" | "MEMETIC" | "CONSTRAINT_PROGRAMMING"
   *  @param departmentId  Optional ADMIN-only scope — omit for global scheduling
   */
  run: (strategy: ScheduleStrategy = 'GREEDY', departmentId?: number | null) => {
    const params = new URLSearchParams({ strategy });
    if (departmentId != null) params.append('departmentId', String(departmentId));
    return axiosInstance.post<ScheduleResult>(`/schedule/run?${params.toString()}`);
  },

  /** PHASE 2: Persists the admin-approved draft assignments to the DB.
   *  Tasks are marked SCHEDULED and Settlements are created as ASSIGNED.
   */
  save: (data: SaveScheduleRequest) =>
    axiosInstance.post<void>('/schedule/save', data),

  /** Optional PHASE 1 override: Run with a specific config preset (Memetic only) */
  runWithConfig: (strategy: ScheduleStrategy, configId: number, departmentId?: number | null) => {
      const params = new URLSearchParams({ strategy, configId: String(configId) });
      if (departmentId != null) params.append('departmentId', String(departmentId));
      return axiosInstance.post<ScheduleResult>(`/schedule/run?${params.toString()}`);
  }
};

// Scheduling Configuration API — CRUD for algorithm parameters
export const schedulingConfigApi = {
  getActive: () => axiosInstance.get<import('../types').SchedulingConfiguration>('/scheduling-configs/active'),
  
  create: (config: Omit<import('../types').SchedulingConfiguration, 'id'>) => 
      axiosInstance.post<import('../types').SchedulingConfiguration>('/scheduling-configs', config),

  // If a GetAll endpoint exists, add it here. For now we assume we might need to fetch all to populate a dropdown
  getAll: () => axiosInstance.get<import('../types').SchedulingConfiguration[]>('/scheduling-configs'),
};
