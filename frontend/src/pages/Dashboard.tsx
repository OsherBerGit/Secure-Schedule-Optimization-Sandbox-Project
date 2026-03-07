import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/useAuth';
import { useNavigate } from 'react-router-dom';
import { CheckCircle } from 'lucide-react';
import { settlementApi } from '../api';
import type { Settlement } from '../types';
import './Dashboard.css';

const Dashboard: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const isAdmin = user?.role === 'ADMIN' || user?.roles?.includes('ADMIN');

  const [mySettlements, setMySettlements] = useState<Settlement[]>([]);
  const [settlementsLoading, setSettlementsLoading] = useState(false);
  const [settlementsError, setSettlementsError] = useState<string | null>(null);

  useEffect(() => {
    if (!isAdmin) {
      const loadSettlements = async () => {
        setSettlementsLoading(true);
        try {
          const res = await settlementApi.getMySettlements();
          setMySettlements(res.data);
        } catch (err) {
          setSettlementsError(err instanceof Error ? err.message : 'Failed to load assignments');
        } finally {
          setSettlementsLoading(false);
        }
      };
      void loadSettlements();
    }
  }, [isAdmin]);

  const handleComplete = (id: number) => {
    settlementApi.completeSettlement(id)
      .then(() =>
        settlementApi.getMySettlements().then(res => setMySettlements(res.data))
      )
      .catch(err => setSettlementsError(err instanceof Error ? err.message : 'Failed to mark as done'));
  };

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <h1>Secure Schedule Dashboard</h1>
        <div className="user-info">
          <span className="user-name">{user?.firstName} {user?.lastName}</span>
          <span className="user-role">{user?.role}</span>
          <button onClick={handleLogout} className="logout-button">Logout</button>
        </div>
      </header>

      <main className="dashboard-main">
        <div className="welcome-section">
          <h2>Welcome back, {user?.firstName}!</h2>
          <p>Role: {user?.role}</p>
          <p>Email: {user?.email}</p>
        </div>

        {/* Worker: My Assignments panel */}
        {!isAdmin && (
          <div className="my-assignments-section">
            <h3>📋 My Assignments</h3>
            {settlementsError && <div className="error-message">{settlementsError}</div>}
            {settlementsLoading ? (
              <p className="loading-text">Loading assignments…</p>
            ) : mySettlements.length === 0 ? (
              <p className="no-assignments">No assignments yet. Check back after the next schedule run.</p>
            ) : (
              <table className="assignments-table">
                <thead>
                  <tr>
                    <th>Task</th>
                    <th>Status</th>
                    <th>Assigned On</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {mySettlements.map(s => {
                    const isCompleted = s.statusName === 'COMPLETED';
                    return (
                      <tr key={s.id}>
                        <td className="assignment-task-title">{s.taskTitle}</td>
                        <td>
                          <span
                            className="status-badge"
                            style={s.statusColorCode ? { background: s.statusColorCode + '22', color: s.statusColorCode, border: `1px solid ${s.statusColorCode}44` } : undefined}
                          >
                            {s.statusName ?? '—'}
                          </span>
                        </td>
                        <td>{new Date(s.settlementDate).toLocaleDateString()}</td>
                        <td>
                          {!isCompleted && (
                            <button className="btn-complete-dash" onClick={() => handleComplete(s.id)}>
                              <CheckCircle size={15} />
                              <span>סמן כבוצע</span>
                            </button>
                          )}
                          {isCompleted && <span className="done-text">✓ Done</span>}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </div>
        )}

        <div className="dashboard-grid">
          {isAdmin && (
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
          <div className="dashboard-card" onClick={() => navigate('/schedule')}>
            <h3>📅 Schedule</h3>
            <p>Run and view task scheduling</p>
          </div>
        </div>
      </main>
    </div>
  );
};

export default Dashboard;

