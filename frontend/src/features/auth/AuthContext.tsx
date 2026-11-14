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
    isAuthenticated: boolean
    login: (credentials: LoginRequest) => Promise<void>
    logout: () => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

interface AuthProviderProps {
    children: ReactNode
}

export function AuthProvider({ children }: AuthProviderProps) {
    const [token, setToken] = useState<string | null>(() => {
        return sessionStorage.getItem(TOKEN_STORAGE_KEY)
    })

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
