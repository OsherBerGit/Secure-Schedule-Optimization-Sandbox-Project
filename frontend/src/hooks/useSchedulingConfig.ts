import { useState, useCallback } from 'react'
import type { SchedulingConfiguration } from '../types'
import { schedulingConfigApi } from '../api'

export const useSchedulingConfig = () => {
    const [configs, setConfigs] = useState<SchedulingConfiguration[]>([])
    const [isConfigModalOpen, setIsConfigModalOpen] = useState(false)
    const [selectedConfigId, setSelectedConfigId] = useState<number | null>(null)
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const fetchConfigs = useCallback(async () => {
        setIsLoading(true)
        setError(null)
        try {
            const res = await schedulingConfigApi.getAll()
            setConfigs(res.data)

            if (res.data.length > 0 && !selectedConfigId) {
                setSelectedConfigId(res.data[0].id)
            }
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : 'Failed to load configurations'
            setError(msg)
        } finally {
            setIsLoading(false)
        }
    }, [selectedConfigId])

    const createConfig = useCallback(async (newConfig: Omit<SchedulingConfiguration, 'id'>) => {
        setIsLoading(true)
        setError(null)
        try {
            const res = await schedulingConfigApi.create(newConfig)
            setConfigs(prev => [...prev, res.data])
            setSelectedConfigId(res.data.id)
            return res.data
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : 'Failed to create configuration'
            setError(msg)
            throw new Error(msg)
        } finally {
            setIsLoading(false)
        }
    }, [])

    const clearError = useCallback(() => setError(null), [])

    return {
        configs,
        isConfigModalOpen,
        selectedConfigId,
        isLoading,
        error,
        openConfigModal: () => setIsConfigModalOpen(true),
        closeConfigModal: () => setIsConfigModalOpen(false),
        selectConfig: setSelectedConfigId,
        fetchConfigs,
        createConfig,
        clearError
    }
}