import { useState, useCallback } from "react";
import type { SchedulingConfiguration } from "../../../types";
import { schedulingConfigApi } from "../../../api";
import { isAxiosError } from "axios";

interface SchedulingConfigState {
    items: SchedulingConfiguration[];
    selectedId: number | null;
    isLoading: boolean;
    error: string | null;
}

export const useSchedulingConfig = () => {
    const [state, setState] = useState<SchedulingConfigState>({
        items: [],
        selectedId: null,
        isLoading: false,
        error: null
    });

    const [isModalOpen, setIsModalOpen] = useState(false);

    const fetchConfigs = useCallback(async () => {
        setState(prev => ({ ...prev, isLoading: true, error: null }));

        try {
            const response = await schedulingConfigApi.getAll();
            const configs = response.data;

            setState(prev => ({
                ...prev,
                items: configs,
                isLoading: false,
                selectedId: prev.selectedId || (configs.length > 0 ? configs[0].id : null)
            }));
        } catch (err: unknown) {
            const message = isAxiosError(err) ? err.response?.data?.message || err.message : "Failed to load configurations";

            setState(prev => ({ ...prev, isLoading: false, error: message }));
        }
    }, []);

    const createConfig = useCallback(async (newConfig: Omit<SchedulingConfiguration, "id" | "isActive">) => {
        setState(prev => ({ ...prev, isLoading: true, error: null }));

        try {
            const response = await schedulingConfigApi.create({ ...newConfig, isActive: false });
            const created = response.data;

            setState(prev => ({
                ...prev,
                items: [...prev.items, created],
                selectedId: created.id,
                isLoading: false
            }));

            return created;
        } catch (err: unknown) {
            const message = isAxiosError(err) ? err.response?.data?.message || err.message : "Failed to create configuration";

            setState(prev => ({
                ...prev,
                isLoading: false,
                error: message
            }));
            throw new Error(message);
        }
    }, []);

    const selectConfig = useCallback((id: number) => {
        setState(prev => ({ ...prev, selectedId: id }));
    }, []);

    const clearError = useCallback(() => {
        setState(prev => ({ ...prev, error: null }));
    }, []);

    return {
        configs: state.items,
        selectedConfigId: state.selectedId,
        isLoading: state.isLoading,
        error: state.error,
        isConfigModalOpen: isModalOpen,
        openConfigModal: () => setIsModalOpen(true),
        closeConfigModal: () => setIsModalOpen(false),
        selectConfig,
        fetchConfigs,
        createConfig,
        clearError
    };
};
