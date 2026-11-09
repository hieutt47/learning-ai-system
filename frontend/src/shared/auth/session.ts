import {
  apiGet,
  apiPost,
  isApiError,
  setAccessTokenProvider,
  setRefreshTokenHandler,
  setSessionExpiredHandler,
} from '../api/client'
import { queryClient } from '../api/queryClient'
import type { AuthResponse, LoginRequest, User } from './types'

interface SessionState {
  user: User | null
  isLoading: boolean
  error: unknown
}

let state: SessionState = { user: null, isLoading: true, error: null }
let accessToken: string | null = null
let refreshPromise: Promise<void> | null = null
let restorePromise: Promise<void> | null = null
let logoutPromise: Promise<void> | null = null
const listeners = new Set<() => void>()

function updateSession(nextState: SessionState): void {
  state = nextState
  listeners.forEach((listener) => listener())
}

export function subscribeSession(listener: () => void): () => void {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

export function getSession(): SessionState {
  return state
}

export function updateCurrentUser(user: User): void {
  if (state.user?.id === user.id) {
    updateSession({ ...state, user })
  }
}

function clearSession(): void {
  accessToken = null
  queryClient.clear()
  updateSession({ user: null, isLoading: false, error: null })
}

function refreshAccessToken(): Promise<void> {
  if (logoutPromise) {
    return Promise.reject(new DOMException('Đang đăng xuất', 'AbortError'))
  }
  if (!refreshPromise) {
    refreshPromise = apiPost<AuthResponse>('/auth/refresh', undefined, { auth: false })
      .then((response) => {
        accessToken = response.access_token
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

export function restoreSession(): Promise<void> {
  if (logoutPromise) {
    return logoutPromise
  }
  if (!restorePromise) {
    updateSession({ ...state, isLoading: true, error: null })
    restorePromise = (async () => {
      try {
        await refreshAccessToken()
        const user = await apiGet<User>('/users/me')
        updateSession({ user, isLoading: false, error: null })
      } catch (error) {
        if (isApiError(error) && (error.status === 401 || error.code === 'ACCOUNT_INACTIVE')) {
          clearSession()
        } else {
          accessToken = null
          updateSession({ user: null, isLoading: false, error })
        }
      } finally {
        restorePromise = null
      }
    })()
  }
  return restorePromise
}

export async function login(request: LoginRequest): Promise<User> {
  const response = await apiPost<AuthResponse>('/auth/login', request, { auth: false })
  queryClient.clear()
  accessToken = response.access_token
  try {
    const user = await apiGet<User>('/users/me')
    updateSession({ user, isLoading: false, error: null })
    return user
  } catch (error) {
    clearSession()
    throw error
  }
}

export function logout(): Promise<void> {
  if (!logoutPromise) {
    logoutPromise = (async () => {
      const pendingSession = restorePromise ?? refreshPromise
      await pendingSession?.catch(() => undefined)
      await apiPost<void>('/auth/logout', undefined, { auth: false })
      clearSession()
    })().finally(() => {
      logoutPromise = null
    })
  }
  return logoutPromise
}

setAccessTokenProvider(() => accessToken)
setRefreshTokenHandler(refreshAccessToken)
setSessionExpiredHandler(clearSession)
