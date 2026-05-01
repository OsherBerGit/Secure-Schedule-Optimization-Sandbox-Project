import { useState, type FormEvent, useMemo } from "react";
import { X, CalendarDays, CheckCircle2 } from "lucide-react";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { format } from "date-fns";
import type { Task, User, Settlement } from "../../../../types";
import { settlementApi } from "../../../../api";
import "./SettlementModal.css";

interface SettlementModalProps {
    tasks: Task[];
    users: User[];
    settlements: Settlement[];
    onSuccess: () => void;
    onClose: () => void;
}

const checkUserEligibility = (user: User, activeSettlements: Settlement[], selectedTaskId: number | "", allTasks: Task[]): boolean => {
    const activeCount = activeSettlements.filter(settlement => settlement.userId === user.id && settlement.statusName !== "COMPLETED").length;

    if (user.maxTasks != null && activeCount >= user.maxTasks) return false;

    if (!selectedTaskId) return true;

    const task = allTasks.find(t => t.id === selectedTaskId);
    if (!task || !task.requiredSkills?.length) return true;

    const userSkillIds = user.skills?.map(skill => skill.id) || [];
    return task.requiredSkills.every(reqSkill => userSkillIds.includes(reqSkill.id));
};

const checkTaskEligibility = (task: Task, activeSettlements: Settlement[], selectedUserId: number | "", allUsers: User[]): boolean => {
    const isTaskTaken = activeSettlements.some(settlement => settlement.taskId === task.id && settlement.statusName !== "COMPLETED");

    if (isTaskTaken || (task.taskStatusName && task.taskStatusName !== "OPEN")) return false;

    if (!selectedUserId) return true;

    const user = allUsers.find(u => u.id === selectedUserId);
    if (!user || !user.skills || !task.requiredSkills?.length) return true;

    const userSkillIds = user.skills.map(skill => skill.id);
    return task.requiredSkills.every(reqSkill => userSkillIds.includes(reqSkill.id));
};

const SettlementModal = ({ tasks: initialTasks, users: initialUsers, settlements, onSuccess, onClose }: SettlementModalProps) => {
    const [taskId, setTaskId] = useState<number | "">("");
    const [userId, setUserId] = useState<number | "">("");
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);
    const [selectedDate, setSelectedDate] = useState<Date | null>(null);

    const filteredUsers = useMemo(
        () => initialUsers.filter(user => checkUserEligibility(user, settlements, taskId, initialTasks)),
        [initialUsers, settlements, taskId, initialTasks]
    );

    const filteredTasks = useMemo(
        () => initialTasks.filter(task => checkTaskEligibility(task, settlements, userId, initialUsers)),
        [initialTasks, settlements, userId, initialUsers]
    );

    if (taskId && !filteredTasks.some(task => task.id === taskId)) setTaskId("");

    if (userId && !filteredUsers.some(user => user.id === userId)) setUserId("");

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        if (!taskId || !userId || !selectedDate) return;

        setIsSubmitting(true);
        setError(null);
        setSuccessMessage(null);

        try {
            await settlementApi.create({
                taskId: Number(taskId),
                userId: Number(userId),
                settlementDate: format(selectedDate, "yyyy-MM-dd'T'HH:mm:00"),
                completionDate: undefined
            });
            setSuccessMessage("Settlement assigned successfully!");
            setTimeout(() => onSuccess(), 1500);
        } catch (err: unknown) {
            setError(err instanceof Error ? err.message : "Failed to save");
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="modal-overlay">
            <div className="modern-modal-card responsive-modal">
                <div className="modal-header">
                    <h2>
                        <CalendarDays size={22} className="text-primary" /> Add Settlement
                    </h2>
                    <button type="button" className="modern-close-btn" onClick={onClose} disabled={isSubmitting}>
                        <X size={24} />
                    </button>
                </div>
                <form onSubmit={handleSubmit} className="modern-modal-form">
                    <div className="modal-body padded-body">
                        {error && <div className="error-message banner-spacing">{error}</div>}
                        {successMessage && (
                            <div className="success-banner banner-spacing">
                                <CheckCircle2 size={20} />
                                {successMessage}
                            </div>
                        )}
                        <div className="form-grid default-gap">
                            <div className="modern-form-group">
                                <label>Task *</label>
                                <select
                                    className="modern-input"
                                    value={taskId}
                                    onChange={e => setTaskId(Number(e.target.value) || "")}
                                    required
                                    disabled={isSubmitting || !!successMessage}>
                                    <option value="">-- Select Task --</option>
                                    {filteredTasks.map(task => (
                                        <option key={task.id} value={task.id}>
                                            {task.title}
                                        </option>
                                    ))}
                                </select>
                            </div>
                            <div className="modern-form-group">
                                <label>User *</label>
                                <select
                                    className="modern-input"
                                    value={userId}
                                    onChange={e => setUserId(Number(e.target.value) || "")}
                                    required
                                    disabled={isSubmitting || !!successMessage}>
                                    <option value="">-- Select User --</option>
                                    {filteredUsers.map(user => (
                                        <option key={user.id} value={user.id}>
                                            {user.firstName} {user.lastName}
                                        </option>
                                    ))}
                                </select>
                            </div>
                            <div className="modern-form-group">
                                <label>Scheduled Date & Time *</label>
                                <DatePicker
                                    className="modern-input"
                                    selected={selectedDate}
                                    onChange={setSelectedDate}
                                    showTimeSelect
                                    timeIntervals={15}
                                    dateFormat="Pp"
                                    portalId="root-portal"
                                    showIcon
                                    placeholderText="Select date and time"
                                    disabled={isSubmitting || !!successMessage}
                                    required
                                    popperClassName="settlement-datepicker-popper"
                                />
                            </div>
                        </div>
                    </div>
                    <div className="modal-actions">
                        <button type="button" className="btn-cancel" onClick={onClose} disabled={isSubmitting}>
                            Cancel
                        </button>
                        <button type="submit" className="btn-submit" disabled={isSubmitting || !!successMessage || !selectedDate}>
                            {isSubmitting ? "Saving..." : "Save Settlement"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default SettlementModal;
