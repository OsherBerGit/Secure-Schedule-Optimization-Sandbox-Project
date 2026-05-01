import React, { useState, useEffect } from "react";
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import ProtectedRoute from "./components/layout/ProtectedRoute";
import Layout from "./components/layout/Layout";

import Login from "./pages/Login/Login";
import Dashboard from "./pages/Dashboard/Dashboard";
import Unauthorized from "./pages/Unauthorized/Unauthorized";
import Users from "./pages/Users/Users";
import Vacations from "./pages/Vacations/Vacations";
import Tasks from "./pages/Tasks/Tasks";
import Settlements from "./pages/Settlements/Settlements";
import Schedule from "./pages/Schedule/Schedule";
import TaskConstraints from "./pages/TaskConstraints/TaskConstraints";
import Departments from "./pages/Departments/Departments";
import Skills from "./pages/Skills/Skills";

import "./App.css";

const App: React.FC = () => {
    const [isDarkMode, setIsDarkMode] = useState(() => {
        const saved = localStorage.getItem("theme");
        return saved ? saved === "dark" : window.matchMedia("(prefers-color-scheme: dark)").matches;
    });

    useEffect(() => {
        const root = document.documentElement;
        isDarkMode ? root.classList.add("dark") : root.classList.remove("dark");
        localStorage.setItem("theme", isDarkMode ? "dark" : "light");
    }, [isDarkMode]);

    return (
        <AuthProvider>
            <Router>
                <Layout isDarkMode={isDarkMode} setIsDarkMode={setIsDarkMode}>
                    <Routes>
                        <Route path="/login" element={<Login />} />
                        <Route path="/unauthorized" element={<Unauthorized />} />

                        <Route element={<ProtectedRoute allowedRoles={["ADMIN", "MANAGER"]} />}>
                            <Route path="/users" element={<Users />} />
                            <Route path="/tasks" element={<Tasks />} />
                            <Route path="/settlements" element={<Settlements />} />
                            <Route path="/task-constraints" element={<TaskConstraints />} />
                        </Route>

                        <Route element={<ProtectedRoute allowedRoles={["ADMIN", "MANAGER", "WORKER"]} />}>
                            <Route path="/dashboard" element={<Dashboard />} />
                            <Route path="/vacations" element={<Vacations />} />
                            <Route path="/schedule" element={<Schedule />} />
                        </Route>

                        <Route element={<ProtectedRoute allowedRoles={["ADMIN"]} />}>
                            <Route path="/departments" element={<Departments />} />
                            <Route path="/skills" element={<Skills />} />
                        </Route>

                        <Route path="/" element={<Navigate to="/dashboard" replace />} />
                        <Route path="*" element={<Navigate to="/dashboard" replace />} />
                    </Routes>
                </Layout>
            </Router>
        </AuthProvider>
    );
};

export default App;
