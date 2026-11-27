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
import TeamManagementPage from '../features/teams/TeamManagementPage'
import ExerciseManagementPage from '../features/exercises/ExerciseManagementPage'
import ExerciseDetailPage from '../features/exercises/ExerciseDetailPage'
import AssessmentManagementPage from '../features/assessments/AssessmentManagementPage'
import AssessmentDetailPage from '../features/assessments/AssessmentDetailPage'
import LabWorkflowPage from '../features/lab-workflow/LabWorkflowPage'

import ProtectedRoute from './ProtectedRoute'

function AppRoutes() {
    return (
        <AuthProvider>
            <BrowserRouter>
                <Routes>
                    {/* Public routes */}
                    <Route element={<AuthLayout />}>
                        <Route
                            path="/login"
                            element={<LoginPage />}
                        />
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

                            <Route
                                path="/teams"
                                element={<TeamManagementPage />}
                            />

                            <Route
                                path="/exercises"
                                element={<ExerciseManagementPage />}
                            />

                            <Route
                                path="/exercises/:id"
                                element={<ExerciseDetailPage />}
                            />

                            {/* Lab Workflow - ADMIN / INSTRUCTOR only */}
                            <Route
                                element={
                                    <ProtectedRoute
                                        allowedRoles={['ADMIN', 'INSTRUCTOR']}
                                    />
                                }
                            >
                                <Route
                                    path="/lab-workflow"
                                    element={<LabWorkflowPage />}
                                />
                            </Route>

                            {/* Assessment routes */}
                            <Route
                                path="/assessments"
                                element={<AssessmentManagementPage />}
                            />

                            <Route
                                path="/assessments/:id"
                                element={<AssessmentDetailPage />}
                            />
                        </Route>
                    </Route>

                    {/* Root route */}
                    <Route
                        path="/"
                        element={<Navigate to="/dashboard" replace />}
                    />

                    {/* Fallback route */}
                    <Route
                        path="*"
                        element={<Navigate to="/" replace />}
                    />
                </Routes>
            </BrowserRouter>
        </AuthProvider>
    )
}

export default AppRoutes
