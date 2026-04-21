import React, { useState, useEffect, useMemo } from 'react';
import {
    Play,
    Settings,
    Database,
    Calendar,
    List,
    LayoutGrid,
    LayoutDashboard,
    CheckCircle2,
    Sparkles,
    Loader2,
    Info,
    AlertCircle // Added for error display
} from 'lucide-react';
import { useSearchParams } from 'react-router-dom';

import { useAuth } from '../context/useAuth';
import { useScheduleData } from '../hooks/useScheduleData';
import { useScheduleAlgorithm } from '../hooks/useScheduleAlgorithm';
import { useSchedulingConfig } from '../hooks/useSchedulingConfig';
import FitnessChart from '../components/FitnessChart';
import SchedulingConfigurationModal from '../components/SchedulingConfigurationModal';
import ScheduleGantt from '../components/schedule/ScheduleGantt';
import ScheduleTable from '../components/schedule/ScheduleTable';
import ScheduleExplainability from '../components/schedule/ScheduleExplainability';
import type { Task, ScheduleStrategy } from '../types';

import './Schedule.css';

const STRATEGY_DESCRIPTIONS: Record<ScheduleStrategy, string> = {
    GREEDY: "A fast, straightforward approach that makes the optimal choice at each step, prioritizing immediate constraints without looking ahead.",
    ROUND_ROBIN: "Assigns tasks to resources in a circular order, ensuring an equal distribution of workload without complex constraint evaluation.",
    CONSTRAINT_PROGRAMMING: "A rigorous mathematical approach that explores the solution space to find a mathematically valid schedule satisfying all hard requirements.",
    MEMETIC: "A powerful hybrid approach combining global evolutionary search with local optimization. It iteratively improves assignments to maximize resource utilization and meet strict deadlines."
}

