import axios from "axios";
import type {
    InternalAxiosRequestConfig,
    AxiosResponse,
    AxiosError,
} from "axios";

const BASE_URL = "http://localhost:8080/api";

const axiosInstance = axios.create({
    baseURL: BASE_URL,
    headers: {
        "Content-Type": "application/json",
    },
});

// Zero-Trust: Automatically attach the JWT access token to every outgoing request
axiosInstance.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        const token = localStorage.getItem("accessToken");
        if (token) config.headers.Authorization = `Bearer ${token}`;
        return config;
    },
    (error: AxiosError) => Promise.reject(error),
);

// Global Error Handler & Stateless JWT Refresh Flow
axiosInstance.interceptors.response.use(
    (response: AxiosResponse) => response,
    async (error: AxiosError) => {
        const originalRequest = error.config as InternalAxiosRequestConfig & {
            _retry?: boolean;
        };

        // Handle 429 Too Many Requests (Rate Limiting)
        if (error.response?.status === 429) {
            alert("Too many requests. Please wait a minute and try again.");
            return Promise.reject(new Error("Rate limit exceeded"));
        }

        // Handle 409 Conflict (Optimistic Locking)
        // Optimistic Locking guard (syncs with Backend @Version mechanism)
        if (error.response?.status === 409) {
            alert(
                "Data Conflict: The schedule has been modified by another user. Please refresh the page to get the latest version.",
            );
            return Promise.reject(error);
        }

        // Handle 422 Unprocessable Entity (Batch Validation)
        if (error.response?.status === 422 && error.response.data) {
            console.warn("Batch validation failed:", error.response.data);
        }

        // Token Refresh Execution (Triggered on 401/403 Unauthorized)
        if (
            (error.response?.status === 401 ||
                error.response?.status === 403) &&
            !originalRequest._retry
        ) {
            originalRequest._retry = true;

            try {
                const refreshToken = localStorage.getItem("refreshToken");
                if (!refreshToken)
                    throw new Error("No refresh token available");

                const response = await axios.post(
                    `${BASE_URL}/auth/refresh-token`,
                    {
                        refreshToken,
                    },
                );

                const { accessToken, refreshToken: newRefreshToken } =
                    response.data;

                localStorage.setItem("accessToken", accessToken);
                localStorage.setItem("refreshToken", newRefreshToken);

                // Re-attempt the original failed request with the new access token
                if (originalRequest.headers)
                    originalRequest.headers.Authorization = `Bearer ${accessToken}`;

                return axiosInstance(originalRequest);
            } catch (refreshError) {
                // Security fallback: Purge sensitive data on refresh failure
                localStorage.removeItem("accessToken");
                localStorage.removeItem("refreshToken");
                localStorage.removeItem("user");
                window.dispatchEvent(new Event("unauthorized"));
                return Promise.reject(refreshError);
            }
        }

        return Promise.reject(error);
    },
);

export default axiosInstance;
