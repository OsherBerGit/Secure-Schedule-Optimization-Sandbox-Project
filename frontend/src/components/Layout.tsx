import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import './Layout.css'

interface LayoutProps {
    children: React.ReactNode
}

interface NavItem {
    to: string;
    label: string;
    roles: ('ADMIN' | 'MANAGER' | 'WORKER')[];
}

const Layout = ({ children }: LayoutProps) => {
    const { isAuthenticated, user, logout } = useAuth()
    const location = useLocation()
    const navigate = useNavigate()

    // Don't render the nav on auth/error pages
    const hideNav = ['/login', '/unauthorized'].includes(location.pathname)

    if (!isAuthenticated || hideNav) {
        return <>{children}</>
    }

    const currentRole = user?.role ?? 'WORKER';

    const handleLogout = async () => {
        await logout()
        navigate('/login')
    }

    const navLinks: NavItem[] = [
        { to: '/dashboard', label: 'Dashboard', roles: ['ADMIN', 'MANAGER', 'WORKER'] },
        { to: '/users', label: 'Users', roles: ['ADMIN', 'MANAGER'] },
        { to: '/tasks', label: 'Tasks', roles: ['ADMIN', 'MANAGER'] },
        { to: '/schedule', label: 'Schedule', roles: ['ADMIN', 'MANAGER'] },
        { to: '/vacations', label: 'Vacations', roles: ['ADMIN', 'MANAGER', 'WORKER'] },
        { to: '/settlements', label: 'Settlements', roles: ['ADMIN', 'MANAGER'] },
        { to: '/task-constraints', label: 'Task Constraints', roles: ['ADMIN', 'MANAGER'] },
        { to: '/departments', label: 'Departments', roles: ['ADMIN'] },
        { to: '/skills', label: 'Skills', roles: ['ADMIN'] },
    ];

    return (
        <div className="layout-root">
            <nav className="global-nav">
                <div className="global-nav-inner">
                    <div className="global-nav-brand">
                        <Link to="/dashboard" className="nav-home-link">
                            🏠 <span className="nav-app-title">Scheduling System</span>
                        </Link>
                    </div>
                    <div className="global-nav-right">
                        <span className="nav-user-chip">
                            {user?.firstName ?? user?.email ?? 'User'}
                            <span className="nav-role-pill">{currentRole}</span>
                        </span>
                        <button 
                            className="btn-logout" 
                            onClick={handleLogout}
                        >Logout</button>
                    </div>
                </div>
            </nav>
            <div className="layout-body">
                <aside className="layout-sidebar">
                    {navLinks
                        .filter(link => link.roles.includes(currentRole as any))
                        .map(link => (
                            <NavLink
                                key={link.to}
                                to={link.to}
                                className={({ isActive }) =>
                                    `nav-link ${isActive ? 'active' : ''}`
                                }
                            >
                                {link.label}
                            </NavLink>
                        ))}
                </aside>
                <main className="layout-main-content">
                    {children}
                </main>
            </div>
        </div>
    )
}

export default Layout
