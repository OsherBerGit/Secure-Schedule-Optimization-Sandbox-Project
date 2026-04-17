import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/useAuth';
import { Link, useNavigate } from 'react-router-dom';
import { CheckCircle, Users, ClipboardList, Plane, CheckSquare, ShieldAlert, Building2, Wrench, Shield, Hash, Mail, CalendarDays } from 'lucide-react';
import { settlementApi } from '../api';
import type { Settlement } from '../types';
import './Dashboard.css';

const Dashboard: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const isAdmin = user?.role === 'ADMIN';
  const isManager = user?.role === 'MANAGER';

  const greetingSuffix = isManager && user?.departmentName
      ? ` - ${user.departmentName} Department`
      : '';

  const [mySettlements, setMySettlements] = useState<Settlement[]>([]);
  const [settlementsLoading, setSettlementsLoading] = useState(false);
  const [settlementsError, setSettlementsError] = useState<string | null>(null);

  const settlementStatusColors: { [key: string]: string } = {
      'PENDING': '#6B7280',
      'ASSIGNED': '#3B82F6',
      'IN_PROGRESS': '#8B5CF6',
      'COMPLETED': '#10B981',
      'FAILED': '#EF4444',
  };

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
        .catch((err: unknown) => setSettlementsError(err instanceof Error ? err.message : 'Failed to mark as done'));
  };

  return (
      <div className="dashboard-container">
        <main className="dashboard-main">
          <div className="welcome-banner">
            <div className="banner-content">
              <h2>Welcome back, {user?.firstName}{greetingSuffix}!</h2>
              <div className="banner-details">
                <span className="banner-pill"><Shield size={14} /> Role: {user?.role}</span>
                <span className="banner-pill"><Hash size={14} /> ID: {user?.nationalId}</span>
                <span className="banner-pill"><Mail size={14} /> Email: {user?.email}</span>
              </div>
            </div>
          </div>

          {!isAdmin && (
              <div className="my-assignments-section">
                <div className="section-header">
                  <h3>📋 My Assignments</h3>
                </div>
                {settlementsError && <div className="error-message">{settlementsError}</div>}
                {settlementsLoading ? (
                    <p className="loading-text">Loading assignments…</p>
                ) : mySettlements.length === 0 ? (
                    <div className="no-assignments">
                      <p>No assignments yet. Check back after the next schedule run.</p>
                    </div>
                ) : (
                    <div className="dashboard-table-wrapper">
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
                          const colorCode = s.statusName ? settlementStatusColors[s.statusName] : undefined;
                          return (
                              <tr key={s.id}>
                                <td className="assignment-task-title">{s.taskTitle}</td>
                                <td>
                            <span
                                className="status-badge"
                                style={colorCode ? { background: colorCode + '22', color: colorCode, border: `1px solid ${colorCode}44` } : undefined}
                            >
                              {s.statusName ?? '-'}
                            </span>
                                </td>
                                <td>{new Date(s.settlementDate).toLocaleDateString()}</td>
                                <td>
                                  {!isCompleted && (
                                      <button className="btn-complete-dash" onClick={() => handleComplete(s.id)}>
                                        <CheckCircle size={15} />
                                        <span>Mark as Done</span>
                                      </button>
                                  )}
                                  {isCompleted && <span className="done-text">✓ Done</span>}
                                </td>
                              </tr>
                          );
                        })}
                        </tbody>
                      </table>
                    </div>
                )}
              </div>
          )}

          <div className="dashboard-action-rows">
            {(isAdmin || isManager) && (
                <div className="top-row">
                  <Link to="/schedule" className="dashboard-card hero-card">
                    <div className="card-icon hero-icon"><CalendarDays size={32} /></div>
                    <div className="card-content">
                      <h3>Schedule / Run Algorithm</h3>
                      <p>Run the scheduling algorithm, review Gantt charts, and approve assignments</p>
                    </div>
                  </Link>
                </div>
            )}

            {(isAdmin || isManager) && (
                <div className="middle-row">
                  <Link to="/users" className="dashboard-card">
                    <div className="card-icon"><Users size={24} /></div>
                    <h3>Manage Personnel</h3>
                    <p>View and manage system users</p>
                  </Link>

                  <Link to="/tasks" className="dashboard-card">
                    <div className="card-icon"><ClipboardList size={24} /></div>
                    <h3>Manage Tasks</h3>
                    <p>Create and assign tasks</p>
                  </Link>
                </div>
            )}

            <div className="bottom-row">
              <Link to="/vacations" className="dashboard-card">
                <div className="card-icon"><Plane size={24} /></div>
                <h3>Vacations</h3>
                <p>Manage vacation requests</p>
              </Link>

              {(isAdmin || isManager) && (
                  <Link to="/settlements" className="dashboard-card">
                    <div className="card-icon"><CheckSquare size={24} /></div>
                    <h3>Settlements</h3>
                    <p>View work settlements</p>
                  </Link>
              )}

              {(isAdmin || isManager) && (
                  <Link to="/task-constraints" className="dashboard-card">
                    <div className="card-icon"><ShieldAlert size={24} /></div>
                    <h3>Task Constraints</h3>
                    <p>View logical rule constraints</p>
                  </Link>
              )}

              {isAdmin && (
                  <Link to="/departments" className="dashboard-card">
                    <div className="card-icon"><Building2 size={24} /></div>
                    <h3>Departments</h3>
                    <p>Manage organizational structures</p>
                  </Link>
              )}

              {isAdmin && (
                  <Link to="/skills" className="dashboard-card">
                    <div className="card-icon"><Wrench size={24} /></div>
                    <h3>Skills</h3>
                    <p>Manage workforce qualifications</p>
                  </Link>
              )}
            </div>
          </div>
        </main>
      </div>
  );
};

export default Dashboard;