import { useState, type FormEvent } from 'react'
import { useLocation, useNavigate } from 'react-router'
import {
    Activity,
    Eye,
    EyeOff,
    LockKeyhole,
    User,
} from 'lucide-react'
import axios from 'axios'

import { useAuth } from './AuthContext'

function LoginPage() {
    const { login } = useAuth()
    const navigate = useNavigate()
    const location = useLocation()

    const [username, setUsername] = useState('')
    const [password, setPassword] = useState('')
    const [showPassword, setShowPassword] = useState(false)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState('')

    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault()
        setError('')

        if (!username.trim() || !password) {
            setError('Please enter your username and password.')
            return
        }

        try {
            setLoading(true)

            await login({
                username: username.trim(),
                password,
            })

            const from = location.state?.from?.pathname || '/'
            navigate(from, { replace: true })
        } catch (err) {
            if (axios.isAxiosError(err)) {
                const message = err.response?.data?.message
                setError(
                    typeof message === 'string'
                        ? message
                        : 'Unable to sign in. Please try again.',
                )
            } else {
                setError('Unable to sign in. Please try again.')
            }
        } finally {
            setLoading(false)
        }
    }

    return (
        <section className="w-full max-w-md rounded-2xl bg-white p-8 shadow-lg">
            <div className="mb-8 text-center">
                <div className="mb-4 flex justify-center">
                    <Activity className="h-12 w-12 text-cyan-600" />
                </div>

                <h1 className="text-2xl font-bold text-slate-900">
                    Cyber Range
                </h1>

                <p className="mt-2 text-sm text-slate-500">
                    Sign in to your account
                </p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-5">
                <div>
                    <label
                        htmlFor="username"
                        className="mb-2 block text-sm font-medium text-slate-700"
                    >
                        Username
                    </label>

                    <div className="relative">
                        <User className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />

                        <input
                            id="username"
                            type="text"
                            value={username}
                            onChange={(event) => setUsername(event.target.value)}
                            placeholder="Enter your username"
                            autoComplete="username"
                            className="w-full rounded-lg border border-slate-300 py-3 pl-10 pr-4 text-sm outline-none focus:border-cyan-500 focus:ring-2 focus:ring-cyan-100"
                            disabled={loading}
                        />
                    </div>
                </div>

                <div>
                    <label
                        htmlFor="password"
                        className="mb-2 block text-sm font-medium text-slate-700"
                    >
                        Password
                    </label>

                    <div className="relative">
                        <LockKeyhole className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />

                        <input
                            id="password"
                            type={showPassword ? 'text' : 'password'}
                            value={password}
                            onChange={(event) => setPassword(event.target.value)}
                            placeholder="Enter your password"
                            autoComplete="current-password"
                            className="w-full rounded-lg border border-slate-300 py-3 pl-10 pr-12 text-sm outline-none focus:border-cyan-500 focus:ring-2 focus:ring-cyan-100"
                            disabled={loading}
                        />

                        <button
                            type="button"
                            onClick={() => setShowPassword((current) => !current)}
                            className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                            aria-label={showPassword ? 'Hide password' : 'Show password'}
                            disabled={loading}
                        >
                            {showPassword ? (
                                <EyeOff className="h-5 w-5" />
                            ) : (
                                <Eye className="h-5 w-5" />
                            )}
                        </button>
                    </div>
                </div>

                {error && (
                    <div
                        role="alert"
                        className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600"
                    >
                        {error}
                    </div>
                )}

                <button
                    type="submit"
                    disabled={loading}
                    className="flex w-full items-center justify-center rounded-lg bg-cyan-600 px-4 py-3 text-sm font-semibold text-white transition hover:bg-cyan-700 disabled:cursor-not-allowed disabled:opacity-60"
                >
                    {loading ? 'Signing in...' : 'Sign in'}
                </button>
            </form>

            <p className="mt-8 text-center text-xs text-slate-400">
                Cyber Range Management System
            </p>
        </section>
    )
}

export default LoginPage
