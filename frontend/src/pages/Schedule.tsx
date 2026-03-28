import { useState, useEffect } from 'react'
import type { ScheduleStrategy } from '../types'
import { useAuth } from '../context/useAuth'
import { useScheduleData } from '../hooks/useScheduleData'
import { useScheduleAlgorithm } from '../hooks/useScheduleAlgorithm'
import { useSchedulingConfig } from '../hooks/useSchedulingConfig'
import FitnessChart from '../components/FitnessChart'
import BatchErrorSummary from '../components/BatchErrorSummary'
import SchedulingConfigurationModal from '../components/SchedulingConfigurationModal'
import ScheduleGantt from '../components/schedule/ScheduleGantt'
import ScheduleTable from '../components/schedule/ScheduleTable'
import ScheduleExplainability from '../components/schedule/ScheduleExplainability'
import UnscheduledWarning from '../components/schedule/UnscheduledWarning'
import './Schedule.css'

const Schedule = () => {
    const { user: currentUser } = useAuth()

    // 1. Data Hook
    const {
        tasks, workers, departments, isLoading: isDataLoading, refreshData, error: dataError
    } = useScheduleData()

    // 2. Algorithm Hook
    const {
        scheduleResult, isGenerating, isSaving,
        error: algoError, validationErrors, successMsg,
        runAlgorithm, saveSchedule, setValidationErrors, clearMessages
    } = useScheduleAlgorithm()

    // 3. Configuration Hook
    const {
        configs, isConfigModalOpen, selectedConfigId, isLoading: isConfigLoading, error: configError,
        openConfigModal, closeConfigModal, selectConfig, createConfig, fetchConfigs
    } = useSchedulingConfig()

    // Local UI State
    const [viewMode, setViewMode] = useState<'gantt' | 'table'>('gantt')
    const [strategy, setStrategy] = useState<ScheduleStrategy>('GREEDY')
    const [selectedDepartmentId, setSelectedDepartmentId] = useState<number | null>(null)

    // Fetch configs when modal opens
    useEffect(() => {
        if (isConfigModalOpen) fetchConfigs()
    }, [isConfigModalOpen, fetchConfigs])

    // Derived State
    const assignmentMap = new Map<number, number | null>(
        scheduleResult?.assignments.map(a => [a.taskId, a.assignedUserId]) ?? []
    )

    const scheduledTasks = tasks.filter(t => t.startTime)
    const unassignedTasks  = tasks.filter(t => !assignmentMap.has(t.id) || assignmentMap.get(t.id) == null)
    // Warning for tasks that are assigned in the draft but don't have a start time yet (should be rare if algorithm works)
    const unscheduledWarningTasks = tasks.filter(t => assignmentMap.get(t.id) != null && !t.startTime)

    // Handlers
    const handleGenerate = async () => {
        clearMessages()
        await runAlgorithm(strategy, selectedDepartmentId, selectedConfigId)
        await refreshData()
    }

    const handleApprove = async () => {
        await saveSchedule(tasks)
        await refreshData()
    }

    const handleCreateConfig = async (newConfig: Omit<import('../types').SchedulingConfiguration, 'id' | 'isActive'>) => {
        // We set isActive=false by default for new custom configs
        const configToCreate = { ...newConfig, isActive: false };
        const created = await createConfig(configToCreate)
        if (created && created.id) {
            selectConfig(created.id)
            closeConfigModal()
            // successMsg sets by hook? No, only algorithm hook. We could set a local toast if needed.
        }
    }

    const error = dataError || algoError || configError

    return (
        <div className="schedule-container">

            {/* Header */}
            <div className="schedule-header">
                <div className="schedule-title">
                    <h1>📅 Schedule</h1>
                    <p className="schedule-subtitle">
                        {scheduledTasks.length} of {tasks.length} tasks scheduled
                    </p>
                </div>
                <div className="schedule-actions">
                    <div className="view-toggle">
                        <button
                            className={viewMode === 'gantt' ? 'active' : ''}
                            onClick={() => setViewMode('gantt')}
                        >
                            Gantt
                        </button>
                        <button
                            className={viewMode === 'table' ? 'active' : ''}
                            onClick={() => setViewMode('table')}
                        >
                            Table
                        </button>
                    </div>
                    {currentUser?.role === 'ADMIN' && (
                        <div className="schedule-generate-group">
                            <select
                                className="department-select"
                                value={selectedDepartmentId ?? ''}
                                onChange={e => {
                                    const val = e.target.value
                                    setSelectedDepartmentId(val ? Number(val) : null)
                                }}
                                disabled={isGenerating}
                            >
                                <option value="">Global (All Departments)</option>
                                {departments.map(d => (
                                    <option key={d.id} value={d.id}>{d.name}</option>
                                ))}
                            </select>

                            <select
                                className="strategy-select"
                                value={strategy}
                                onChange={e => setStrategy(e.target.value as ScheduleStrategy)}
                                disabled={isGenerating}
                            >
                                <option value="GREEDY">Greedy (Fastest)</option>
                                <option value="ROUND_ROBIN">Round Robin (Fairness)</option>
                                <option value="CONSTRAINT_PROGRAMMING">Constraint Programming (Exact)</option>
                                <option value="MEMETIC">Memetic (Genetic + Local Search)</option>
                            </select>

                            <button
                                className={`btn-secondary ${strategy !== 'MEMETIC' ? 'btn-ghost' : ''}`}
                                disabled={strategy !== 'MEMETIC'}
                                title={strategy !== 'MEMETIC' ? "Available only for Memetic Algorithm" : "Configure Algorithm Parameters"}
                                onClick={openConfigModal}
                                style={strategy !== 'MEMETIC' ? { opacity: 0.5, cursor: 'not-allowed' } : {}}
                            >
                               ⚙️ Algorithm Configuration
                            </button>

                            <button
                                className="generate-btn"
                                onClick={handleGenerate}
                                disabled={isGenerating || tasks.length === 0}
                            >
                                {isGenerating ? 'Optimizing...' : 'Generate Schedule Draft'}
                            </button>

                            {scheduleResult && scheduleResult.strategyUsed === 'MEMETIC' && (
                                <span style={{ marginLeft: '1rem', fontStyle: 'italic', color: '#666' }}>
                                    Used Config ID: {selectedConfigId || 'Default'}
                                </span>
                            )}
                        </div>
                    )}
                </div>
            </div>

            {/* Batch Validation Errors */}
            {validationErrors.length > 0 && (
                <BatchErrorSummary
                    errors={validationErrors}
                    onClose={() => setValidationErrors([])}
                />
            )}

            {/* Success/Error Alerts */}
            {error && !validationErrors.length && <div className="error-msg">{error}</div>}
            {successMsg && <div className="success-msg">{successMsg}</div>}

            {/* Algorithm Result Panel */}
            {scheduleResult && (
                <div className="result-panel">
                    <h3 className="result-title">📊 Draft Preview — {scheduleResult.strategyUsed}</h3>
                    <div className="result-stats">
                        <div className="result-stat">
                            <span className="stat-value">{scheduleResult.totalTasks}</span>
                            <span className="stat-label">Total</span>
                        </div>
                        <div className="result-stat result-stat--success">
                            <span className="stat-value">{scheduleResult.assignedTasks}</span>
                            <span className="stat-label">Assigned</span>
                        </div>
                        <div className="result-stat result-stat--warn">
                            <span className="stat-value">{scheduleResult.unassignedTasks}</span>
                            <span className="stat-label">Unassigned</span>
                        </div>
                    </div>
                    {/* Only show simplified assignment list if needed, or rely on Gantt/Table */}
                    {/* For now, keeping the list as it provides quick feedback */}
                    <div className="result-assignments">
                        {scheduleResult.assignments.map(a => (
                            <div key={a.taskId} className={`result-row ${a.assignedUserId ? 'assigned' : 'unassigned'}`}>
                                <span className="result-task">{a.taskTitle}</span>
                                <span className="result-arrow">→</span>
                                <span className="result-user">
                                    {a.assignedUserFullName ?? '⚠️ Unassigned'}
                                </span>
                                {a.scheduledStart && (
                                    <span className="result-dates">
                                        {new Date(a.scheduledStart).toLocaleDateString()} –{' '}
                                        {a.scheduledEnd ? new Date(a.scheduledEnd).toLocaleDateString() : '?'}
                                    </span>
                                )}
                                <span className="result-reason">{a.reason}</span>
                            </div>
                        ))}
                    </div>

                    {/* Approve & Save */}
                    {currentUser?.role === 'ADMIN' && (
                        <div className="result-approve-row">
                            <p className="result-approve-hint">
                                ℹ️ This is a <strong>draft preview</strong>. Nothing has been saved yet.
                                Click the button below to commit these assignments to the database.
                            </p>
                            <button
                                className="btn-approve"
                                onClick={handleApprove}
                                disabled={isSaving || scheduleResult.assignedTasks === 0}
                            >
                                {isSaving ? '💾 Saving...' : '✅ Approve & Save Schedule'}
                            </button>
                        </div>
                    )}
                </div>
            )}

            {/* ── Convergence Graph ── */}
            {scheduleResult?.fitnessHistory && scheduleResult.fitnessHistory.length > 0 && (
                <FitnessChart fitnessHistory={scheduleResult.fitnessHistory} />
            )}

            {isDataLoading ? (
                <div className="loading">Loading schedule data...</div>
            ) : (
                <>
                    {/* Summary Cards */}
                    <div className="summary-cards">
                        <div className="summary-card">
                            <span className="card-value">{tasks.length}</span>
                            <span className="card-label">Total Tasks</span>
                        </div>
                        <div className="summary-card">
                            <span className="card-value">{scheduledTasks.length}</span>
                            <span className="card-label">Scheduled</span>
                        </div>
                        <div className="summary-card">
                            <span className="card-value">{unscheduledWarningTasks.length}</span>
                            <span className="card-label">Unscheduled</span>
                        </div>
                        <div className="summary-card">
                            <span className="card-value">{unassignedTasks.length}</span>
                            <span className="card-label">Unassigned</span>
                        </div>
                        <div className="summary-card">
                            <span className="card-value">{workers.length}</span>
                            <span className="card-label">Workers</span>
                        </div>
                    </div>

                    {scheduledTasks.length === 0 && !scheduleResult ? (
                        <div className="empty-state">
                            <div className="empty-icon">📋</div>
                            <h3>No scheduled tasks yet</h3>
                            <p>
                                {currentUser?.role === 'ADMIN'
                                    ? 'Click "Generate Schedule" to run the scheduling algorithm.'
                                    : 'The admin has not generated a schedule yet.'}
                            </p>
                        </div>
                    ) : viewMode === 'gantt' ? (
                        <ScheduleGantt
                            tasks={tasks}
                            workers={workers}
                            assignmentMap={assignmentMap}
                        />
                    ) : (
                        <ScheduleTable
                            tasks={tasks}
                            workers={workers}
                            assignmentMap={assignmentMap}
                            assignments={scheduleResult?.assignments ?? []}
                        />
                    )}

                    {/* Warnings and Explainability */}
                    <UnscheduledWarning tasks={unscheduledWarningTasks} />

                    {scheduleResult?.unscheduledTasks && (
                        <ScheduleExplainability failures={scheduleResult.unscheduledTasks} />
                    )}
                </>
            )}

            {/* Configuration Modal */}
            {isConfigModalOpen && (
                <SchedulingConfigurationModal
                    configs={configs}
                    isLoading={isConfigLoading}
                    error={configError}
                    onClose={closeConfigModal}
                    initialConfigId={selectedConfigId}
                    onSelectConfig={selectConfig}
                    onCreateConfig={handleCreateConfig}
                />
            )}

            {isGenerating && (
                <div className="progress-indicator">
                    <div className="spinner" />
                    <span>Optimizing schedule, please wait...</span>
                </div>
            )}
        </div>
    )
}

export default Schedule
