import axios from 'axios';
import type { InternalAxiosRequestConfig, AxiosResponse, AxiosError } from 'axios';

const BASE_URL = 'http://localhost:8080/api';

// Create axios instance
const axiosInstance = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - attach JWT token to every request
axiosInstance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('accessToken');
    if (token)
      config.headers.Authorization = `Bearer ${token}`;

    return config;
  },
  (error: AxiosError) => {
    return Promise.reject(error);
  }
);

// Response interceptor - handle token refresh
axiosInstance.interceptors.response.use(
  (response: AxiosResponse) => {
    return response;
  },
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // Handle 429 Too Many Requests (Rate Limiting)
    if (error.response?.status === 429) {
      alert("Too many requests. Please wait a minute and try again.");
      return Promise.reject(new Error('Rate limit exceeded'));
    }

    // Handle 409 Conflict (Optimistic Locking)
    if (error.response?.status === 409) {
      alert("Data Conflict: The schedule has been modified by another user. Please refresh the page to get the latest version.");
      return Promise.reject(error);
    }

    // Handle 422 Unprocessable Entity (Batch Validation)
    if (error.response?.status === 422 && error.response.data) {
      // Ensure the component receives the structured error details
      // We pass the error through, but the component will inspect error.response.data.details
      console.warn("Batch validation failed:", error.response.data);
    }

    // If error is 401 (expired) or 403 (blacklisted/invalid) and we haven't retried yet
    if ((error.response?.status === 401 || error.response?.status === 403) && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken)
          throw new Error('No refresh token available');

        // Try to refresh the token
        const response = await axios.post(`${BASE_URL}/auth/refresh-token`, {
          refreshToken,
        });

        const { accessToken, refreshToken: newRefreshToken } = response.data;

        // Store new tokens
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', newRefreshToken);

        // Retry original request with new token
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return axiosInstance(originalRequest);
      } catch (refreshError) {
        // Refresh failed - redirect to login
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        window.dispatchEvent(new Event('unauthorized'));
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default axiosInstance;

