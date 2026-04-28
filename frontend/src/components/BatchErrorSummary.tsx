import React from "react";
import "./BatchErrorSummary.css";

interface BatchErrorSummaryProps {
    errors: string[];
    onClose?: () => void;
}

const BatchErrorSummary: React.FC<BatchErrorSummaryProps> = ({
    errors,
    onClose,
}) => {
    if (!errors || errors.length === 0) return null;

    return (
        <div className="batch-error-container" role="alert">
            <div className="batch-error-header">
                <div className="batch-error-title-wrapper">
                    <svg
                        className="batch-error-icon"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                        xmlns="http://www.w3.org/2000/svg"
                    >
                        <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth="2"
                            d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                        />
                    </svg>
                    <h3 className="batch-error-title">
                        Batch Scheduling Failed
                    </h3>
                </div>
                {onClose && (
                    <button
                        onClick={onClose}
                        className="batch-error-close"
                        aria-label="Close"
                    >
                        <svg
                            className="w-5 h-5"
                            fill="none"
                            stroke="currentColor"
                            viewBox="0 0 24 24"
                            width="24"
                            height="24"
                        >
                            <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                strokeWidth="2"
                                d="M6 18L18 6M6 6l12 12"
                            />
                        </svg>
                    </button>
                )}
            </div>

            <div className="batch-error-content">
                <p className="batch-error-description">
                    The following Zero-Trust validation errors prevented the
                    schedule from being saved:
                </p>
                <ul className="batch-error-list">
                    {errors.map((err, index) => (
                        <li key={index}>{err}</li>
                    ))}
                </ul>
            </div>
        </div>
    );
};

export default BatchErrorSummary;