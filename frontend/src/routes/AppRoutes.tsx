import {
    BrowserRouter,
    Navigate,
    Route,
    Routes,
} from 'react-router'

import { AuthProvider } from '../features/auth/AuthContext'

import AuthLayout from '../layouts/AuthLayout'
import DashboardLayout from '../layouts/DashboardLayout'

import LoginPage from '../features/auth/LoginPage'
import DashboardPage from '../features/dashboard/DashboardPage'
import UserManagementPage from '../features/users/UserManagementPage'

import ProtectedRoute from './ProtectedRoute'

function AppRoutes() {
    return (
        <AuthProvider>
            <BrowserRouter>
                <Routes>
                    {/* Public routes */}
                    <Route element={<AuthLayout />}>
                        <Route path="/login" element={<LoginPage />} />
                    </Route>

                    {/* Routes requiring authentication */}
                    <Route element={<ProtectedRoute />}>
                        <Route element={<DashboardLayout />}>
                            <Route
                                path="/dashboard"
                                element={<DashboardPage />}
                            />

                            <Route
                                path="/users"
                                element={<UserManagementPage />}
                            />
                        </Route>
                    </Route>

                    {/* Root route */}
                    <Route
                        path="/"
                        element={<Navigate to="/dashboard" replace />}
                    />

                    {/* Fallback route */}
                    <Route path="*" element={<Navigate to="/" replace />} />
                </Routes>
            </BrowserRouter>
        </AuthProvider>
    )
}

export default AppRoutes
