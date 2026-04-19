import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Ban } from 'lucide-react';
import './Unauthorized.css';

const Unauthorized: React.FC = () => {
  const navigate = useNavigate();

  return (
    <div className="unauthorized-container">
      <div className="unauthorized-card">
        <h1>
          <Ban className="icon" size={32} /> Access Denied
        </h1>
        <p>You don't have permission to access this page.</p>
        <button onClick={() => navigate('/dashboard')} className="back-button">
          Go to Dashboard
        </button>
      </div>
    </div>
  );
};

export default Unauthorized;
