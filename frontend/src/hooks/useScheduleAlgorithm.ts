import { useState } from 'react'
import type { Task, ScheduleStrategy, ScheduleResult, SaveTaskAssignment } from '../types'
import { scheduleApi } from '../api'

export const useScheduleAlgorithm = () => {
    const [scheduleResult, setScheduleResult] = useState<ScheduleResult | null>(null)
    const [fitnessData, setFitnessData] = useState<any[]>([])
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
        setError(null)
        setSuccessMsg(null)
        setScheduleResult(null)
        setFitnessData([])

        try {
            let res;
            if (strategy === 'MEMETIC' && configId) {
                res = await scheduleApi.runWithConfig(strategy, configId, departmentId)
            } else {
                res = await scheduleApi.run(strategy, departmentId)
            }

            if (res.data.fitnessHistory) {
                setFitnessData(res.data.fitnessHistory)
            }

            setScheduleResult(res.data)
            const assigned = res.data.assignedTasks
            const unassigned = res.data.unassignedTasks

            setSuccessMsg(
                `Draft generated using ${res.data.strategyUsed} - ` +
                `${assigned} assigned, ${unassigned} unassigned. ` +
                `Review below and click "Approve & Save" to persist.`
            )
            return res.data
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : 'Failed to generate schedule'
            setError(msg)
            throw new Error(msg)
        } finally {
            setIsGenerating(false)
        }
    }

    const saveSchedule = async (tasks: Task[]) => {
        if (!scheduleResult) return

        setIsSaving(true)
        setError(null)
        setValidationErrors([])
        setSuccessMsg(null)

        try {
            const assignments: SaveTaskAssignment[] = scheduleResult.assignments.map(a => {
                const originalTask = tasks.find(t => t.id === a.taskId)
                if (!originalTask) {
                    throw new Error(`Task ID ${a.taskId} not found in local state.`)
                }

                return {
                    taskId: a.taskId,
                    assignedUserId: a.assignedUserId ?? null,
                    scheduledStart: a.scheduledStart ?? null,
                    scheduledEnd: a.scheduledEnd ?? null,
                    version: originalTask.version
                }
            })

            await scheduleApi.save({ assignments })

            const msg = `Schedule approved and saved - ${scheduleResult.assignedTasks} task(s) scheduled.`
            setSuccessMsg(msg)
            setScheduleResult(null)
            setFitnessData([])
            return msg
        } catch (err: unknown) {
            // @ts-expect-error safely checked
            if (err?.response?.status === 422 && Array.isArray(err?.response?.data?.details)) {
                // @ts-expect-error checked above
                setValidationErrors(err.response.data.details)
                setError('Batch validation failed. Please review the errors below.')
            } else if (err instanceof Error) {
                setError(err.message)
            } else {
                setError('Failed to save schedule')
            }
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