import React, { useState, type FormEvent, type ChangeEvent } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/useAuth";
import { ShieldCheck, Loader2 } from "lucide-react";
import { isAxiosError } from "axios";
import "./Login.css";

const Login: React.FC = () => {
    const [credentials, setCredentials] = useState({
        nationalId: "",
        password: ""
    });
    const [error, setError] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(false);

    const { login } = useAuth();
    const navigate = useNavigate();

    const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
        const { id, value } = e.target;
        setCredentials(prev => ({
            ...prev,
            [id]: value
        }));
    };

    const handleLogin = async (e: FormEvent) => {
        e.preventDefault();
        setError(null);
        setIsLoading(true);

        try {
            await login(credentials.nationalId.trim(), credentials.password);
            navigate("/dashboard");
        } catch (err: unknown) {
            let message = "Login failed. Please check your credentials.";

            if (isAxiosError(err)) {
                message = err.response?.data?.message || err.message;
            } else if (err instanceof Error) {
                message = err.message;
            }

            setError(message);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="login-container">
            <div className="login-card">
                <div className="login-header">
                    <div className="login-icon-wrapper">
                        <ShieldCheck size={40} className="text-primary" />
                    </div>
                    <h1>Login</h1>
                    <p className="login-subtitle">Enter your national ID to access Secure-Schedule</p>
                </div>

                <form onSubmit={handleLogin} className="login-form">
                    <div className="form-group">
                        <label htmlFor="nationalId">National ID</label>
                        <input
                            type="text"
                            id="nationalId"
                            className="modern-input"
                            value={credentials.nationalId}
                            onChange={handleChange}
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
                            value={credentials.password}
                            onChange={handleChange}
                            required
                            autoComplete="current-password"
                            placeholder="Enter your password"
                        />
                    </div>

                    {error && <div className="error-banner banner-spacing">{error}</div>}

                    <button type="submit" disabled={isLoading} className="login-button btn-submit-modern">
                        {isLoading ? (
                            <span className="button-content">
                                <Loader2 className="spinner" size={18} />
                                Authenticating...
                            </span>
                        ) : (
                            "Sign In"
                        )}
                    </button>
                </form>
            </div>
        </div>
    );
};

export default Login;
