import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from '../features/auth/AuthContext'

interface ProtectedRouteProps {
    allowedRoles?: string[]
}

function ProtectedRoute({
                            allowedRoles,
                        }: ProtectedRouteProps) {
    const { isAuthenticated, role } = useAuth()
    const location = useLocation()

    if (!isAuthenticated) {
        return (
            <Navigate
                to="/login"
                replace
                state={{ from: location }}
            />
        )
    }

    if (
        allowedRoles &&
        !allowedRoles.includes(role ?? '')
    ) {
        return <Navigate to="/dashboard" replace />
    }

    return <Outlet />
}

export default ProtectedRoute
