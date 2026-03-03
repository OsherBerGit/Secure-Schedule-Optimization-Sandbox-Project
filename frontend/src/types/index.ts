// TypeScript interfaces matching backend DTOs

export interface User {
  id: number;
  nationalId: string;
  firstName: string | null;
  lastName: string | null;
  email: string | null;
  phoneNumber: string | null;
  salary: number | null;
  address: string | null;
  dailyAvailabilityHours: number | null;
  maxTasks: number | null;
  roles: string[]; // e.g. ["ADMIN"] or ["WORKER"]
  // helper getter — derived on frontend
  role?: 'ADMIN' | 'WORKER';
}

export interface CreateUserRequest {
  nationalId: string;
  password: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  phoneNumber?: string;
  role: 'ADMIN' | 'WORKER';
}

export interface UpdateUserRequest {
  firstName?: string;
  lastName?: string;
  email?: string;
  phoneNumber?: string;
  role?: 'ADMIN' | 'WORKER';
}

export interface Task {
  id: number;
  title: string;
  description: string | null;
  deadline: string | null;
  durationHours: number | null;
  startTime: string | null;
  priorityId: number | null;
  statusId: number | null;
  assignedWorkerId: number | null;
  priorityName: string | null;
  statusName: string | null;
  assignedWorkerName: string | null;
}

export interface CreateTaskRequest {
  title: string;
  description?: string;
  deadline?: string;
  durationHours?: number;
  priorityId: number;
  statusId: number;
  assignedWorkerId?: number;
}

export interface UpdateTaskRequest {
  title: string;
  description?: string;
  deadline?: string;
  durationHours?: number;
  priorityId: number;
  statusId: number;
  assignedWorkerId?: number;
}

export interface Status {
  id: number;
  name: string;
}

export interface Priority {
  id: number;
  name: string;
}

export interface Vacation {
  id: number;
  workerId: number;
  workerName: string;
  startDate: string;
  endDate: string;
  statusName: string | null;
}

export interface CreateVacationRequest {
  workerId: number;
  startDate: string;
  endDate: string;
}

export interface UpdateVacationRequest {
  workerId?: number;
  startDate: string;
  endDate: string;
}

// Worker self-request — no workerId needed
export interface VacationRequestDto {
  startDate: string;
  endDate: string;
}

// Admin approve/reject
export interface VacationStatusUpdateRequest {
  status: 'APPROVED' | 'REJECTED';
}

export interface Settlement {
  id: number;
  taskId: number;
  workerId: number;
  settlementDate: string;
  completionDate: string | null;
  taskTitle: string;
  workerName: string;
}

export interface CreateSettlementRequest {
  taskId: number;
  workerId: number;
  settlementDate: string;
  completionDate?: string;
}

export interface UpdateSettlementRequest {
  taskId: number;
  workerId: number;
  settlementDate: string;
  completionDate?: string;
}

// Authentication types
export interface LoginRequest {
  nationalId: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface RefreshTokenResponse {
  accessToken: string;
  refreshToken: string;
}

export interface ApiResponse<T> {
  data: T;
  message?: string;
  success: boolean;
}

export interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  refreshAccessToken: () => Promise<void>;
}

// ── Algorithm / Schedule types ──────────────────────────────────────────────

export type ScheduleStrategy = 'GREEDY' | 'ROUND_ROBIN';

export interface TaskAssignmentResult {
  taskId: number;
  taskTitle: string;
  assignedUserId: number | null;
  assignedUserFullName: string | null;
  scheduledStart: string | null;
  scheduledEnd: string | null;
  reason: string;
}

export interface ScheduleResult {
  strategyUsed: string;
  totalTasks: number;
  assignedTasks: number;
  unassignedTasks: number;
  assignments: TaskAssignmentResult[];
}

