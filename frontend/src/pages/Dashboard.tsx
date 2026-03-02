import React from 'react';
import { useAuth } from '../context/useAuth';
import { useNavigate } from 'react-router-dom';
import './Dashboard.css';

const Dashboard: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <h1>Secure Schedule Dashboard</h1>
        <div className="user-info">
          <span className="user-name">
            {user?.firstName} {user?.lastName}
          </span>
          <span className="user-role">{user?.role}</span>
          <button onClick={handleLogout} className="logout-button">
            Logout
          </button>
        </div>
      </header>

      <main className="dashboard-main">
        <div className="welcome-section">
          <h2>Welcome back, {user?.firstName}!</h2>
          <p>Role: {user?.role}</p>
          <p>Email: {user?.email}</p>
        </div>

        <div className="dashboard-grid">
          {/* Admin-only sections */}
          {user?.role === 'ADMIN' && (
            <>
              <div className="dashboard-card" onClick={() => navigate('/users')}>
                <h3>👥 Users</h3>
                <p>Manage system users</p>
              </div>
              <div className="dashboard-card" onClick={() => navigate('/priorities')}>
                <h3>⭐ Priorities</h3>
                <p>Manage task priorities</p>
              </div>
              <div className="dashboard-card" onClick={() => navigate('/statuses')}>
                <h3>📊 Statuses</h3>
                <p>Manage task statuses</p>
              </div>
              <div className="dashboard-card" onClick={() => navigate('/constraint-types')}>
                <h3>🔗 Constraint Types</h3>
                <p>Manage constraint types</p>
              </div>
            </>
          )}

          {/* Common sections for all users */}
          <div className="dashboard-card" onClick={() => navigate('/tasks')}>
            <h3>📋 Tasks</h3>
            <p>View and manage tasks</p>
          </div>
          <div className="dashboard-card" onClick={() => navigate('/task-constraints')}>
            <h3>⚙️ Task Constraints</h3>
            <p>View task constraints</p>
          </div>
          <div className="dashboard-card" onClick={() => navigate('/vacations')}>
            <h3>🏖️ Vacations</h3>
            <p>Manage vacation requests</p>
          </div>
          <div className="dashboard-card" onClick={() => navigate('/settlements')}>
            <h3>💰 Settlements</h3>
            <p>View work settlements</p>
          </div>
        </div>
      </main>
    </div>
  );
};

export default Dashboard;

