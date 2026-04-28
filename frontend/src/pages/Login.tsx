import React, { useState } from "react";
import type { FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import { ShieldCheck, Loader2 } from "lucide-react";
import "./Login.css";

const Login: React.FC = () => {
    const [nationalId, setNationalId] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [isLoading, setIsLoading] = useState(false);
    const { login } = useAuth();
    const navigate = useNavigate();

    const handleLogin = async (e: FormEvent) => {
        e.preventDefault();
        setError("");
        setIsLoading(true);
        try {
            await login(nationalId, password);
            navigate("/dashboard");
        } catch (err: any) {
            setError(
                err?.response?.data?.message ||
                    "Login failed. Please check your credentials.",
            );
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="login-container">
            <div className="login-card">
                <div className="login-header">
                    <div className="login-icon-wrapper">
                        <ShieldCheck size={40} color="var(--primary-color)" />
                    </div>
                    <h1>Login</h1>
                    <p className="login-subtitle">
                        Enter your national Id below to login to your account
                    </p>
                </div>
                <form onSubmit={handleLogin} className="login-form">
                    <div className="form-group">
                        <label htmlFor="nationalId">National ID</label>
                        <input
                            type="text"
                            id="nationalId"
                            className="modern-input"
                            value={nationalId}
                            onChange={(e) => setNationalId(e.target.value)}
                            required
                            autoComplete="username"
                            placeholder="Enter your National ID"
                        />
                    </div>
                    <div className="form-group">
                        <label htmlFor="password">Password</label>
                        <input
                            type="password"
                            id="password"
                            className="modern-input"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            autoComplete="current-password"
                            placeholder="Enter your password"
                        />
                    </div>
                    {error && <div className="error-message">{error}</div>}
                    <button
                        type="submit"
                        disabled={isLoading}
                        className="login-button"
                    >
                        {isLoading ? (
                            <span className="button-content">
                                <Loader2 className="spinner" size={18} />
                                Logging in...
                            </span>
                        ) : (
                            "Login"
                        )}
                    </button>
                </form>
            </div>
        </div>
    );
};

export default Login;