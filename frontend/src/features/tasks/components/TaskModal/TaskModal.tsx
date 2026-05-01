import { useState, useEffect, type FormEvent, type ChangeEvent } from "react";
import type { Task, Department, Skill, Status, Priority, CreateTaskRequest, UpdateTaskRequest } from "../../../../types";
import { X, FileText } from "lucide-react";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import "./TaskModal.css";

interface TaskModalProps {
    task: Task | null;
    departments: Department[];
    skills: Skill[];
    statuses?: Status[];
    priorities?: Priority[];
    onSubmit: (data: CreateTaskRequest | UpdateTaskRequest) => Promise<void> | void;
    onClose: () => void;
}

interface TaskFormData {
    title: string;
    description: string;
    deadline: Date | null;
    durationHours: number | "";
    priorityId: number | string;
    statusId: number | string;
    departmentId: number | string;
    requiredSkills: number[];
}

const formatLocal = (d: Date) => {
    const pad = (n: number) => n.toString().padStart(2, "0");
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

const TaskModal = ({ task, departments, skills, statuses = [], priorities = [], onSubmit, onClose }: TaskModalProps) => {
    const [formData, setFormData] = useState<TaskFormData>({
        title: task?.title ?? "",
        description: task?.description ?? "",
        deadline: task?.deadline ? new Date(task.deadline) : null,
        durationHours: task?.durationHours ?? 1,
        priorityId: task?.priorityId ?? "",
        statusId: task?.taskStatusId ?? "",
        departmentId: task?.departmentName ? (departments.find(department => department.name === task.departmentName)?.id ?? "") : "",
        requiredSkills: task?.requiredSkills ? task.requiredSkills.map(skill => skill.id) : []
    });

    const [isSubmitting, setIsSubmitting] = useState(false);
    const [errorMsg, setErrorMsg] = useState<string | null>(null);

    useEffect(() => {
        if (!task) {
            setFormData(prev => {
                let newPriorityId = prev.priorityId;
                let newStatusId = prev.statusId;

                if (priorities.length > 0 && prev.priorityId === "") newPriorityId = priorities[0].id;

                if (statuses.length > 0 && prev.statusId === "") {
                    const openStatus = statuses.find(status => status.name === "OPEN");
                    if (openStatus) newStatusId = openStatus.id;
                }

                if (newPriorityId !== prev.priorityId || newStatusId !== prev.statusId) return { ...prev, priorityId: newPriorityId, statusId: newStatusId };

                return prev;
            });
        }
    }, [priorities, statuses, task]);

    const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
        const { name, value, type } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: type === "number" ? (value === "" ? "" : Number(value)) : value
        }));
    };

    const handleSkillChange = (skillId: number) => {
        setFormData(prev => ({
            ...prev,
            requiredSkills: prev.requiredSkills.includes(skillId) ? prev.requiredSkills.filter(id => id !== skillId) : [...prev.requiredSkills, skillId]
        }));
    };

    const handleDateChange = (date: Date | null) => {
        setFormData(prev => ({ ...prev, deadline: date }));
    };

    async function handleSubmit(e: FormEvent) {
        e.preventDefault();
        setErrorMsg(null);
        setIsSubmitting(true);

        const data: CreateTaskRequest | UpdateTaskRequest = {
            title: formData.title,
            description: formData.description || undefined,
            deadline: formData.deadline ? formatLocal(formData.deadline) : undefined,
            durationHours: formData.durationHours !== "" ? Number(formData.durationHours) : undefined,
            priorityId: Number(formData.priorityId),
            departmentId: formData.departmentId !== "" ? Number(formData.departmentId) : undefined,
            requiredSkills: [...formData.requiredSkills]
        };

        if (task) (data as UpdateTaskRequest).statusId = Number(formData.statusId);

        try {
            const res = onSubmit(data);
            if (res instanceof Promise) await res;
        } catch (err: unknown) {
            const errorMessage = err instanceof Error ? err.message : "Request failed.";
            setErrorMsg(errorMessage);
        } finally {
            setIsSubmitting(false);
        }
    }

    const allowedStatuses = statuses.filter(status => ["OPEN", "LOCKED"].includes(status.name));

    return (
        <div className="modal-overlay">
            <div className="task-modal-card">
                <div className="modal-header">
                    <h2>
                        <FileText size={22} className="text-primary" /> {task ? "Edit Task" : "Add New Task"}
                    </h2>
                    <button type="button" className="modern-close-btn" onClick={onClose}>
                        <X size={24} />
                    </button>
                </div>
                <form onSubmit={handleSubmit} className="task-modal-form">
                    <div className="task-modal-body">
                        {errorMsg && <div className="error-message banner-spacing">{errorMsg}</div>}
                        <div className="form-grid">
                            <div className="modern-form-group full-width">
                                <label>Title</label>
                                <input
                                    type="text"
                                    name="title"
                                    className="modern-input"
                                    value={formData.title}
                                    onChange={handleChange}
                                    placeholder="Task title"
                                    required
                                />
                            </div>
                            <div className="modern-form-group full-width">
                                <label>Description</label>
                                <textarea
                                    name="description"
                                    className="modern-input modern-textarea"
                                    value={formData.description}
                                    onChange={handleChange}
                                    placeholder="Task details..."
                                    rows={4}
                                />
                            </div>
                            <div className="modern-form-group">
                                <label>Deadline</label>
                                <DatePicker
                                    selected={formData.deadline}
                                    onChange={handleDateChange}
                                    showTimeSelect
                                    timeIntervals={15}
                                    dateFormat="Pp"
                                    portalId="root-portal"
                                    popperPlacement="bottom-start"
                                    className="modern-input"
                                    calendarClassName="task-calendar"
                                    showIcon={true}
                                    placeholderText="Select Deadline..."
                                    shouldCloseOnSelect={false}
                                    popperClassName="task-datepicker-popper"
                                />
                            </div>
                            <div className="modern-form-group">
                                <label>Duration (Hours)</label>
                                <input
                                    type="number"
                                    name="durationHours"
                                    className="modern-input"
                                    value={formData.durationHours}
                                    onChange={handleChange}
                                    step={0.5}
                                />
                            </div>
                            <div className="modern-form-group">
                                <label>Department</label>
                                <select name="departmentId" className="modern-input" value={formData.departmentId} onChange={handleChange}>
                                    <option value="">- Unassigned -</option>
                                    {departments.map(department => (
                                        <option key={department.id} value={department.id}>
                                            {department.name}
                                        </option>
                                    ))}
                                </select>
                            </div>
                            <div className="modern-form-group">
                                <label>Priority</label>
                                <select name="priorityId" className="modern-input" value={formData.priorityId} onChange={handleChange} required>
                                    {priorities.map(priority => (
                                        <option key={priority.id} value={priority.id}>
                                            {priority.name}
                                        </option>
                                    ))}
                                </select>
                            </div>
                            {task && (
                                <div className="modern-form-group full-width">
                                    <label>Status</label>
                                    <select name="statusId" className="modern-input" value={formData.statusId} onChange={handleChange} required>
                                        {allowedStatuses.map(status => (
                                            <option key={status.id} value={status.id}>
                                                {status.name}
                                            </option>
                                        ))}
                                        {!allowedStatuses.find(status => status.id === Number(formData.statusId)) && (
                                            <option value={task.taskStatusId ?? ""}>{task.taskStatusName}</option>
                                        )}
                                    </select>
                                </div>
                            )}

                            <div className="modern-form-group full-width top-margin">
                                <span className="modal-section-title">Required Skills</span>
                                <div className="skills-container-wrapper">
                                    <div className="skills-selection-grid">
                                        {skills.map(skill => (
                                            <label key={skill.id} className="skill-checkbox-item">
                                                <input
                                                    type="checkbox"
                                                    checked={formData.requiredSkills.includes(skill.id)}
                                                    onChange={() => handleSkillChange(skill.id)}
                                                />
                                                <span>{skill.name}</span>
                                            </label>
                                        ))}
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div className="modal-actions">
                        <button type="button" className="btn-cancel" onClick={onClose}>
                            Cancel
                        </button>
                        <button type="submit" className="btn-submit" disabled={isSubmitting}>
                            {isSubmitting ? "Saving..." : "Save Task"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default TaskModal;
