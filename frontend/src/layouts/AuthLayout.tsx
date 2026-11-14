import { Outlet } from 'react-router'

function AuthLayout() {
    return (
        <main className="flex min-h-screen items-center justify-center bg-slate-100 p-4">
            <Outlet />
        </main>
    )
}

export default AuthLayout