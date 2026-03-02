import React, { useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import type { User, AuthContextType } from '../types';
import { authApi } from '../api';
import axiosInstance from '../api/axios';
import { AuthContext } from './AuthContext';

interface AuthProviderProps {
  children: ReactNode;
}

// Decode JWT payload without a library
function decodeJwt(token: string): { sub?: string; roles?: string[] } {
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
    const storedUser = localStorage.getItem('user');
    const accessToken = localStorage.getItem('accessToken');
    if (storedUser && accessToken) {
      setUser(JSON.parse(storedUser));
    }
    setIsLoading(false);
  }, []);

  const login = async (nationalId: string, password: string) => {
    try {
      // 1. Authenticate — backend returns only tokens
      const response = await authApi.login({ nationalId, password });
      const { accessToken, refreshToken } = response.data;
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);

      // 2. Decode nationalId from JWT, then fetch full user profile
      const payload = decodeJwt(accessToken);
      const id = payload.sub; // sub = nationalId
      if (!id) throw new Error('Invalid token: missing subject');

      const userResponse = await axiosInstance.get<User>(`/users/national-id/${id}`);
      const userData = userResponse.data;

      // 3. Derive a single `role` field from the roles array for convenience
      const roles: string[] = userData.roles ?? [];
      const derivedRole: 'ADMIN' | 'WORKER' = roles.includes('ADMIN') ? 'ADMIN' : 'WORKER';
      const enrichedUser: User = { ...userData, role: derivedRole };

      localStorage.setItem('user', JSON.stringify(enrichedUser));
      setUser(enrichedUser);
    } catch (error) {
      console.error('Login failed:', error);
      throw error;
    }
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } catch (error) {
      console.error('Logout failed:', error);
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      setUser(null);
    }
  };

  const refreshAccessToken = async () => {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) throw new Error('No refresh token available');
    try {
      const response = await authApi.refresh({ refreshToken });
      const { accessToken, refreshToken: newRefreshToken } = response.data;
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', newRefreshToken);
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
