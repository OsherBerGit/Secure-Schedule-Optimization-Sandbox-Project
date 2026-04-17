import React, { useState } from 'react'
import type { SchedulingConfiguration } from '../types'
import SchedulingConfigurationForm from './config/SchedulingConfigurationForm'
import { useAuth } from '../context/useAuth'
import { Settings2, X, FileCheck, Plus, Check } from 'lucide-react'
import './SchedulingConfigurationModal.css'

interface SchedulingConfigurationModalProps {
    configs: SchedulingConfiguration[]
    isLoading: boolean
    error: string | null
    onClose: () => void
    onSelectConfig: (configId: number) => void
    onCreateConfig: (config: Omit<SchedulingConfiguration, 'id' | 'isActive'>) => Promise<void>
    initialConfigId?: number | null
}

const SchedulingConfigurationModal: React.FC<SchedulingConfigurationModalProps> = ({configs, isLoading, error, onClose, onSelectConfig, onCreateConfig, initialConfigId}) => {
    const { user } = useAuth()
    const isAdmin = user?.role === 'ADMIN'

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
        setMode('select')
    }

    return (
        <div className="modal-overlay">
            <div className="modern-modal-card config-modal">
                <div className="modal-header">
                    <div className="modal-title-combined">
                        <Settings2 size={22} className="text-primary" />
                        <h2>Memetic Algorithm Configuration</h2>
                    </div>
                    <button type="button" className="modern-close-btn" onClick={onClose}>
                        <X size={20} />
                    </button>
                </div>

                <div className="config-tabs">
                    <button
                        className={`config-tab ${mode === 'select' ? 'active' : ''}`}
                        onClick={() => setMode('select')}
                    >
                        <FileCheck size={16} />
                        <span>Select Preset</span>
                    </button>
                    {isAdmin && (
                        <button
                            className={`config-tab ${mode === 'create' ? 'active' : ''}`}
                            onClick={() => setMode('create')}
                        >
                            <Plus size={16} />
                            <span>New Configuration</span>
                        </button>
                    )}
                </div>

                {mode === 'select' ? (
                    <>
                        <div className="modal-body config-body">
                            {error && (
                                <div className="error-banner">
                                    <X size={16} />
                                    <span>{error}</span>
                                </div>
                            )}
                            <div className="config-select-mode">
                                <p className="config-instruction">Choose a performance profile to guide the optimization process.</p>
                                <div className="modern-form-group">
                                    <label className="modern-label">Active Profile</label>
                                    <select
                                        className="modern-input preset-select"
                                        value={selectedId}
                                        onChange={(e) => setSelectedId(Number(e.target.value))}
                                        disabled={isLoading}
                                    >
                                        <option value="" disabled>-- Choose a Configuration --</option>
                                        {configs.map(c => (
                                            <option key={c.id} value={c.id ?? ''}>
                                                {c.configName} (Gen: {c.maxGenerations}, Pop: {c.populationSize})
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                {selectedId && configs.find(c => c.id === selectedId) && (
                                    <div className="config-details-preview">
                                        <div className="preview-stat">
                                            <span className="label">Generations</span>
                                            <span className="value">{configs.find(c => c.id === selectedId)?.maxGenerations}</span>
                                        </div>
                                        <div className="preview-stat">
                                            <span className="label">Population</span>
                                            <span className="value">{configs.find(c => c.id === selectedId)?.populationSize}</span>
                                        </div>
                                        <div className="preview-stat">
                                            <span className="label">Mutation</span>
                                            <span className="value">{(configs.find(c => c.id === selectedId)?.mutationRate || 0) * 100}%</span>
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>

                        <div className="modal-actions-footer">
                            <button type="button" className="btn-cancel-flat" onClick={onClose}>Cancel</button>
                            <button
                                type="button"
                                className="btn-submit-modern"
                                onClick={handleSelectSubmit}
                                disabled={!selectedId || isLoading}
                            >
                                <Check size={18} />
                                Apply Configuration
                            </button>
                        </div>
                    </>
                ) : (
                    <>
                        {error && (
                            <div className="error-banner" style={{ margin: '1rem 2rem 0' }}>
                                <X size={16} />
                                <span>{error}</span>
                            </div>
                        )}
                        <SchedulingConfigurationForm
                            onSubmit={handleCreateSubmit}
                            onCancel={() => setMode('select')}
                            isLoading={isLoading}
                        />
                    </>
                )}
            </div>
        </div>
    )
}

export default SchedulingConfigurationModal
