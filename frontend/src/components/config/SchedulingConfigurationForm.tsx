import React, { useState } from 'react'
import type { SchedulingConfiguration } from '../../types'

interface SchedulingConfigurationFormProps {
    initialConfig?: Omit<SchedulingConfiguration, 'id' | 'isActive'>
    onSubmit: (config: Omit<SchedulingConfiguration, 'id' | 'isActive'>) => void
    onCancel: () => void
    isLoading: boolean
}

const DEFAULT_CONFIG: Omit<SchedulingConfiguration, 'id' | 'isActive'> = {
    configName: 'New Configuration',
    weightPriority: 0.5,
    weightDeadline: 0.5,
    weightFairness: 0.5,
    populationSize: 50,
    maxGenerations: 100,
    mutationRate: 0.1,
    crossoverRate: 0.9,
    localSearchFrequency: 0.2
}

const SchedulingConfigurationForm: React.FC<SchedulingConfigurationFormProps> = ({ 
    initialConfig = DEFAULT_CONFIG, 
    onSubmit, 
    onCancel, 
    isLoading 
}) => {
    const [config, setConfig] = useState(initialConfig)

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault()
        onSubmit(config)
    }

    return (
        <form onSubmit={handleSubmit}>
            <div className="config-section">
                <h3>General Settings</h3>
                <div className="form-group">
                    <label>Configuration Name</label>
                    <input 
                        type="text" 
                        required 
                        value={config.configName}
                        onChange={e => setConfig({...config, configName: e.target.value})}
                    />
                </div>
            </div>

            <div className="config-section">
                <h3>Evolutionary Parameters</h3>
                <div className="form-row">
                    <div className="form-group">
                        <label>Population Size (min 10)</label>
                        <input 
                            type="number" 
                            min="10"
                            required
                            value={config.populationSize}
                            onChange={e => setConfig({...config, populationSize: parseInt(e.target.value)})}
                        />
                    </div>
                    <div className="form-group">
                        <label>Max Generations (min 1)</label>
                        <input 
                            type="number" 
                            min="1" 
                            required
                            value={config.maxGenerations}
                            onChange={e => setConfig({...config, maxGenerations: parseInt(e.target.value)})}
                        />
                    </div>
                </div>
                <div className="form-row">
                    <div className="form-group">
                        <label>Mutation Rate (0.0 - 1.0)</label>
                        <input 
                            type="number" 
                            step="0.01" min="0" max="1"
                            required
                            value={config.mutationRate}
                            onChange={e => setConfig({...config, mutationRate: parseFloat(e.target.value)})}
                        />
                    </div>
                    <div className="form-group">
                        <label>Crossover Rate (0.0 - 1.0)</label>
                        <input 
                            type="number" 
                            step="0.01" min="0" max="1"
                            required
                            value={config.crossoverRate}
                            onChange={e => setConfig({...config, crossoverRate: parseFloat(e.target.value)})}
                        />
                    </div>
                </div>
                <div className="form-group">
                    <label>Local Search Frequency (0.0 - 1.0)</label>
                    <input 
                        type="number" 
                        step="0.01" min="0" max="1"
                        required
                        value={config.localSearchFrequency}
                        onChange={e => setConfig({...config, localSearchFrequency: parseFloat(e.target.value)})}
                    />
                </div>
            </div>

            <div className="config-section">
                <h3>Fitness Weights (0.0 - 1.0)</h3>
                <div className="form-row">
                    <div className="form-group">
                        <label>Priority Weight</label>
                        <input 
                            type="number" 
                            step="0.1" min="0" max="1"
                            required
                            value={config.weightPriority}
                            onChange={e => setConfig({...config, weightPriority: parseFloat(e.target.value)})}
                        />
                    </div>
                    <div className="form-group">
                        <label>Deadline Weight</label>
                        <input 
                            type="number" 
                            step="0.1" min="0" max="1"
                            required
                            value={config.weightDeadline}
                            onChange={e => setConfig({...config, weightDeadline: parseFloat(e.target.value)})}
                        />
                    </div>
                    <div className="form-group">
                        <label>Fairness Weight</label>
                        <input 
                            type="number" 
                            step="0.1" min="0" max="1"
                            required
                            value={config.weightFairness}
                            onChange={e => setConfig({...config, weightFairness: parseFloat(e.target.value)})}
                        />
                    </div>
                </div>
            </div>

            <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={onCancel}>Cancel</button>
                <button type="submit" className="btn-primary" disabled={isLoading}>
                    {isLoading ? 'Saving...' : 'Save & Select'}
                </button>
            </div>
        </form>
    )
}

export default SchedulingConfigurationForm

