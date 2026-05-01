import React, { useState } from "react";
import type { SchedulingConfiguration } from "../../types";

interface SchedulingConfigurationFormProps {
    initialConfig?: Omit<SchedulingConfiguration, "id" | "isActive">;
    onSubmit: (config: Omit<SchedulingConfiguration, "id" | "isActive">) => void;
    onCancel: () => void;
    isLoading: boolean;
}

const DEFAULT_CONFIG: Omit<SchedulingConfiguration, "id" | "isActive"> = {
    configName: "New Configuration",
    weightPriority: 0.5,
    weightDeadline: 0.5,
    weightFairness: 0.5,
    populationSize: 50,
    maxGenerations: 100,
    mutationRate: 0.1,
    crossoverRate: 0.9,
    localSearchFrequency: 0.2
};

const SchedulingConfigurationForm: React.FC<SchedulingConfigurationFormProps> = ({ initialConfig = DEFAULT_CONFIG, onSubmit, onCancel, isLoading }) => {
    const [config, setConfig] = useState(initialConfig);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value, type } = e.target;
        const parsedValue = type === "number" ? parseFloat(value) || 0 : value;

        setConfig(prev => ({
            ...prev,
            [name]: parsedValue
        }));
    };

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        onSubmit(config);
    };

    return (
        <form onSubmit={handleSubmit} className="config-form-container">
            <div className="config-body">
                <div className="config-form-section">
                    <div className="modern-form-group full-width">
                        <label className="modern-label">Configuration Name</label>
                        <input type="text" name="configName" className="modern-input" required value={config.configName} onChange={handleChange} />
                    </div>
                </div>

                <div className="config-section-wrapper">
                    <h3>Evolutionary Parameters</h3>
                    <div className="config-form-section">
                        <div className="modern-form-group">
                            <label className="modern-label">Population Size</label>
                            <input
                                type="number"
                                name="populationSize"
                                className="modern-input"
                                min="10"
                                required
                                value={config.populationSize}
                                onChange={handleChange}
                            />
                        </div>
                        <div className="modern-form-group">
                            <label className="modern-label">Max Generations</label>
                            <input
                                type="number"
                                name="maxGenerations"
                                className="modern-input"
                                min="1"
                                required
                                value={config.maxGenerations}
                                onChange={handleChange}
                            />
                        </div>
                        <div className="modern-form-group">
                            <label className="modern-label">Mutation Rate (0-1)</label>
                            <input
                                type="number"
                                name="mutationRate"
                                className="modern-input"
                                step="0.01"
                                min="0"
                                max="1"
                                required
                                value={config.mutationRate}
                                onChange={handleChange}
                            />
                        </div>
                        <div className="modern-form-group">
                            <label className="modern-label">Crossover Rate (0-1)</label>
                            <input
                                type="number"
                                name="crossoverRate"
                                className="modern-input"
                                step="0.01"
                                min="0"
                                max="1"
                                required
                                value={config.crossoverRate}
                                onChange={handleChange}
                            />
                        </div>
                        <div className="modern-form-group">
                            <label className="modern-label">Local Search Frequency (0-1)</label>
                            <input
                                type="number"
                                name="localSearchFrequency"
                                className="modern-input"
                                step="0.01"
                                min="0"
                                max="1"
                                required
                                value={config.localSearchFrequency}
                                onChange={handleChange}
                            />
                        </div>
                    </div>
                </div>

                <div className="config-section-wrapper">
                    <h3>Fitness Weights</h3>
                    <div className="config-form-section">
                        <div className="modern-form-group">
                            <label className="modern-label">Priority Weight</label>
                            <input
                                type="number"
                                name="weightPriority"
                                className="modern-input"
                                step="0.1"
                                min="0"
                                max="1"
                                required
                                value={config.weightPriority}
                                onChange={handleChange}
                            />
                        </div>
                        <div className="modern-form-group">
                            <label className="modern-label">Deadline Weight</label>
                            <input
                                type="number"
                                name="weightDeadline"
                                className="modern-input"
                                step="0.1"
                                min="0"
                                max="1"
                                required
                                value={config.weightDeadline}
                                onChange={handleChange}
                            />
                        </div>
                        <div className="modern-form-group">
                            <label className="modern-label">Fairness Weight</label>
                            <input
                                type="number"
                                name="weightFairness"
                                className="modern-input"
                                step="0.1"
                                min="0"
                                max="1"
                                required
                                value={config.weightFairness}
                                onChange={handleChange}
                            />
                        </div>
                    </div>
                </div>
            </div>

            <div className="form-actions">
                <button type="button" className="btn-secondary" onClick={onCancel}>
                    Cancel
                </button>
                <button type="submit" className="btn-primary-modern" disabled={isLoading}>
                    {isLoading ? "Saving..." : "Save & Select"}
                </button>
            </div>
        </form>
    );
};

export default SchedulingConfigurationForm;
