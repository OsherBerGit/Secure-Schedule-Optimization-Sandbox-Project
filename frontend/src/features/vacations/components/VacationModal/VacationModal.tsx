import { useState, type FormEvent } from "react";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { format } from "date-fns";
import { X, Plane, AlertCircle } from "lucide-react";
import type { Vacation, User } from "../../../../types";
import "./VacationModal.css";

export interface VacationRequestData {
    userId: number;
    startDate: string;
    endDate: string;
    status: string;
}

interface VacationModalProps {
    vacation: Vacation | null;
    isAdmin: boolean;
    users: User[];
    onSubmit: (data: VacationRequestData) => void;
    onClose: () => void;
}

interface VacationFormData {
    userId: number | "";
    startDate: Date | null;
    endDate: Date | null;
}

const VacationModal = ({ vacation, isAdmin, users, onSubmit, onClose }: VacationModalProps) => {
    const [formData, setFormData] = useState<VacationFormData>({
        userId: vacation?.userId || "",
        startDate: vacation ? new Date(vacation.startDate) : null,
        endDate: vacation ? new Date(vacation.endDate) : null
    });

    const [error, setError] = useState<string | null>(null);

    const handleSubmit = (e: FormEvent) => {
        e.preventDefault();
        setError(null);

        if (!formData.startDate || !formData.endDate) {
            setError("Please select both start and end dates.");
            return;
        }

        if (isAdmin && !formData.userId) {
            setError("Please select an employee.");
            return;
        }

        if (formData.startDate > formData.endDate) {
            setError("Start date cannot be after end date.");
            return;
        }

        const requestData: VacationRequestData = {
            userId: Number(formData.userId),
            startDate: format(formData.startDate, "yyyy-MM-dd"),
            endDate: format(formData.endDate, "yyyy-MM-dd"),
            status: vacation?.statusName || "PENDING"
        };

        onSubmit(requestData);
    };

    return (
        <div className="modal-overlay">
            <div className="modern-modal-card responsive-modal">
                <div className="modal-header">
                    <h2>
                        <Plane size={22} className="text-primary" /> {vacation ? "Edit Vacation" : "New Vacation Request"}
                    </h2>
                    <button type="button" className="modern-close-btn" onClick={onClose}>
                        <X size={24} />
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="modern-modal-form">
                    <div className="modal-body padded-body">
                        {error && (
                            <div className="error-banner banner-spacing flex-center">
                                <AlertCircle size={16} />
                                <span>{error}</span>
                            </div>
                        )}

                        {isAdmin && (
                            <div className="modern-form-group banner-spacing">
                                <label>Select Employee *</label>
                                <select
                                    className="modern-input"
                                    value={formData.userId}
                                    onChange={e =>
                                        setFormData(prev => ({
                                            ...prev,
                                            userId: e.target.value ? Number(e.target.value) : ""
                                        }))
                                    }
                                    required>
                                    <option value="" disabled>
                                        -- Select User --
                                    </option>
                                    {users.map(user => (
                                        <option key={user.id} value={user.id}>
                                            {user.firstName} {user.lastName}
                                        </option>
                                    ))}
                                </select>
                            </div>
                        )}

                        <div className="form-grid default-gap dates-grid">
                            <div className="modern-form-group">
                                <label>Start Date *</label>
                                <DatePicker
                                    className="modern-input"
                                    selected={formData.startDate}
                                    onChange={(date: Date | null) => {
                                        setFormData(prev => {
                                            const newEndDate = date && prev.endDate && date > prev.endDate ? null : prev.endDate;
                                            return { ...prev, startDate: date, endDate: newEndDate };
                                        });
                                    }}
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
                                    selected={formData.endDate}
                                    onChange={(date: Date | null) => setFormData(prev => ({ ...prev, endDate: date }))}
                                    minDate={formData.startDate || new Date()}
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

                    <div className="modal-actions modal-actions-footer">
                        <button type="button" className="btn-cancel" onClick={onClose}>
                            Cancel
                        </button>
                        <button type="submit" className="btn-submit" disabled={!formData.startDate || !formData.endDate}>
                            {vacation ? "Update Record" : "Submit Request"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default VacationModal;
