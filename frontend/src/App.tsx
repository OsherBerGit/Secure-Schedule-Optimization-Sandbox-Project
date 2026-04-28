import {
    BrowserRouter as Router,
    Routes,
    Route,
    Navigate,
} from "react-router-dom";
import { AuthProvider } from "./context/AuthContext.tsx";
import ProtectedRoute from "./components/ProtectedRoute";
import Layout from "./components/Layout";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import Unauthorized from "./pages/Unauthorized";

import Users from "./pages/Users";
import Vacations from "./pages/Vacations";
import Tasks from "./pages/Tasks.tsx";
import Settlements from "./pages/Settlements";
import Schedule from "./pages/Schedule";
import TaskConstraints from "./pages/TaskConstraints";
import Departments from "./pages/Departments";
import Skills from "./pages/Skills";
import { useState, useEffect } from "react";

import "./App.css";

function App() {
    const [isDarkMode, setIsDarkMode] = useState(() => {
        const savedTheme = localStorage.getItem("theme");
        if (savedTheme) return savedTheme === "dark";
        return window.matchMedia("(prefers-color-scheme: dark)").matches;
    });

    useEffect(() => {
        if (isDarkMode) {
            document.documentElement.classList.add("dark");
            localStorage.setItem("theme", "dark");
        } else {
            document.documentElement.classList.remove("dark");
            localStorage.setItem("theme", "light");
        }
    }, [isDarkMode]);

    return (
        <AuthProvider>
            <Router>
                <Layout isDarkMode={isDarkMode} setIsDarkMode={setIsDarkMode}>
                    <Routes>
                        {/* Public routes */}
                        <Route path="/login" element={<Login />} />
                        <Route
                            path="/unauthorized"
                            element={<Unauthorized />}
                        />

                        <Route
                            path="/users"
                            element={
                                <ProtectedRoute
                                    allowedRoles={["ADMIN", "MANAGER"]}
                                >
                                    <Users />
                                </ProtectedRoute>
                            }
                        />

                        {/* Protected routes */}
                        <Route
                            path="/tasks"
                            element={
                                <ProtectedRoute
                                    allowedRoles={["ADMIN", "MANAGER"]}
                                >
                                    <Tasks />
                                </ProtectedRoute>
                            }
                        />
                        <Route
                            path="/vacations"
                            element={
                                <ProtectedRoute
                                    allowedRoles={[
                                        "ADMIN",
                                        "MANAGER",
                                        "WORKER",
                                    ]}
                                >
                                    <Vacations />
                                </ProtectedRoute>
                            }
                        />
                        <Route
                            path="/dashboard"
                            element={
                                <ProtectedRoute
                                    allowedRoles={[
                                        "ADMIN",
                                        "MANAGER",
                                        "WORKER",
                                    ]}
                                >
                                    <Dashboard />
                                </ProtectedRoute>
                            }
                        />
                        <Route
                            path="/schedule"
                            element={
                                <ProtectedRoute
                                    allowedRoles={[
                                        "ADMIN",
                                        "MANAGER",
                                        "WORKER",
                                    ]}
                                >
                                    <Schedule />
                                </ProtectedRoute>
                            }
                        />
                        <Route
                            path="/settlements"
                            element={
                                <ProtectedRoute
                                    allowedRoles={["ADMIN", "MANAGER"]}
                                >
                                    <Settlements />
                                </ProtectedRoute>
                            }
                        />

                        {/* Admin-only lookup table management */}
                        <Route
                            path="/departments"
                            element={
                                <ProtectedRoute allowedRoles={["ADMIN"]}>
                                    <Departments />
                                </ProtectedRoute>
                            }
                        />
                        <Route
                            path="/skills"
                            element={
                                <ProtectedRoute allowedRoles={["ADMIN"]}>
                                    <Skills />
                                </ProtectedRoute>
                            }
                        />

                        {/* Task constraints - visible to all authenticated users, create/delete is ADMIN only (enforced in component) */}
                        <Route
                            path="/task-constraints"
                            element={
                                <ProtectedRoute
                                    allowedRoles={["ADMIN", "MANAGER"]}
                                >
                                    <TaskConstraints />
                                </ProtectedRoute>
                            }
                        />

                        {/* Redirect root to dashboard */}
                        <Route
                            path="/"
                            element={<Navigate to="/dashboard" replace />}
                        />

                        {/* Catch-all - redirect to dashboard */}
                        <Route
                            path="*"
                            element={<Navigate to="/dashboard" replace />}
                        />
                    </Routes>
                </Layout>
            </Router>
        </AuthProvider>
    );
}

export default App;