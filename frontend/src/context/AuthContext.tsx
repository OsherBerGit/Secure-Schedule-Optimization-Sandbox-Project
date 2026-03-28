import React, { useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import type { User, AuthContextType } from '../types';
import { authApi } from '../api';
import axiosInstance from '../api/axios';
import { AuthContext } from './AuthContext';

interface AuthProviderProps {
  children: ReactNode;
}

// Decode JWT payload to get essential info
function decodeJwt(token: string): { sub?: string; role?: 'ROLE_ADMIN' | 'ROLE_MANAGER' | 'ROLE_WORKER'; departmentId?: number } {
  try {
    const payload = token.split('.')[1];
    return JSON.parse(atob(payload));
  } catch {
    return {};
  }
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const accessToken = localStorage.getItem('accessToken');
    if (accessToken) {
      // If a token exists, decode it to get the user's nationalId and fetch fresh data
      const payload = decodeJwt(accessToken);
      const nationalId = payload.sub;
      if (nationalId) {
        axiosInstance.get<User>(`/users/national-id/${nationalId}`)
          .then(response => {
            const userData = response.data;
            // The role now comes directly from the backend response
            setUser(userData);
          })
          .catch(() => {
            // If fetching the user fails (e.g., token expired), log them out
            logout();
          })
          .finally(() => {
            setIsLoading(false);
          });
      } else {
        // Invalid token, clear session
        logout();
        setIsLoading(false);
      }
    } else {
      setIsLoading(false);
    }
  }, []);

  const login = async (nationalId: string, password: string) => {
    try {
      // 1. Authenticate — backend returns tokens
      const response = await authApi.login({ nationalId, password });
      const { accessToken, refreshToken } = response.data;
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);

      // 2. Decode nationalId from JWT, then fetch full user profile
      const payload = decodeJwt(accessToken);
      const id = payload.sub;
      if (!id) throw new Error('Invalid token: missing subject');

      const userResponse = await axiosInstance.get<User>(`/users/national-id/${id}`);
      const userData = userResponse.data;

      // 3. The user object from the backend now includes the correct single role.
      //    No need to derive it manually.
      setUser(userData);
      // We no longer store the full user object in localStorage for security and to prevent stale data.
      // The user is now fetched fresh on every application load.
      localStorage.removeItem('user'); // Clean up old storage if it exists

    } catch (error) {
      console.error('Login failed:', error);
      throw error;
    }
  };

  const logout = async () => {
    try {
      // Inform the backend, but don't let it block the frontend logout
      await authApi.logout();
    } catch (error) {
      console.error('Logout failed on backend:', error);
    } finally {
      // Clear all session-related items from storage
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user'); // Ensure old user object is gone
      axiosInstance.defaults.headers.common['Authorization'] = ''; // Clear auth header
      setUser(null);
    }
  };

  const refreshAccessToken = async () => {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) {
      await logout();
      throw new Error('No refresh token available');
    }
    try {
      const response = await authApi.refresh({ refreshToken });
      const { accessToken, refreshToken: newRefreshToken } = response.data;
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', newRefreshToken);
      axiosInstance.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
    } catch (error) {
      console.error('Token refresh failed:', error);
      await logout();
      throw error;
    }
  };

  const value: AuthContextType = {
    user,
    isAuthenticated: !!user,
    isLoading,
    login,
    logout,
    refreshAccessToken,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
