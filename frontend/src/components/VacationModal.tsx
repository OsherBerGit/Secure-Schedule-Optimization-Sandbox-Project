import { useState } from 'react';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import { format } from 'date-fns';
import { X, Plane, AlertCircle } from 'lucide-react';
import type { Vacation, User } from '../types';
import './VacationModal.css';

interface VacationModalProps { vacation: Vacation | null; isAdmin: boolean; users: User[]; onSubmit: (data: any) => void; onClose: () => void; }

const VacationModal = ({ vacation, isAdmin, users, onSubmit, onClose }: VacationModalProps) => {
    const [userId, setUserId] = useState<number | ''>(vacation?.userId || '');
    const [startDate, setStartDate] = useState<Date | null>(vacation ? new Date(vacation.startDate) : null);
    const [endDate, setEndDate] = useState<Date | null>(vacation ? new Date(vacation.endDate) : null);
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        if (!startDate || !endDate) return setError('Please select both start and end dates.');
        if (isAdmin && !userId) return setError('Please select an employee.');
        if (startDate > endDate) return setError('Start date cannot be after end date.');

        onSubmit({
            userId: Number(userId),
            startDate: format(startDate, 'yyyy-MM-dd'),
            endDate: format(endDate, 'yyyy-MM-dd'),
            status: vacation?.statusName || 'PENDING'
        });
    };

    return (
        <div className="modal-overlay">
            <div className="modern-modal-card" style={{ maxWidth: '500px' }}>
                <div className="modal-header">
                    <h2><Plane size={22} className="text-primary" /> {vacation ? 'Edit Vacation' : 'New Vacation Request'}</h2>
                    <button type="button" className="modern-close-btn" onClick={onClose}><X size={24} /></button>
                </div>
                <form onSubmit={handleSubmit} className="modern-modal-form">
                    <div className="modal-body">
                        {error && <div className="error-message" style={{ marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}><AlertCircle size={16}/>{error}</div>}

                        {isAdmin && (
                            <div className="modern-form-group">
                                <label>Select Employee *</label>
                                <select className="modern-input" value={userId} onChange={e => setUserId(Number(e.target.value))} required>
                                    <option value="">-- Select User --</option>
                                    {users.map(w => <option key={w.id} value={w.id}>{w.firstName} {w.lastName}</option>)}
                                </select>
                            </div>
                        )}

                        <div className="dates-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginTop: '1rem' }}>
                            <div className="modern-form-group">
                                <label>Start Date *</label>
                                <DatePicker 
                                    className="modern-input" 
                                    selected={startDate} 
                                    onChange={d => { setStartDate(d); if (d && endDate && d > endDate) setEndDate(null); }} 
                                    dateFormat="yyyy-MM-dd" 
                                    portalId="root-portal" 
                                    popperPlacement="bottom-start" 
                                    minDate={new Date()} 
                                    showIcon 
                                    placeholderText="Start" 
                                    required 
                                    popperClassName="vacation-datepicker-popper"
                                />
                            </div>
                            <div className="modern-form-group">
                                <label>End Date *</label>
                                <DatePicker 
                                    className="modern-input" 
                                    selected={endDate} 
                                    onChange={setEndDate} 
                                    minDate={startDate || new Date()} 
                                    dateFormat="yyyy-MM-dd" 
                                    portalId="root-portal" 
                                    popperPlacement="bottom-start" 
                                    showIcon 
                                    placeholderText="End" 
                                    required 
                                    popperClassName="vacation-datepicker-popper"
                                />
                            </div>
                        </div>
                    </div>
                    <div className="modal-actions">
                        <button type="button" className="btn-cancel" onClick={onClose}>Cancel</button>
                        <button type="submit" className="btn-submit" disabled={!startDate || !endDate}>
                            {vacation ? 'Update Record' : 'Submit Request'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default VacationModal;