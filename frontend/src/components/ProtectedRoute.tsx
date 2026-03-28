import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/useAuth';

interface ProtectedRouteProps {
  children: React.ReactElement;
  allowedRoles?: ('ADMIN' | 'MANAGER' | 'WORKER')[];
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children, allowedRoles }) => {
  const { isAuthenticated, user, isLoading } = useAuth();

  if (isLoading) {
    return <div>Loading...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && user) {
    // Derive role defensively: use stored role field, fall back to roles array
    // This prevents a redirect when role is undefined after a page refresh
    const effectiveRole: 'ADMIN' | 'MANAGER' | 'WORKER' =
      user.role ??
      (user.roles?.includes('ADMIN') ? 'ADMIN'
       : user.roles?.includes('MANAGER') ? 'MANAGER'
       : 'WORKER');

    if (!allowedRoles.includes(effectiveRole)) {
      return <Navigate to="/unauthorized" replace />;
    }
  }

  return children;
};

export default ProtectedRoute;

