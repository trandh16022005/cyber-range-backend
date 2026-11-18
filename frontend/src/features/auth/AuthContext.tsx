import {
    createContext,
    useCallback,
    useContext,
    useState,
    type ReactNode,
} from 'react'

import { TOKEN_STORAGE_KEY } from '../../services/api'
import { authService } from '../../services/authService'
import type { LoginRequest } from '../../services/authService'

interface AuthContextType {
    token: string | null
    role: string | null
    isAuthenticated: boolean
    login: (credentials: LoginRequest) => Promise<void>
    logout: () => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

interface AuthProviderProps {
    children: ReactNode
}

// Decode role from JWT payload.
// This is only for frontend UI display, not authorization.
function getRoleFromToken(token: string | null): string | null {
    if (!token) return null

    try {
        const payload = token.split('.')[1]

        if (!payload) return null

        const base64 = payload
            .replace(/-/g, '+')
            .replace(/_/g, '/')

        const decoded = JSON.parse(
            atob(base64),
        ) as { role?: unknown }

        return typeof decoded.role === 'string'
            ? decoded.role
            : null
    } catch {
        return null
    }
}

export function AuthProvider({ children }: AuthProviderProps) {
    const [token, setToken] = useState<string | null>(() => {
        return sessionStorage.getItem(TOKEN_STORAGE_KEY)
    })

    const role = getRoleFromToken(token)

    const login = useCallback(async (credentials: LoginRequest) => {
        const result = await authService.login(credentials)

        sessionStorage.setItem(TOKEN_STORAGE_KEY, result.token)
        setToken(result.token)
    }, [])

    const logout = useCallback(() => {
        sessionStorage.removeItem(TOKEN_STORAGE_KEY)
        setToken(null)
    }, [])

    return (
        <AuthContext.Provider
            value={{
                token,
                role,
                isAuthenticated: Boolean(token),
                login,
                logout,
            }}
        >
            {children}
        </AuthContext.Provider>
    )
}

export function useAuth() {
    const context = useContext(AuthContext)

    if (!context) {
        throw new Error('useAuth must be used within AuthProvider')
    }

    return context
}
