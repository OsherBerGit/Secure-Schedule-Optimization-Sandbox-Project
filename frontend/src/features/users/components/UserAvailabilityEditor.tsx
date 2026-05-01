import React from "react";
import DatePicker from "react-datepicker";
import { Trash2 } from "lucide-react";
import type { UserAvailability } from "../../../types";
import "react-datepicker/dist/react-datepicker.css";

export const DAYS_OF_WEEK: UserAvailability["dayOfWeek"][] = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];

interface UserAvailabilityEditorProps {
    availabilities: UserAvailability[];
    onChange: (index: number, field: keyof UserAvailability, value: string) => void;
    onAdd: () => void;
    onRemove: (index: number) => void;
    disabled?: boolean;
    hideHeader?: boolean;
}

const timeStringToDate = (timeStr: string): Date | null => {
    if (!timeStr) return null;
    const [h, m] = timeStr.split(":").map(Number);
    const d = new Date();
    d.setHours(h, m, 0, 0);
    return d;
};

const UserAvailabilityEditor: React.FC<UserAvailabilityEditorProps> = ({ availabilities, onChange, onAdd, onRemove, disabled = false, hideHeader = false }) => {
    const handleTimeChange = (index: number, field: "startTime" | "endTime", date: Date | null) => {
        if (date) {
            const timeString = `${date.getHours().toString().padStart(2, "0")}:${date.getMinutes().toString().padStart(2, "0")}:00`;
            onChange(index, field, timeString);
        }
    };

    return (
        <div className={`availability-section ${hideHeader ? "" : "full-width-span top-margin"}`}>
            {!hideHeader && (
                <div className="availability-header">
                    <h3 className="section-title">User Availability</h3>
                    <button type="button" className="btn-outline-primary" onClick={onAdd} disabled={disabled}>
                        Add Shift
                    </button>
                </div>
            )}
            {availabilities.map((row, i) => (
                <div key={i} className="availability-row">
                    <select
                        className="modern-input compact-select"
                        value={row.dayOfWeek}
                        onChange={e => onChange(i, "dayOfWeek", e.target.value as UserAvailability["dayOfWeek"])}
                        disabled={disabled}>
                        {DAYS_OF_WEEK.map(day => (
                            <option key={day} value={day}>
                                {day}
                            </option>
                        ))}
                    </select>
                    <div className="time-picker-wrapper">
                        <DatePicker
                            selected={timeStringToDate(row.startTime)}
                            onChange={(date: Date | null) => handleTimeChange(i, "startTime", date)}
                            showTimeSelect
                            showTimeSelectOnly
                            timeIntervals={15}
                            dateFormat="HH:mm"
                            className="modern-input"
                            portalId="root-portal"
                            popperPlacement="bottom-start"
                            placeholderText="Start"
                            required
                            disabled={disabled}
                        />
                    </div>
                    <div className="time-picker-wrapper">
                        <DatePicker
                            selected={timeStringToDate(row.endTime)}
                            onChange={(date: Date | null) => handleTimeChange(i, "endTime", date)}
                            showTimeSelect
                            showTimeSelectOnly
                            timeIntervals={15}
                            dateFormat="HH:mm"
                            className="modern-input"
                            portalId="root-portal"
                            popperPlacement="bottom-start"
                            placeholderText="End"
                            required
                            disabled={disabled}
                        />
                    </div>
                    <button type="button" className="btn-icon delete-btn" onClick={() => onRemove(i)} title="Remove shift" disabled={disabled}>
                        <Trash2 size={18} />
                    </button>
                </div>
            ))}
            {availabilities.length === 0 && <div className="empty-state-banner">No availability defined. Click "Add Shift" to set working hours.</div>}
        </div>
    );
};

export default UserAvailabilityEditor;
