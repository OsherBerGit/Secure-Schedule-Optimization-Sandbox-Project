import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext.tsx';
import ProtectedRoute from './components/ProtectedRoute';
import Layout from './components/Layout';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Unauthorized from './pages/Unauthorized';

import Users from './pages/Users';
import Vacations from './pages/Vacations';
import Tasks from './pages/Tasks.tsx';
import Settlements from './pages/Settlements';
import Schedule from './pages/Schedule';
import Priorities from './pages/Priorities';
import Statuses from './pages/Statuses';
import ConstraintTypes from './pages/ConstraintTypes';
import TaskConstraints from './pages/TaskConstraints';
import Departments from './pages/Departments';

import './App.css';

function App() {
  return (
    <AuthProvider>
      <Router>
        <Layout>
          <Routes>
          {/* Public routes */}
          <Route path="/login" element={<Login />} />
          <Route path="/unauthorized" element={<Unauthorized />} />

            <Route
                path="/users"
                element={
                    <ProtectedRoute allowedRoles={['ADMIN']}>
                        <Users />
                    </ProtectedRoute>
                }
            />

          {/* Protected routes */}
          <Route
            path="/tasks"
            element={
              <ProtectedRoute>
                <Tasks />
              </ProtectedRoute>
            }
          />
          <Route
            path="/vacations"
            element={
              <ProtectedRoute>
                <Vacations />
              </ProtectedRoute>
            }
          />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <Dashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path="/schedule"
            element={
              <ProtectedRoute>
                <Schedule />
              </ProtectedRoute>
            }
          />
          <Route
            path="/settlements"
            element={
              <ProtectedRoute>
                <Settlements />
              </ProtectedRoute>
            }
          />

          {/* Admin-only lookup table management */}
          <Route
            path="/priorities"
            element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <Priorities />
              </ProtectedRoute>
            }
          />
          <Route
            path="/statuses"
            element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <Statuses />
              </ProtectedRoute>
            }
          />
          <Route
            path="/constraint-types"
            element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <ConstraintTypes />
              </ProtectedRoute>
            }
          />
          <Route
            path="/departments"
            element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <Departments />
              </ProtectedRoute>
            }
          />

          {/* Task constraints — visible to all authenticated users, create/delete is ADMIN only (enforced in component) */}
          <Route
            path="/task-constraints"
            element={
              <ProtectedRoute>
                <TaskConstraints />
              </ProtectedRoute>
            }
          />

          {/* Redirect root to dashboard */}
          <Route path="/" element={<Navigate to="/dashboard" replace />} />

          {/* Catch-all - redirect to dashboard */}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
        </Layout>
      </Router>
    </AuthProvider>
  );
}

export default App;
