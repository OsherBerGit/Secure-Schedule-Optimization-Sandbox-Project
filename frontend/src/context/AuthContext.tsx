import React, { useState, useEffect, createContext } from "react";
import type { ReactNode } from "react";
import type { User, AuthContextType } from "../types";
import { authApi } from "../api";
import axiosInstance from "../api/axios";

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
    children: ReactNode;
}

function decodeJwt(token: string): { sub?: string } {
    try {
        const payload = token.split(".")[1];
        return JSON.parse(atob(payload));
    } catch {
        return {};
    }
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
    const [user, setUser] = useState<User | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const accessToken = localStorage.getItem("accessToken");

        if (accessToken) {
            const payload = decodeJwt(accessToken);

            if (payload.sub) {
                axiosInstance
                    .get<User>(`/users/me`)
                    .then(response => {
                        setUser(response.data);
                    })
                    .catch(() => {
                        logout();
                    })
                    .finally(() => {
                        setIsLoading(false);
                    });
            } else {
                logout();
                setIsLoading(false);
            }
        } else {
            setIsLoading(false);
        }
    }, []);

    const login = async (nationalId: string, password: string) => {
        try {
            const response = await authApi.login({ nationalId, password });
            localStorage.setItem("accessToken", response.data.accessToken);

            const userResponse = await axiosInstance.get<User>(`/users/me`);
            setUser(userResponse.data);

            localStorage.removeItem("user");
        } catch (error: unknown) {
            console.error("Login failed:", error);
            throw error;
        }
    };

    const logout = async () => {
        try {
            await authApi.logout();
        } catch (error: unknown) {
            console.error("Logout failed on backend:", error);
        } finally {
            localStorage.removeItem("accessToken");
            localStorage.removeItem("user");
            axiosInstance.defaults.headers.common["Authorization"] = "";
            setUser(null);
        }
    };

    const refreshAccessToken = async () => {
        try {
            const response = await authApi.refresh();
            const { accessToken } = response.data;

            localStorage.setItem("accessToken", accessToken);
            axiosInstance.defaults.headers.common["Authorization"] = `Bearer ${accessToken}`;
        } catch (error: unknown) {
            console.error("Token refresh failed:", error);
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
        refreshAccessToken
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
