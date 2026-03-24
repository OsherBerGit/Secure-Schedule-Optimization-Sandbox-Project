import { useState } from 'react'
import type { Task, ScheduleStrategy, ScheduleResult, SaveTaskAssignment } from '../types'
import { scheduleApi } from '../api'

export const useScheduleAlgorithm = () => {
    const [scheduleResult, setScheduleResult] = useState<ScheduleResult | null>(null)
    const [isGenerating, setIsGenerating] = useState(false)
    const [isSaving, setIsSaving] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [validationErrors, setValidationErrors] = useState<string[]>([])
    const [successMsg, setSuccessMsg] = useState<string | null>(null)

    // Run the algorithm
    const runAlgorithm = async (
        strategy: ScheduleStrategy, 
        departmentId: number | null,
        configId: number | null
    ) => {
        setIsGenerating(true)
        setError(null)
        setSuccessMsg(null)
        setScheduleResult(null)
        
        try {
            let res;
            if (strategy === 'MEMETIC' && configId) {
                res = await scheduleApi.runWithConfig(strategy, configId, departmentId)
            } else {
                res = await scheduleApi.run(strategy, departmentId)
            }
            
            setScheduleResult(res.data)
            const assigned = res.data.assignedTasks
            const unassigned = res.data.unassignedTasks
            setSuccessMsg(
                `✅ Draft generated using ${res.data.strategyUsed} — ` +
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

    // Save the approved draft
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
                    throw new Error(`Task ID ${a.taskId} not found in local state via ID.`)
                }

                return {
                    taskId: a.taskId,
                    assignedUserId: a.assignedUserId ?? null,
                    scheduledStart: a.scheduledStart ?? null,
                    scheduledEnd: a.scheduledEnd ?? null,
                    version: originalTask.version // Send version for optimistic locking
                }
            })

            await scheduleApi.save({ assignments })
            
            const msg = `✅ Schedule approved and saved — ${scheduleResult.assignedTasks} task(s) scheduled.`
            setSuccessMsg(msg)
            setScheduleResult(null) // Clear draft state
            return msg
        } catch (err: unknown) {
            // Handle Structured Batch Validation Errors (422)
            // @ts-expect-error err is unknown but we check fields safely
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
        isGenerating,
        isSaving,
        error,
        validationErrors,
        successMsg,
        setValidationErrors, // Exposed for closing the summary component
        runAlgorithm,
        saveSchedule,
        clearMessages
    }
}

