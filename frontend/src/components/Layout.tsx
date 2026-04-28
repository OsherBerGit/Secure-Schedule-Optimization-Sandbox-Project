import { Link, NavLink, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import {
    LayoutDashboard,
    Users,
    CheckSquare,
    CalendarDays,
    Plane,
    UserCheck,
    ShieldAlert,
    Building2,
    Wrench,
    LogOut,
    Shield,
    Sun,
    Moon,
} from "lucide-react";
import "./Layout.css";

interface LayoutProps {
    children: React.ReactNode;
    isDarkMode: boolean;
    setIsDarkMode: (val: boolean | ((prev: boolean) => boolean)) => void;
}
interface NavItem {
    to: string;
    label: string;
    roles: ("ADMIN" | "MANAGER" | "WORKER")[];
    icon: React.ElementType;
}

const Layout = ({ children, isDarkMode, setIsDarkMode }: LayoutProps) => {
    const { isAuthenticated, user, logout } = useAuth();
    const location = useLocation();
    const navigate = useNavigate();

    if (
        !isAuthenticated ||
        ["/login", "/unauthorized"].includes(location.pathname)
    )
        return <>{children}</>;

    const currentRole = user?.role ?? "WORKER";
    const handleLogout = async () => {
        await logout();
        navigate("/login");
    };

    const navLinks: NavItem[] = [
        {
            to: "/dashboard",
            label: "Dashboard",
            icon: LayoutDashboard,
            roles: ["ADMIN", "MANAGER", "WORKER"],
        },
        {
            to: "/users",
            label: "Users",
            icon: Users,
            roles: ["ADMIN", "MANAGER"],
        },
        {
            to: "/tasks",
            label: "Tasks",
            icon: CheckSquare,
            roles: ["ADMIN", "MANAGER"],
        },
        {
            to: "/schedule",
            label: "Schedule",
            icon: CalendarDays,
            roles: ["ADMIN", "MANAGER"],
        },
        {
            to: "/vacations",
            label: "Vacations",
            icon: Plane,
            roles: ["ADMIN", "MANAGER", "WORKER"],
        },
        {
            to: "/settlements",
            label: "Settlements",
            icon: UserCheck,
            roles: ["ADMIN", "MANAGER"],
        },
        {
            to: "/task-constraints",
            label: "Task Constraints",
            icon: ShieldAlert,
            roles: ["ADMIN", "MANAGER"],
        },
        {
            to: "/departments",
            label: "Departments",
            icon: Building2,
            roles: ["ADMIN"],
        },
        { to: "/skills", label: "Skills", icon: Wrench, roles: ["ADMIN"] },
    ];

    return (
        <div className="layout-root">
            <aside className="layout-sidebar">
                <div className="sidebar-brand">
                    <Link to="/dashboard" className="brand-link">
                        <Shield className="brand-icon" size={24} />
                        <span className="brand-text">Secure Schedule</span>
                    </Link>
                </div>
                <nav className="sidebar-nav">
                    {navLinks
                        .filter((link) =>
                            link.roles.includes(currentRole as any),
                        )
                        .map((link) => {
                            const Icon = link.icon;
                            return (
                                <NavLink
                                    key={link.to}
                                    to={link.to}
                                    className={({ isActive }) =>
                                        `nav-link ${isActive ? "active" : ""}`
                                    }
                                >
                                    <Icon size={20} className="nav-icon" />
                                    {link.label}
                                </NavLink>
                            );
                        })}
                </nav>
            </aside>
            <div className="layout-content-wrapper">
                <header className="layout-topbar">
                    <div className="topbar-right">
                        <button
                            className="btn-icon"
                            onClick={() => setIsDarkMode((prev) => !prev)}
                            title="Toggle Theme"
                            style={{
                                background: "none",
                                border: "none",
                                cursor: "pointer",
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "center",
                                padding: "0.5rem",
                                color: "var(--text-secondary)",
                            }}
                        >
                            {isDarkMode ? (
                                <Sun size={20} />
                            ) : (
                                <Moon size={20} />
                            )}
                        </button>
                        <span className="user-role-badge">{currentRole}</span>
                        <div className="user-avatar">
                            {user?.nationalId
                                ? user.nationalId.substring(0, 2)
                                : "ID"}
                        </div>
                        <button
                            className="btn-logout"
                            onClick={handleLogout}
                            title="Logout"
                        >
                            <LogOut size={20} />
                        </button>
                    </div>
                </header>
                <main className="layout-main-content">{children}</main>
            </div>
        </div>
    );
};

export default Layout;