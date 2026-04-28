export interface SettlementStatus {
    id: number;
    name: string;
}

export interface Settlement {
    id: number;
    taskId: number;
    userId: number;
    settlementDate: string;
    completionDate: string | null;
    taskTitle: string;
    userName: string;
    statusId: number | null;
    statusName: string | null;
}

export interface CreateSettlementRequest {
    taskId: number;
    userId: number;
    settlementDate: string;
    completionDate?: string;
    statusId?: number;
}

export interface UpdateSettlementRequest {
    taskId: number;
    userId: number;
    settlementDate: string;
    completionDate?: string;
}

export interface Vacation {
    id: number;
    userId: number;
    userName: string;
    startDate: string;
    endDate: string;
    statusName: string | null;
}

export interface CreateVacationRequest {
    userId: number;
    startDate: string;
    endDate: string;
}

export interface UpdateVacationRequest {
    userId?: number;
    startDate: string;
    endDate: string;
}

export interface VacationRequestDto {
    startDate: string;
    endDate: string;
}

export interface VacationStatusUpdateRequest {
    status: "APPROVED" | "REJECTED";
}