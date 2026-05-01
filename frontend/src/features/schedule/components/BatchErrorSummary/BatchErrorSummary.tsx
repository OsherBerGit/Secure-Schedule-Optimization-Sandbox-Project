import React from "react";
import { AlertTriangle, X } from "lucide-react";
import "./BatchErrorSummary.css";

interface BatchErrorSummaryProps {
    errors: string[];
    onClose?: () => void;
}

const BatchErrorSummary: React.FC<BatchErrorSummaryProps> = ({ errors, onClose }) => {
    if (!errors?.length) return null;

    return (
        <div className="batch-error-container" role="alert">
            <div className="batch-error-header">
                <div className="batch-error-title-wrapper">
                    <AlertTriangle className="batch-error-icon" size={24} />
                    <h3 className="batch-error-title">Batch Scheduling Failed</h3>
                </div>
                {onClose && (
                    <button onClick={onClose} className="batch-error-close" aria-label="Close">
                        <X size={20} />
                    </button>
                )}
            </div>

            <div className="batch-error-content">
                <p className="batch-error-description">The following Zero-Trust validation errors prevented the schedule from being saved:</p>
                <ul className="batch-error-list">
                    {errors.map((errorMessage, index) => (
                        <li key={index}>{errorMessage}</li>
                    ))}
                </ul>
            </div>
        </div>
    );
};

export default BatchErrorSummary;
