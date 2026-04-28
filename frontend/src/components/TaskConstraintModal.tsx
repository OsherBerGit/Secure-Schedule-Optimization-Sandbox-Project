import { useState } from "react";
import type { FormEvent } from "react";
import { X, GitMerge } from "lucide-react";
import type { Task, ConstraintType } from "../types";

interface TaskConstraintModalProps {
    tasks: Task[];
    constraintTypes: ConstraintType[];
    onSubmit: (data: {
        predecessorTaskId: number;
        successorTaskId: number;
        constraintTypeId: number;
        lagMinutes?: number;
    }) => void;
    onClose: () => void;
}

const TaskConstraintModal = ({
    tasks,
    constraintTypes,
    onSubmit,
    onClose,
}: TaskConstraintModalProps) => {
    const [predId, setPredId] = useState<number | "">("");
    const [succId, setSuccId] = useState<number | "">("");
    const [typeId, setTypeId] = useState<number | "">("");
    const [lag, setLag] = useState<number | "">("");
    const [error, setError] = useState<string | null>(null);

    function handleSubmit(e: FormEvent) {
        e.preventDefault();
        setError(null);
        if (predId === "" || succId === "" || typeId === "") {
            setError("Please fill all required fields");
            return;
        }
        if (predId === succId) {
            setError("Predecessor and successor tasks cannot be the same");
            return;
        }
        onSubmit({
            predecessorTaskId: Number(predId),
            successorTaskId: Number(succId),
            constraintTypeId: Number(typeId),
            lagMinutes: lag !== "" ? Number(lag) : undefined,
        });
    }

    return (
        <div className="modal-overlay">
            <div
                className="modern-modal-card"
                style={{ maxWidth: "500px", width: "90%" }}
            >
                <div className="modal-header">
                    <h2>
                        <GitMerge size={22} className="text-primary" /> Add Task
                        Constraint
                    </h2>
                    <button
                        type="button"
                        className="modern-close-btn"
                        onClick={onClose}
                    >
                        <X size={24} />
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="modern-modal-form">
                    <div className="modal-body" style={{ padding: "2rem" }}>
                        {error && (
                            <div
                                className="error-message"
                                style={{ marginBottom: "1.5rem" }}
                            >
                                {error}
                            </div>
                        )}

                        <div
                            className="form-grid"
                            style={{
                                display: "grid",
                                gridTemplateColumns: "1fr",
                                gap: "1.5rem",
                            }}
                        >
                            <div className="modern-form-group">
                                <label>Predecessor Task *</label>
                                <select
                                    className="modern-input"
                                    value={predId}
                                    onChange={(e) =>
                                        setPredId(
                                            e.target.value === ""
                                                ? ""
                                                : Number(e.target.value),
                                        )
                                    }
                                    required
                                >
                                    <option value="" disabled>
                                        -- Select First Task --
                                    </option>
                                    {tasks.map((t) => (
                                        <option key={t.id} value={t.id}>
                                            {t.title}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div className="modern-form-group">
                                <label>Successor Task *</label>
                                <select
                                    className="modern-input"
                                    value={succId}
                                    onChange={(e) =>
                                        setSuccId(
                                            e.target.value === ""
                                                ? ""
                                                : Number(e.target.value),
                                        )
                                    }
                                    required
                                >
                                    <option value="" disabled>
                                        -- Select Dependent Task --
                                    </option>
                                    {tasks.map((t) => (
                                        <option key={t.id} value={t.id}>
                                            {t.title}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div className="modern-form-group">
                                <label>Constraint Type *</label>
                                <select
                                    className="modern-input"
                                    value={typeId}
                                    onChange={(e) =>
                                        setTypeId(
                                            e.target.value === ""
                                                ? ""
                                                : Number(e.target.value),
                                        )
                                    }
                                    required
                                >
                                    <option value="" disabled>
                                        -- Select Rule --
                                    </option>
                                    {constraintTypes.map((ct) => (
                                        <option key={ct.id} value={ct.id}>
                                            {ct.name}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div className="modern-form-group">
                                <label>Lag (Minutes)</label>
                                <input
                                    type="number"
                                    className="modern-input"
                                    min={0}
                                    placeholder="e.g. 60 (Optional delay)"
                                    value={lag}
                                    onChange={(e) =>
                                        setLag(
                                            e.target.value === ""
                                                ? ""
                                                : Number(e.target.value),
                                        )
                                    }
                                />
                            </div>
                        </div>
                    </div>

                    <div
                        className="modal-actions"
                        style={{
                            padding: "1.5rem 2rem",
                            background: "#f8fafc",
                            borderTop: "1px solid #e2e8f0",
                        }}
                    >
                        <button
                            type="button"
                            className="btn-cancel"
                            onClick={onClose}
                            style={{
                                fontSize: "1rem",
                                padding: "0.75rem 1.5rem",
                            }}
                        >
                            Cancel
                        </button>
                        <button
                            type="submit"
                            className="btn-submit"
                            style={{
                                fontSize: "1rem",
                                padding: "0.75rem 1.5rem",
                            }}
                        >
                            Create Constraint
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default TaskConstraintModal;