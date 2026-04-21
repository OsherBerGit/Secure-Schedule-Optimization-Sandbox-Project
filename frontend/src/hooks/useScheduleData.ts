import { useState, useCallback, useEffect } from 'react'
import type { Task, User, Department, Settlement } from '../types'
import { taskApi, userApi, departmentApi, settlementApi } from '../api'

export const useScheduleData = () => {
    const [tasks, setTasks] = useState<Task[]>([])
    const [users, setUsers] = useState<User[]>([])
    const [departments, setDepartments] = useState<Department[]>([])
    const [settlements, setSettlements] = useState<Settlement[]>([])
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const fetchData = useCallback(async () => {
        setIsLoading(true)
        setError(null)
        try {
            const [tasksRes, usersRes, departmentsRes, settlementsRes] = await Promise.all([
                taskApi.getAll(),
                userApi.getByRole('WORKER'),
                departmentApi.getAll().catch(() => ({ data: [] as Department[] })), // Safe fallback
                settlementApi.getAll().catch(() => ({ data: [] as Settlement[] }))
            ])
            setTasks(tasksRes.data)
            setUsers(usersRes.data)
            setDepartments(departmentsRes.data || [])
            setSettlements(settlementsRes.data || [])
        } catch (err: unknown) {
            setError(err instanceof Error ? err.message : 'Failed to load schedule data')
        } finally {
            setIsLoading(false)
        }
    }, [])

    useEffect(() => {
        void fetchData()
    }, [fetchData])

    return {
        tasks,
        users: users,
        departments,
        settlements,
        isLoading,
        error,
        refreshData: fetchData
    }
}
