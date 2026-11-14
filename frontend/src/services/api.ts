import axios from 'axios'

export const TOKEN_STORAGE_KEY = 'cyber_range_token'

const api = axios.create({
    baseURL: '/api/v1',
    headers: {
        'Content-Type': 'application/json',
    },
})

api.interceptors.request.use((config) => {
    const token = sessionStorage.getItem(TOKEN_STORAGE_KEY)

    if (token) {
        config.headers.Authorization = `Bearer ${token}`
    }

    return config
})

export default api
