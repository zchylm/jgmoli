import { apiUrl } from './api'

export type AuthUser = {
  id: string
  customerReference: string
  email: string
  displayName: string
  role: string
  emailVerified: boolean
}

export type AuthSession = {
  accessToken: string
  user: AuthUser
}

type AuthError = { message?: string }

export const AUTH_TOKEN_STORAGE_KEY = 'jgmoli.accessToken'

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(apiUrl(`/api${path}`), {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({})) as AuthError
    throw new Error(error.message || 'Something went wrong. Try again.')
  }
  return response.json() as Promise<T>
}

export async function register(email: string, password: string, displayName: string) {
  return request<AuthSession>('/auth/register', {
    method: 'POST',
    body: JSON.stringify({ email, password, displayName }),
  })
}

export async function login(email: string, password: string) {
  return request<AuthSession>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })
}

export async function getCurrentUser(token: string) {
  return request<AuthUser>('/auth/me', {
    headers: { Authorization: `Bearer ${token}` },
  })
}

export function loadToken() {
  return window.localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)
}

export function saveToken(token: string) {
  window.localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, token)
}

export function clearToken() {
  window.localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY)
}
