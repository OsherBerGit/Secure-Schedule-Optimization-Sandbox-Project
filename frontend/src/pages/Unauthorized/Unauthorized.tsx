import React from "react";
import { useNavigate } from "react-router-dom";
import { ShieldAlert, ArrowLeft } from "lucide-react";
import "./Unauthorized.css";

const Unauthorized: React.FC = () => {
    const navigate = useNavigate();

    return (
        <div className="unauthorized-page flex-center">
            <div className="modern-modal-card responsive-modal text-center">
                <div className="modal-header flex-center">
                    <div className="error-icon-wrapper top-margin">
                        <ShieldAlert size={48} className="text-danger" />
                    </div>
                </div>

                <div className="modal-body padded-body">
                    <h1 className="text-primary-dark">Access Denied</h1>
                    <div className="error-banner banner-spacing">Security Policy: You do not have the required permissions to view this resource.</div>
                    <p className="text-secondary">If you believe this is an error, please contact your system administrator.</p>
                </div>

                <div className="modal-actions modal-actions-footer flex-center">
                    <button onClick={() => navigate("/dashboard")} className="btn-add-primary flex-center gap-small">
                        <ArrowLeft size={18} />
                        <span>Return to Dashboard</span>
                    </button>
                </div>
            </div>
        </div>
    );
};

export default Unauthorized;
