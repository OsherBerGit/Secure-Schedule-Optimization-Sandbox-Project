import { useState, useEffect, type FormEvent } from 'react';
import type { Task, Department, Skill, Status, Priority, CreateTaskRequest, UpdateTaskRequest } from '../types';
import { X, FileText } from 'lucide-react';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import './TaskModal.css';

interface TaskModalProps { task: Task | null; departments: Department[]; skills: Skill[]; statuses?: Status[]; priorities?: Priority[]; onSubmit: (data: CreateTaskRequest | UpdateTaskRequest) => Promise<void> | void; onClose: () => void; }

const TaskModal = ({ task, departments, skills, statuses = [], priorities = [], onSubmit, onClose }: TaskModalProps) => {
    const [title, setTitle] = useState(task?.title ?? '');
    const [description, setDescription] = useState(task?.description ?? '');
    const [deadline, setDeadline] = useState<Date | null>(task?.deadline ? new Date(task.deadline) : null);
    const [durationHours, setDurationHours] = useState<number | ''>(task?.durationHours ?? 1);
    const [priorityId, setPriorityId] = useState<number | string>(task?.priorityId ?? '');
    const [statusId, setStatusId] = useState<number | string>(task?.taskStatusId ?? '');
    const [departmentId, setDepartmentId] = useState<number | string>(task?.departmentName ? departments.find(d => d.name === task.departmentName)?.id ?? '' : '');
    const [requiredSkills, setRequiredSkills] = useState<number[]>([]);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [errorMsg, setErrorMsg] = useState<string | null>(null);

    useEffect(() => { setRequiredSkills(task?.requiredSkills ? task.requiredSkills.map(s => s.id) : []); }, [task]);
    useEffect(() => {
        if (!task) {
            if (priorities.length > 0 && priorityId === '') setPriorityId(priorities[0].id);
            if (statuses.length > 0 && statusId === '') { const open = statuses.find(s => s.name === 'OPEN'); if (open) setStatusId(open.id); }
        }
    }, [priorities, statuses, task, priorityId, statusId]);

    function handleSkillChange(id: number) { setRequiredSkills(prev => prev.includes(id) ? prev.filter(s => s !== id) : [...prev, id]); }

    async function handleSubmit(e: FormEvent) {
        e.preventDefault(); setErrorMsg(null); setIsSubmitting(true);
        const formatLocal = (d: Date) => { const pad = (n: number) => n.toString().padStart(2, '0'); return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`; };
        const data: CreateTaskRequest | UpdateTaskRequest = { title, description: description || undefined, deadline: deadline ? formatLocal(deadline) : undefined, durationHours: durationHours !== '' ? Number(durationHours) : undefined, priorityId: Number(priorityId), departmentId: departmentId !== '' ? Number(departmentId) : undefined, requiredSkills: [...requiredSkills] };
        if (task) (data as UpdateTaskRequest).statusId = Number(statusId);
        try { const res = onSubmit(data); if (res instanceof Promise) await res; }
        catch (err: any) { setErrorMsg(err?.response?.data?.message || err.message || 'Request failed.'); }
        finally { setIsSubmitting(false); }
    }

    const allowedStatuses = statuses.filter(s => ['OPEN', 'LOCKED'].includes(s.name));

    return (
        <div className="modal-overlay">
            <div className="task-modal-card">
                <div className="modal-header"><h2><FileText size={22} className="text-primary" /> {task ? 'Edit Task' : 'Add New Task'}</h2><button type="button" className="modern-close-btn" onClick={onClose}><X size={24} /></button></div>
                <form onSubmit={handleSubmit} className="task-modal-form">
                    <div className="task-modal-body">
                        {errorMsg && <div className="error-message" style={{ marginBottom: '1.5rem' }}>{errorMsg}</div>}
                        <div className="form-grid">
                            <div className="modern-form-group full-width"><label>Title</label><input type="text" className="modern-input" value={title} onChange={e => setTitle(e.target.value)} placeholder="Task title" required /></div>
                            <div className="modern-form-group full-width"><label>Description</label><textarea className="modern-input" value={description} onChange={e => setDescription(e.target.value)} placeholder="Task details..." rows={4} style={{ resize: 'none', overflowY: 'auto', minHeight: '120px', paddingTop: '12px', paddingBottom: '12px', lineHeight: '1.5' }} /></div>
                            <div className="modern-form-group"><label>Deadline</label><DatePicker selected={deadline} onChange={setDeadline} showTimeSelect timeIntervals={15} dateFormat="Pp" portalId="root-portal" popperPlacement="bottom-start" className="modern-input" calendarClassName="task-calendar" showIcon={true} placeholderText="Select Deadline..." shouldCloseOnSelect={false} popperClassName="task-datepicker-popper" /></div>
                            <div className="modern-form-group"><label>Duration (Hours)</label><input type="number" className="modern-input" value={durationHours} onChange={e => setDurationHours(e.target.value === '' ? '' : Number(e.target.value))} step={0.5} /></div>
                            <div className="modern-form-group"><label>Department</label><select className="modern-input" value={departmentId} onChange={e => setDepartmentId(e.target.value)}><option value="">- Unassigned -</option>{departments.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}</select></div>
                            <div className="modern-form-group"><label>Priority</label><select className="modern-input" value={priorityId} onChange={e => setPriorityId(e.target.value)} required>{priorities.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</select></div>
                            {task && <div className="modern-form-group full-width"><label>Status</label><select className="modern-input" value={statusId} onChange={e => setStatusId(e.target.value)} required>{allowedStatuses.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}{!allowedStatuses.find(s => s.id === Number(statusId)) && <option value={task.taskStatusId ?? ''}>{task.taskStatusName}</option>}</select></div>}

                            <div className="modern-form-group full-width" style={{ marginTop: '0.5rem' }}>
                                <label style={{ marginBottom: '0.5rem', display: 'block' }}>Required Skills</label>
                                <div className="skills-grid" style={{ background: 'var(--bg-main)', padding: '1.25rem', borderRadius: '0.75rem', border: '1px solid var(--border-color)' }}>
                                    {skills.map(s => (
                                        <div key={s.id} style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}><input type="checkbox" id={`s-${s.id}`} checked={requiredSkills.includes(s.id)} onChange={() => handleSkillChange(s.id)} style={{ width: '18px', height: '18px', cursor: 'pointer' }} /><label htmlFor={`s-${s.id}`} style={{ fontSize: '0.95rem', cursor: 'pointer', color: 'var(--text-primary)' }}>{s.name}</label></div>
                                    ))}
                                </div>
                            </div>
                        </div>
                    </div>
                    <div className="modal-actions"><button type="button" className="btn-cancel" onClick={onClose}>Cancel</button><button type="submit" className="btn-submit" disabled={isSubmitting}>{isSubmitting ? 'Saving...' : 'Save Task'}</button></div>
                </form>
            </div>
        </div>
    );
};

export default TaskModal;