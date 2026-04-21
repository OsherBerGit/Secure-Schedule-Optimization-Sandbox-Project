import type {Skill} from "./user.ts";

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
    taskStatusId: number | null;
    taskStatusName: string | null; // "OPEN" | "LOCKED" | "CLOSED"
    taskStatusCategory: string | null; // always "TASK"
    version: number;
    requiredSkills?: Skill[];
}

export interface CreateTaskRequest {
    title: string;
    description?: string;
    deadline?: string;
    durationHours?: number;
    priorityId: number;
    departmentId?: number;
    requiredSkills?: number[];
}

export interface UpdateTaskRequest {
    title: string;
    description?: string;
    deadline?: string;
    durationHours?: number;
    priorityId: number;
    departmentId?: number;
    requiredSkills?: number[];
    statusId?: number;
    taskStatusId?: number;
    prerequisiteTaskId?: number;
}

export interface CreateConstraintTypeRequest {
    name: string;
    description?: string;
}

export interface UpdateConstraintTypeRequest {
    name: string;
    description?: string;
}

export interface ConstraintType {
    id: number;
    name: string;
    description: string | null;
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

export interface Priority {
    id: number;
    name: string;
}