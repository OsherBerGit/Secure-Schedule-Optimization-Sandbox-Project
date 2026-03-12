import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import './Layout.css'

interface LayoutProps {
    children: React.ReactNode
}

const Layout = ({ children }: LayoutProps) => {
    const { isAuthenticated, user } = useAuth()
    const location = useLocation()

    // Don't render the nav on auth/error pages
    const hideNav = ['/login', '/unauthorized'].includes(location.pathname)

    if (!isAuthenticated || hideNav) {
        return <>{children}</>
    }

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
                            <span className="nav-role-pill">{user?.role ?? (user?.roles?.[0] ?? '')}</span>
                        </span>
                    </div>
                </div>
            </nav>
            <div className="layout-content">
                {children}
            </div>
        </div>
    )
}

export default Layout

