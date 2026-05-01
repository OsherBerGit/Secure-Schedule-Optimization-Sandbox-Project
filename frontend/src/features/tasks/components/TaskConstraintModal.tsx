import { useState, type FormEvent, type ChangeEvent } from "react";
import { X, GitMerge } from "lucide-react";
import type { Task, ConstraintType } from "../../../types";

interface TaskConstraintModalProps {
    tasks: Task[];
    constraintTypes: ConstraintType[];
    onSubmit: (data: { predecessorTaskId: number; successorTaskId: number; constraintTypeId: number; lagMinutes?: number }) => void;
    onClose: () => void;
}

interface ConstraintFormData {
    predecessorTaskId: number | "";
    successorTaskId: number | "";
    constraintTypeId: number | "";
    lagMinutes: number | "";
}

const TaskConstraintModal = ({ tasks, constraintTypes, onSubmit, onClose }: TaskConstraintModalProps) => {
    const [formData, setFormData] = useState<ConstraintFormData>({
        predecessorTaskId: "",
        successorTaskId: "",
        constraintTypeId: "",
        lagMinutes: ""
    });
    const [error, setError] = useState<string | null>(null);

    const handleChange = (e: ChangeEvent<HTMLSelectElement | HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value === "" ? "" : Number(value)
        }));
    };

    function handleSubmit(e: FormEvent) {
        e.preventDefault();
        setError(null);

        if (formData.predecessorTaskId === "" || formData.successorTaskId === "" || formData.constraintTypeId === "") {
            setError("Please fill all required fields");
            return;
        }

        if (formData.predecessorTaskId === formData.successorTaskId) {
            setError("Predecessor and successor tasks cannot be the same");
            return;
        }

        onSubmit({
            predecessorTaskId: Number(formData.predecessorTaskId),
            successorTaskId: Number(formData.successorTaskId),
            constraintTypeId: Number(formData.constraintTypeId),
            lagMinutes: formData.lagMinutes !== "" ? Number(formData.lagMinutes) : undefined
        });
    }

    return (
        <div className="modal-overlay">
            <div className="modern-modal-card responsive-modal">
                <div className="modal-header">
                    <h2>
                        <GitMerge size={22} className="text-primary" /> Add Task Constraint
                    </h2>
                    <button type="button" className="modern-close-btn" onClick={onClose}>
                        <X size={24} />
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="modern-modal-form">
                    <div className="modal-body padded-body">
                        {error && <div className="error-message banner-spacing">{error}</div>}

                        <div className="form-grid default-gap">
                            <div className="modern-form-group">
                                <label>Predecessor Task *</label>
                                <select className="modern-input" name="predecessorTaskId" value={formData.predecessorTaskId} onChange={handleChange} required>
                                    <option value="" disabled>
                                        -- Select First Task --
                                    </option>
                                    {tasks.map(task => (
                                        <option key={task.id} value={task.id}>
                                            {task.title}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div className="modern-form-group">
                                <label>Successor Task *</label>
                                <select className="modern-input" name="successorTaskId" value={formData.successorTaskId} onChange={handleChange} required>
                                    <option value="" disabled>
                                        -- Select Dependent Task --
                                    </option>
                                    {tasks.map(task => (
                                        <option key={task.id} value={task.id}>
                                            {task.title}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div className="modern-form-group">
                                <label>Constraint Type *</label>
                                <select className="modern-input" name="constraintTypeId" value={formData.constraintTypeId} onChange={handleChange} required>
                                    <option value="" disabled>
                                        -- Select Rule --
                                    </option>
                                    {constraintTypes.map(constraintType => (
                                        <option key={constraintType.id} value={constraintType.id}>
                                            {constraintType.name}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div className="modern-form-group">
                                <label>Lag (Minutes)</label>
                                <input
                                    type="number"
                                    className="modern-input"
                                    name="lagMinutes"
                                    min={0}
                                    placeholder="e.g. 60 (Optional delay)"
                                    value={formData.lagMinutes}
                                    onChange={handleChange}
                                />
                            </div>
                        </div>
                    </div>

                    <div className="modal-actions modal-actions-footer">
                        <button type="button" className="btn-cancel" onClick={onClose}>
                            Cancel
                        </button>
                        <button type="submit" className="btn-submit">
                            Create Constraint
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default TaskConstraintModal;