const Schedule: React.FC = () => {
    const [isDarkMode, setIsDarkMode] = useState(false);

    useEffect(() => {
        const checkDarkMode = () => setIsDarkMode(document.documentElement.classList.contains('dark'));
        checkDarkMode();
        const observer = new MutationObserver(checkDarkMode);
        observer.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] });
        return () => observer.disconnect();
    }, []);

    const { user: currentUser } = useAuth()
    const isAdmin = currentUser?.role === 'ADMIN'
    const isManager = currentUser?.role === 'MANAGER'
    const isWorker = currentUser?.role === 'WORKER'
    const canManage = isAdmin || isManager

    const { tasks, users, departments, settlements, isLoading: isDataLoading, refreshData, error: dataError } = useScheduleData()

    // Extracted validationErrors from the updated hook
    const {
        scheduleResult,
        fitnessData,
        isGenerating,
        isSaving,
        error: algoError,
        validationErrors, // Added this
        successMsg,
        runAlgorithm,
        saveSchedule
    } = useScheduleAlgorithm()

    const { configs, isConfigModalOpen, selectedConfigId, isLoading: isConfigLoading, error: configError, openConfigModal, closeConfigModal, selectConfig, createConfig, fetchConfigs } = useSchedulingConfig()

    const [searchParams, setSearchParams] = useSearchParams()
    const queryUserId = searchParams.get('userId')

    const [viewMode, setViewMode] = useState<'gantt' | 'table'>('gantt')
    const [strategy, setStrategy] = useState<ScheduleStrategy>('GREEDY')
    const [selectedDepartmentId, setSelectedDepartmentId] = useState<number | null>(null)
    const [selectedGanttTask, setSelectedGanttTask] = useState<Task | null>(null)

    useEffect(() => { if (isConfigModalOpen) fetchConfigs() }, [isConfigModalOpen, fetchConfigs])

    const mergedTasks = useMemo(() => tasks.map(t => {
        const draft = scheduleResult?.assignments.find(a => a.taskId === t.id)
        return draft ? { ...t, startTime: draft.scheduledStart ?? t.startTime } : t
    }), [tasks, scheduleResult])

    const assignmentMap = useMemo(() => {
        const map = new Map<number, number | null>()
        settlements?.forEach(s => { if (s.taskId) map.set(s.taskId, s.userId) })
        scheduleResult?.assignments.forEach(a => { map.set(a.taskId, a.assignedUserId) })
        return map
    }, [settlements, scheduleResult])

    const displayTasks = useMemo(() => {
        const deptName = selectedDepartmentId ? departments.find(d => d.id === selectedDepartmentId)?.name : null
        return deptName ? mergedTasks.filter(t => t.departmentName === deptName) : mergedTasks
    }, [mergedTasks, selectedDepartmentId, departments])

    const scheduledTasks = useMemo(() => displayTasks.filter(t => t.startTime || t.taskStatusName === 'SCHEDULED'), [displayTasks])

    return (
        <div className="schedule-page">
            <div
                className="schedule-header"
                style={isDarkMode ? { background: 'transparent', backgroundColor: 'transparent', boxShadow: 'none' } : {}}
            >
                <div
                    className="header-left"
                    style={isDarkMode ? { background: 'transparent', backgroundColor: 'transparent' } : {}}
                >
                    <div className="header-icon-wrapper">
                        <Calendar className="text-primary" size={32} />
                    </div>
                    <div className="header-title-wrapper">
                        <h1>Schedule Management</h1>
                        <p className="header-subtitle">{scheduledTasks.length} of {tasks.length} tasks scheduled</p>
                    </div>
                </div>

                <div
                    className="header-right"
                    style={isDarkMode ? { background: 'transparent', backgroundColor: 'transparent' } : {}}
                >
                    <div className="view-switcher">
                        <button className={viewMode === 'gantt' ? 'active' : ''} onClick={() => setViewMode('gantt')}>
                            <LayoutGrid size={16} /> Gantt
                        </button>
                        <button className={viewMode === 'table' ? 'active' : ''} onClick={() => setViewMode('table')}>
                            <List size={16} /> Table
                        </button>
                    </div>

                    {!isWorker && canManage && (
                        <div className="algo-controls">
                            <select className="modern-select" value={selectedDepartmentId ?? ''} onChange={e => setSelectedDepartmentId(e.target.value ? Number(e.target.value) : null)}>
                                <option value="">Global (All Depts)</option>
                                {departments.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}
                            </select>
                            <select className="modern-select" value={strategy} onChange={e => setStrategy(e.target.value as ScheduleStrategy)}>
                                <option value="GREEDY">Greedy</option>
                                <option value="ROUND_ROBIN">Round Robin</option>
                                <option value="CONSTRAINT_PROGRAMMING">Constraint Programming</option>
                                <option value="MEMETIC">Memetic</option>
                            </select>
                            <button className="icon-btn-settings" onClick={openConfigModal} disabled={strategy !== 'MEMETIC'}>
                                <Settings size={20} />
                            </button>
                            <button className="btn-generate-main" onClick={() => runAlgorithm(strategy, selectedDepartmentId, selectedConfigId)} disabled={isGenerating}>
                                {isGenerating ? <Loader2 className="spinner" size={18} /> : <Play size={18} fill="currentColor" />}
                                <span>{isGenerating ? 'Running...' : 'Generate'}</span>
                            </button>
                        </div>
                    )}
                </div>
            </div>

            {/* --- Global Error Banner & Validation List --- */}
            {(algoError || dataError || configError) && (
                <div
                    className="error-banner"
                    style={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: '12px',
                        padding: '16px',
                        backgroundColor: isDarkMode ? 'rgba(239, 68, 68, 0.1)' : '#fef2f2',
                        border: `1px solid ${isDarkMode ? 'rgba(239, 68, 68, 0.3)' : '#f87171'}`,
                        color: isDarkMode ? '#f87171' : '#991b1b',
                        borderRadius: '8px',
                        marginBottom: '20px'
                    }}
                >
                    <AlertCircle size={24} style={{ flexShrink: 0, marginTop: '2px' }} />
                    <div style={{ display: 'flex', flexDirection: 'column' }}>
                        <span style={{ fontWeight: 600, fontSize: '1.05rem' }}>
                            {algoError || dataError || configError}
                        </span>

                        {/* Render specific Zero-Trust validation errors if they exist */}
                        {validationErrors && validationErrors.length > 0 && (
                            <ul style={{
                                marginTop: '12px',
                                paddingLeft: '24px',
                                fontSize: '0.95rem',
                                display: 'flex',
                                flexDirection: 'column',
                                gap: '6px'
                            }}>
                                {validationErrors.map((err, idx) => (
                                    <li key={idx}><strong>Violation:</strong> {err}</li>
                                ))}
                            </ul>
                        )}
                    </div>
                </div>
            )}
            {/* ------------------------------------------- */}

            {successMsg && (
                <div className="success-banner">
                    <CheckCircle2 size={20} />
                    <span>{successMsg.replace(/^[✅\s]+/, '')}</span>
                </div>
            )}

            {isGenerating && (
                <div className="generating-status-card large">
                    <Loader2 className="spinner" size={48} />
                    <div className="generating-info">
                        <h3>Optimizing Schedule...</h3>
                        <span>Running {strategy} algorithm to find the best possible plan.</span>
                    </div>
                </div>
            )}

            {!isGenerating && !scheduleResult && scheduledTasks.length === 0 && (
                <div className="empty-state-card">
                    <div className="empty-icon-wrapper">
                        <Sparkles size={40} />
                    </div>
                    <h2>No Schedule Generated</h2>
                    <p>Select a strategy and click Generate to start the optimization process.</p>
                    <div
                        className="strategy-info-box"
                        style={isDarkMode
                            ? { backgroundColor: 'rgba(59, 130, 246, 0.15)', borderColor: 'rgba(59, 130, 246, 0.3)' }
                            : { backgroundColor: 'rgba(59, 130, 246, 0.1)', borderColor: 'rgba(59, 130, 246, 0.2)' }
                        }
                    >
                        <Info size={20} className="strategy-icon" style={isDarkMode ? { color: '#60a5fa' } : { color: '#3b82f6' }} />
                        <div>
                            <strong style={{ display: 'block', color: isDarkMode ? '#60a5fa' : '#1e3a8a', marginBottom: '4px' }}>Algorithm Info</strong>
                            <span style={{ color: isDarkMode ? '#93c5fd' : '#1e40af', fontSize: '0.95rem', lineHeight: '1.4' }}>
                                A fast, straightforward approach that makes the optimal choice at each step, prioritizing immediate constraints without looking ahead.
                            </span>
                        </div>
                    </div>
                </div>
            )}

            {scheduleResult && (
                <div className="result-layout-vertical">
                    <div className="draft-preview-card">
                        <div className="card-header">
                            <div className="header-title-row">
                                <LayoutDashboard size={20} />
                                <h3>Draft Preview: {scheduleResult.strategyUsed}</h3>
                            </div>
                            {scheduleResult.strategyUsed === 'MEMETIC' && <span className="config-tag">Config ID: {selectedConfigId || 'Default'}</span>}
                        </div>

                        <div className="stats-grid">
                            <div className="stat-box">
                                <span className="stat-label">Total</span>
                                <span className="stat-number">{scheduleResult.totalTasks}</span>
                            </div>
                            <div className="stat-box success">
                                <span className="stat-label">Assigned</span>
                                <span className="stat-number">{scheduleResult.assignedTasks}</span>
                            </div>
                            <div className="stat-box warning">
                                <span className="stat-label">Unassigned</span>
                                <span className="stat-number">{scheduleResult.unassignedTasks}</span>
                            </div>
                        </div>

                        <div className="draft-actions">
                            <p>Review the assignments below before saving to calendars.</p>
                            <button className="btn-save-schedule" onClick={() => saveSchedule(mergedTasks)} disabled={isSaving}>
                                <Database size={18} /> {isSaving ? 'Saving...' : 'Approve & Save Schedule'}
                            </button>
                        </div>
                    </div>

                    {scheduleResult.strategyUsed === 'MEMETIC' && fitnessData && fitnessData.length > 0 && (
                        <div className="fitness-section">
                            <div className="fitness-header">
                                <LayoutDashboard size={18} />
                                <span>Convergence Analysis (Fitness Score)</span>
                            </div>
                            <FitnessChart data={fitnessData} />
                        </div>
                    )}
                </div>
            )}

            {(scheduledTasks.length > 0 || scheduleResult) && (
                <div className="schedule-content">
                    {viewMode === 'gantt' ? (
                        <ScheduleGantt tasks={scheduledTasks} users={users} assignmentMap={assignmentMap} onTaskClick={setSelectedGanttTask} />
                    ) : (
                        <ScheduleTable tasks={scheduledTasks} users={users} assignmentMap={assignmentMap} />
                    )}
                </div>
            )}

            {scheduleResult?.unscheduledTasks && <ScheduleExplainability failures={scheduleResult.unscheduledTasks} />}

            {isConfigModalOpen && (
                <SchedulingConfigurationModal configs={configs} isLoading={isConfigLoading} error={configError} onClose={closeConfigModal} initialConfigId={selectedConfigId} onSelectConfig={selectConfig} onCreateConfig={createConfig} />
            )}
        </div>
    )
}

export default Schedule
