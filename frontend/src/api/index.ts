import axiosInstance from './axios';
import type {
  LoginRequest,
  LoginResponse,
  RefreshTokenRequest,
  RefreshTokenResponse,
  User,
  CreateUserRequest,
  UpdateUserRequest,
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
  Priority,
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

  update: (id: number, data: UpdateUserRequest) =>
    axiosInstance.put<User>(`/users/${id}`, data),

  delete: (id: number) =>
    axiosInstance.delete(`/users/${id}`),

  getByRole: (role: string) =>
    axiosInstance.get<User[]>(`/users/role/${role}`),

  getByEmail: (email: string) =>
    axiosInstance.get<User>(`/users/email/${email}`),
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

  getByStatus: (statusName: string) =>
    axiosInstance.get<Task[]>(`/tasks/status/${statusName}`),
};

// Status API
export const statusApi = {
  getAll: () =>
    axiosInstance.get<Status[]>('/statuses'),
};

// Priority API
export const priorityApi = {
  getAll: () =>
    axiosInstance.get<Priority[]>('/priorities'),
};

// Settlement API
export const settlementApi = {  getAll: () =>
    axiosInstance.get<Settlement[]>('/settlements'),

  getById: (id: number) =>
    axiosInstance.get<Settlement>(`/settlements/${id}`),

  create: (data: CreateSettlementRequest) =>
    axiosInstance.post<Settlement>('/settlements', data),

  delete: (id: number) =>
    axiosInstance.delete(`/settlements/${id}`),

  getByWorker: (workerId: number) =>
    axiosInstance.get<Settlement[]>(`/settlements/worker/${workerId}`),

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

// Schedule API — placeholder, ready to connect when backend algorithm is implemented
export const scheduleApi = {
  // POST /api/schedule/generate — triggers the scheduling algorithm
  generate: () =>
    axiosInstance.post('/schedule/generate'),
};

