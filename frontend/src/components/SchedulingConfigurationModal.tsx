import React, { useState } from 'react'
import type { SchedulingConfiguration } from '../types'
import SchedulingConfigurationForm from './config/SchedulingConfigurationForm' // Extracted form
import { useAuth } from '../context/useAuth'
import './SchedulingConfigurationModal.css'

interface SchedulingConfigurationModalProps {
    // Data
    configs: SchedulingConfiguration[]
    isLoading: boolean
    error: string | null
    
    // Actions
    onClose: () => void
    onSelectConfig: (configId: number) => void
    onCreateConfig: (config: Omit<SchedulingConfiguration, 'id' | 'isActive'>) => Promise<void>
    
    // Initial State
    initialConfigId?: number | null
}

const SchedulingConfigurationModal: React.FC<SchedulingConfigurationModalProps> = ({ 
    configs, 
    isLoading, 
    error,
    onClose, 
    onSelectConfig, 
    onCreateConfig,
    initialConfigId 
}) => {
    const { user } = useAuth();
    const isAdmin = user?.role === 'ADMIN';

    // UI Mode: 'select' (dropdown) or 'create' (form)
    const [mode, setMode] = useState<'select' | 'create'>('select')
    const [selectedId, setSelectedId] = useState<number | ''>(initialConfigId ?? '')

    const handleSelectSubmit = () => {
        if (selectedId !== '') {
            onSelectConfig(Number(selectedId))
            onClose()
        }
    }

    const handleCreateSubmit = async (newConfig: Omit<SchedulingConfiguration, 'id' | 'isActive'>) => {
        await onCreateConfig(newConfig)
    }

    return (
        <div className="scheduling-config-modal-overlay">
            <div className="scheduling-config-modal">
                <h2>⚙️ Algorithm Configuration (Memetic)</h2>
                
                {error && <div className="error-msg">{error}</div>}

                {/* TABS */}
                <div style={{ marginBottom: '1rem', borderBottom: '1px solid #ddd', display: 'flex', gap: '1rem' }}>
                    <button 
                        type="button"
                        onClick={() => setMode('select')}
                        className={mode === 'select' ? 'btn-primary' : 'btn-outline'}
                        style={{ borderBottomLeftRadius: 0, borderBottomRightRadius: 0, borderBottom: 'none' }}
                    >
                        Select Existing
                    </button>
                    {isAdmin && (
                        <button
                            type="button"
                            onClick={() => setMode('create')}
                            className={mode === 'create' ? 'btn-primary' : 'btn-outline'}
                            style={{ borderBottomLeftRadius: 0, borderBottomRightRadius: 0, borderBottom: 'none' }}
                        >
                            Create New +
                        </button>
                    )}
                </div>

                {mode === 'select' ? (
                    <div className="config-section">
                        <div className="form-group">
                            <label>Choose a Preset:</label>
                            <select 
                                value={selectedId} 
                                onChange={(e) => setSelectedId(Number(e.target.value))}
                                disabled={isLoading}
                            >
                                <option value="" disabled>-- Select a Configuration --</option>
                                {configs.map(c => (
                                    <option key={c.id} value={c.id ?? ''}>
                                        {c.configName} (Gen: {c.maxGenerations}, Pop: {c.populationSize})
                                    </option>
                                ))}
                            </select>
                        </div>
                        <div className="modal-actions">
                            <button className="btn-secondary" onClick={onClose}>Cancel</button>
                            <button 
                                className="btn-primary" 
                                onClick={handleSelectSubmit}
                                disabled={!selectedId}
                            >
                                Confirm Selection
                            </button>
                        </div>
                    </div>
                ) : (
                    <SchedulingConfigurationForm 
                        onSubmit={handleCreateSubmit}
                        onCancel={() => setMode('select')}
                        isLoading={isLoading}
                    />
                )}
            </div>
        </div>
    )
}

export default SchedulingConfigurationModal
