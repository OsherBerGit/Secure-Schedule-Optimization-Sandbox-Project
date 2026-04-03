// TypeScript interfaces matching backend DTOs

export interface Department {
  id: number;
  name: string;
}

export interface Skill {
  id: number;
  name: string;
  description?: string;
}

export interface WorkerAvailability {
  id: number | null;
  dayOfWeek: 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';
  startTime: string; // ISO LocalTime, e.g. "09:00:00"
  endTime: string;   // ISO LocalTime, e.g. "17:00:00"
}

export interface User {
  id: number;
  nationalId: string;
  firstName: string | null;
  lastName: string | null;
  email: string | null;
  phoneNumber: string | null;
  salary: number | null;
  address: string | null;
  availabilities: WorkerAvailability[];
  maxTasks: number | null;
  departmentName: string | null;
  departmentId?: number; // Decoded from JWT
  role: 'ADMIN' | 'MANAGER' | 'WORKER';
  skills: Skill[];
}

export interface CreateUserRequest {
  nationalId: string;
  password: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  phoneNumber?: string;
  role: 'ADMIN' | 'MANAGER' | 'WORKER';
  departmentName?: string;
  availabilities?: WorkerAvailability[];
  skillIds?: number[];
}

export interface UpdateUserRequest {
  firstName?: string;
  lastName?: string;
  email?: string;
  phoneNumber?: string;
  role?: 'ADMIN' | 'MANAGER' | 'WORKER';
  departmentName?: string | null;
  availabilities?: WorkerAvailability[];
  skillIds?: number[];
}

export interface Task {
  id: number;
  title: string;
  description: string | null;
  deadline: string | null;
  durationHours: number | null;
  startTime: string | null;
  priorityId: number | null;
  priorityName: string | null;
  departmentName: string | null;
  // Task lifecycle status (category="TASK": OPEN, LOCKED, CLOSED)
  taskStatusId: number | null;
  taskStatusName: string | null;       // "OPEN" | "LOCKED" | "CLOSED"
  taskStatusCategory: string | null;   // always "TASK"
  taskStatusColorCode: string | null;  // hex colour for badge
  
  /** Optimistic locking version from backend. */
  version: number;
  requiredSkill?: Skill;
}

export interface SchedulingConfiguration {
  id: number | null;
  configName: string;
  weightPriority: number;
  weightDeadline: number;
  weightFairness: number;
  mutationRate: number;
  crossoverRate: number;
  localSearchFrequency: number;
  populationSize: number;
  maxGenerations: number;
  isActive: boolean;
  createdByUserId?: number;
}

export interface CreateTaskRequest {
  title: string;
  description?: string;
  deadline?: string;
  durationHours?: number;
  priorityId: number;
  departmentId?: number;
  requiredSkill?: number | null;
}

export interface UpdateTaskRequest {
  title: string;
  description?: string;
  deadline?: string;
  durationHours?: number;
  priorityId: number;
  departmentId?: number;
  requiredSkill?: number | null;
  statusId?: number;
  taskStatusId?: number;
}

export interface Status {
  id: number;
  name: string;
  colorCode: string | null;
}

/** Lookup values for Settlement execution lifecycle (PENDING, ASSIGNED, IN_PROGRESS, COMPLETED, FAILED). */
export interface SettlementStatus {
  id: number;
  name: string;
  colorCode: string | null;
}

export interface Priority {
  id: number;
  name: string;
}

export interface ConstraintType {
  id: number;
  name: string;
  description: string | null;
}

export interface CreateConstraintTypeRequest {
  name: string;
  description?: string;
}

export interface UpdateConstraintTypeRequest {
  name: string;
  description?: string;
}

export interface TaskConstraint {
  id: number;
  predecessorTaskId: number;
  successorTaskId: number;
  constraintTypeId: number;
  lagMinutes: number | null;
  predecessorTaskTitle: string | null;
  successorTaskTitle: string | null;
  constraintTypeName: string | null;
}

export interface CreateTaskConstraintRequest {
  predecessorTaskId: number;
  successorTaskId: number;
  constraintTypeId: number;
  lagMinutes?: number;
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

// Worker self-request - no workerId needed
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
  // Settlement execution status (PENDING, IN_PROGRESS, COMPLETED, FAILED) - from settlement_statuses table
  statusId: number | null;
  statusName: string | null;
  statusColorCode: string | null;
}

export interface CreateSettlementRequest {
  taskId: number;
  workerId: number;
  settlementDate: string;
  completionDate?: string;
  statusId?: number;
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

export type ScheduleStrategy = 'GREEDY' | 'ROUND_ROBIN' | 'MEMETIC' | 'CONSTRAINT_PROGRAMMING';

export interface TaskAssignmentResult {
  taskId: number;
  taskTitle: string;
  assignedUserId: number | null;
  assignedUserFullName: string | null;
  scheduledStart: string | null;
  scheduledEnd: string | null;
  reason: string;
}

/** A task that the algorithm failed to schedule, with an explainability reason. */
export interface UnscheduledTaskResult {
  taskId: number;
  taskName: string;
  reason: string;
}

/** A single approved assignment to persist via POST /api/schedule/save. */
export interface SaveTaskAssignment {
  taskId: number;
  assignedUserId: number | null;
  scheduledStart: string | null;
  scheduledEnd: string | null;
  /** Version of the task at the time of scheduling, for optimistic locking. */
  version: number;
}

/** Request body for POST /api/schedule/save. */
export interface SaveScheduleRequest {
  assignments: SaveTaskAssignment[];
}

export interface ScheduleResult {
  strategyUsed: string;
  totalTasks: number;
  assignedTasks: number;
  unassignedTasks: number;
  assignments: TaskAssignmentResult[];
  /** Best fitness score recorded at each generation (Memetic algorithm only). */
  fitnessHistory?: number[];
  /** Tasks the algorithm could not assign, with human-readable failure reasons. */
  unscheduledTasks?: UnscheduledTaskResult[];
}
