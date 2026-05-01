export interface Department {
    id: number;
    name: string;
}

export interface Skill {
    id: number;
    name: string;
}

export interface UserAvailability {
    id: number | null;
    dayOfWeek: "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY";
    startTime: string; // ISO LocalTime, e.g. "09:00:00"
    endTime: string; // ISO LocalTime, e.g. "17:00:00"
}

export interface User {
    id: number;
    nationalId: string;
    firstName: string | null;
    lastName: string | null;
    email: string | null;
    phoneNumber: string | null;
    availabilities: UserAvailability[];
    maxTasks: number | null;
    departmentName: string | null;
    departmentId?: number; // Decoded from JWT
    role: "ADMIN" | "MANAGER" | "WORKER";
    skills: Skill[];
}

export interface CreateUserRequest {
    nationalId: string;
    password: string;
    firstName?: string;
    lastName?: string;
    email?: string;
    phoneNumber?: string;
    role: "ADMIN" | "MANAGER" | "WORKER";
    maxTasks?: number;
    departmentName?: string;
    availabilities?: UserAvailability[];
    skillIds?: number[];
}

export interface UpdateUserRequest {
    firstName?: string;
    lastName?: string;
    email?: string;
    phoneNumber?: string;
    role?: "ADMIN" | "MANAGER" | "WORKER";
    maxTasks?: number;
    departmentName?: string | null;
    availabilities?: UserAvailability[];
    skillIds?: number[];
}
