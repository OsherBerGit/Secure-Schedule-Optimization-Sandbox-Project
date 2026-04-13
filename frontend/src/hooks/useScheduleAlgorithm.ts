import { useState } from 'react'
import type { Task, ScheduleStrategy, ScheduleResult, SaveTaskAssignment } from '../types'
import { scheduleApi } from '../api'
import { isAxiosError } from 'axios'

export const useScheduleAlgorithm = () => {
    const [scheduleResult, setScheduleResult] = useState<ScheduleResult | null>(null)
    const [fitnessData, setFitnessData] = useState<number[]>([])
    const [isGenerating, setIsGenerating] = useState(false)
    const [isSaving, setIsSaving] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [validationErrors, setValidationErrors] = useState<string[]>([])
    const [successMsg, setSuccessMsg] = useState<string | null>(null)

    const runAlgorithm = async (
        strategy: ScheduleStrategy,
        departmentId: number | null,
        configId: number | null
    ) => {
        setIsGenerating(true)
        clearMessages()
        setScheduleResult(null)
        setFitnessData([])

        try {
            const res = (strategy === 'MEMETIC' && configId)
                ? await scheduleApi.runWithConfig(strategy, configId, departmentId)
                : await scheduleApi.run(strategy, departmentId)

            if (res.data.fitnessHistory)
                setFitnessData(res.data.fitnessHistory)

            setScheduleResult(res.data)

            const { assignedTasks, unassignedTasks, strategyUsed } = res.data

            setSuccessMsg(
                `Draft generated using ${res.data.strategyUsed} - ` +
                `${assignedTasks} assigned, ${unassignedTasks} unassigned. ` +
                `Review below and click "Approve & Save" to persist.`
            )
            return res.data
        } catch (err: unknown) {
            const msg = isAxiosError(err) ? err.response?.data?.message || err.message : 'Failed to generate schedule'
            setError(msg)
            throw new Error(msg)
        } finally {
            setIsGenerating(false)
        }
    }

    const saveSchedule = async (tasks: Task[]) => {
        if (!scheduleResult) return

        setIsSaving(true)
        clearMessages()

        try {
            const assignments: SaveTaskAssignment[] = scheduleResult.assignments.map(a => {
                const originalTask = tasks.find(t => t.id === a.taskId)

                if (!originalTask)
                    throw new Error(`Task ID ${a.taskId} not found in local state.`)

                if (originalTask.version === undefined || originalTask.version === null)
                    throw new Error(`Integrity Error: Task ID ${a.taskId} is missing a version number required for saving.`)

                return {
                    taskId: a.taskId,
                    assignedUserId: a.assignedUserId ?? null,
                    scheduledStart: a.scheduledStart ?? null,
                    scheduledEnd: a.scheduledEnd ?? null,
                    version: originalTask.version
                }
            })

            await scheduleApi.save({ assignments })

            const msg = `Schedule approved and saved - ${scheduleResult.assignedTasks} task(s) updated.`
            setSuccessMsg(msg)

            setScheduleResult(null)
            setFitnessData([])

            return msg
        } catch (err: unknown) {
            if (isAxiosError(err)) {
                if (err.response?.status === 422 && Array.isArray(err.response.data?.details)) {
                    setValidationErrors(err.response.data.details)
                    setError('Batch validation failed. Please review the specific errors.')
                } else if (err.response?.status === 409)
                    setError('Data Conflict: Tasks were modified by another user. Please refresh and try again.')
                else
                    setError(err.response?.data?.message || err.message)

            } else if (err instanceof Error)
                setError(err.message)
            else
                setError('An unexpected error occurred while saving the schedule')

            throw err
        } finally {
            setIsSaving(false)
        }
    }

    const clearMessages = () => {
        setError(null)
        setSuccessMsg(null)
        setValidationErrors([])
    }

    return {
        scheduleResult,
        fitnessData,
        isGenerating,
        isSaving,
        error,
        validationErrors,
        successMsg,
        setValidationErrors,
        runAlgorithm,
        saveSchedule,
        clearMessages
    }
}