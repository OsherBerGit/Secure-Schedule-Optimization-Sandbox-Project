import { useState, type FormEvent } from 'react';
import { X, CalendarDays, CheckCircle2 } from 'lucide-react';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import { format } from 'date-fns';
import type { CreateSettlementRequest, Task, User, Settlement } from '../types';
import { settlementApi } from '../api';
import './SettlementModal.css';

interface SettlementModalProps { tasks: Task[]; users: User[]; settlements: Settlement[]; onSuccess: () => void; onClose: () => void; }

const SettlementModal = ({ tasks: initialTasks, users: initialUsers, settlements, onSuccess, onClose }: SettlementModalProps) => {
    const [taskId, setTaskId] = useState<number | ''>('');
    const [userId, setUserId] = useState<number | ''>('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);
    const [selectedDate, setSelectedDate] = useState<Date | null>(null);

    const filteredUsers = initialUsers.filter(w => {
        const activeCount = settlements.filter(s => s.userId === w.id && s.statusName !== 'COMPLETED').length;
        if (w.maxTasks !== null && w.maxTasks !== undefined && activeCount >= w.maxTasks) return false;
        if (!taskId) return true;
        const task = initialTasks.find(t => t.id === taskId);
        if (!task || !task.requiredSkills) return true;
        const skillIds = w.skills?.map(s => s.id) || [];
        return task.requiredSkills.every(skill => skillIds.includes(skill.id));
    });

    const filteredTasks = initialTasks.filter(t => {
        if (settlements.some(s => s.taskId === t.id && s.statusName !== 'COMPLETED') || (t.taskStatusName && t.taskStatusName !== 'OPEN')) return false;
        if (!userId) return true;
        const user = initialUsers.find(w => w.id === userId);
        if (!user || !user.skills || !t.requiredSkills) return true;
        const skillIds = user.skills.map(s => s.id);
        return t.requiredSkills.every(skill => skillIds.includes(skill.id));
    });

    if (taskId && !filteredTasks.some(t => t.id === taskId)) setTaskId('');
    if (userId && !filteredUsers.some(w => w.id === userId)) setUserId('');

    async function handleSubmit(e: FormEvent) {
        e.preventDefault(); if (!taskId || !userId || !selectedDate) return;
        setIsSubmitting(true); setError(null); setSuccessMessage(null);
        try {
            await settlementApi.create({ taskId: Number(taskId), userId: Number(userId), settlementDate: format(selectedDate, "yyyy-MM-dd'T'HH:mm:00"), completionDate: undefined });
            setSuccessMessage('Settlement assigned successfully!');
            setTimeout(() => onSuccess(), 1500);
        } catch (err: unknown) { setError(err instanceof Error ? err.message : 'Failed to save'); setIsSubmitting(false); }
    }

    return (
        <div className="modal-overlay">
            <div className="modern-modal-card" style={{ maxWidth: '500px', width: '90%' }}>
                <div className="modal-header"><h2><CalendarDays size={22} className="text-primary" /> Add Settlement</h2><button type="button" className="modern-close-btn" onClick={onClose} disabled={isSubmitting}><X size={24} /></button></div>
                <form onSubmit={handleSubmit} className="modern-modal-form">
                    <div className="modal-body" style={{ padding: '2rem' }}>
                        {error && <div className="error-message" style={{ marginBottom: '1.5rem' }}>{error}</div>}
                        {successMessage && <div className="success-banner" style={{ marginBottom: '1.5rem' }}><CheckCircle2 size={20} />{successMessage}</div>}
                        <div className="form-grid" style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '1.5rem' }}>
                            <div className="modern-form-group"><label>Task *</label><select className="modern-input" value={taskId} onChange={e => setTaskId(Number(e.target.value) || '')} required disabled={isSubmitting || !!successMessage}><option value="">-- Select Task --</option>{filteredTasks.map(t => <option key={t.id} value={t.id}>{t.title}</option>)}</select></div>
                            <div className="modern-form-group"><label>User *</label><select className="modern-input" value={userId} onChange={e => setUserId(Number(e.target.value) || '')} required disabled={isSubmitting || !!successMessage}><option value="">-- Select User --</option>{filteredUsers.map(w => <option key={w.id} value={w.id}>{w.firstName} {w.lastName}</option>)}</select></div>
                            <div className="modern-form-group"><label>Scheduled Date & Time *</label><DatePicker className="modern-input" selected={selectedDate} onChange={setSelectedDate} showTimeSelect timeIntervals={15} dateFormat="Pp" portalId="root-portal" showIcon placeholderText="Select date and time" disabled={isSubmitting || !!successMessage} required popperClassName="settlement-datepicker-popper" /></div>
                        </div>
                    </div>
                    <div className="modal-actions"><button type="button" className="btn-cancel" onClick={onClose} disabled={isSubmitting}>Cancel</button><button type="submit" className="btn-submit" disabled={isSubmitting || !!successMessage || !selectedDate}>{isSubmitting ? 'Saving...' : 'Save Settlement'}</button></div>
                </form>
            </div>
        </div>
    );
};
export default SettlementModal;