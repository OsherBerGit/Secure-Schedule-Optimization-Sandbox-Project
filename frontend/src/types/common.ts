import type { User } from "./user.ts";

export interface Status {
    id: number;
    name: string;
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

// Algorithm types

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

export type ScheduleStrategy =
    | "GREEDY"
    | "ROUND_ROBIN"
    | "MEMETIC"
    | "CONSTRAINT_PROGRAMMING";

export interface TaskAssignmentResult {
    taskId: number;
    taskTitle: string;
    assignedUserId: number | null;
    assignedUserFullName: string | null;
    scheduledStart: string | null;
    scheduledEnd: string | null;
    reason: string;
}

export interface UnscheduledTaskResult {
    taskId: number;
    taskName: string;
    reason: string;
}

export interface SaveTaskAssignment {
    taskId: number;
    assignedUserId: number | null;
    scheduledStart: string | null;
    scheduledEnd: string | null;
    version: number;
}

export interface SaveScheduleRequest {
    assignments: SaveTaskAssignment[];
}

export interface ScheduleResult {
    strategyUsed: string;
    totalTasks: number;
    assignedTasks: number;
    unassignedTasks: number;
    assignments: TaskAssignmentResult[];
    unscheduledTasks?: UnscheduledTaskResult[];
}

export interface MemeticScheduleResult extends ScheduleResult {
    strategyUsed: "MEMETIC";
    fitnessHistory: number[];
}

export function isMemeticResult(
    result: ScheduleResult | null | undefined,
): result is MemeticScheduleResult {
    return (
        !!result &&
        result.strategyUsed === "MEMETIC" &&
        "fitnessHistory" in result
    );
}