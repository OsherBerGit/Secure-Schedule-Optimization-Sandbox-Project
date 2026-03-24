import { useState, useCallback, useEffect } from 'react'
import type { Task, User, Department } from '../types'
import { taskApi, userApi, departmentApi } from '../api'

export const useScheduleData = () => {
    const [tasks, setTasks] = useState<Task[]>([])
    const [workers, setWorkers] = useState<User[]>([])
    const [departments, setDepartments] = useState<Department[]>([])
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const fetchData = useCallback(async () => {
        setIsLoading(true)
        setError(null)
        try {
            const [tasksRes, workersRes, departmentsRes] = await Promise.all([
                taskApi.getAll(),
                userApi.getByRole('WORKER'),
                departmentApi.getAll().catch(() => ({ data: [] as Department[] })) // Safe fallback
            ])
            setTasks(tasksRes.data)
            setWorkers(workersRes.data)
            setDepartments(departmentsRes.data || [])
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
        workers,
        departments,
        isLoading,
        error,
        refreshData: fetchData
    }
}

