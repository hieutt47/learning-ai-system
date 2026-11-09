import { useSyncExternalStore } from 'react'
import type { Role } from '../api/types'
import { getSession, subscribeSession } from './session'
import type { User } from './types'

export type ViewerRole = Role | 'GUEST'

export interface CurrentUser {
  role: ViewerRole
  name: string | null
  user: User | null
  isLoading: boolean
  error: unknown
}

export const ROLE_LABEL: Record<Role, string> = {
  STUDENT: 'Sinh viên',
  TEACHER: 'Giảng viên',
  ADMIN: 'Admin',
}

export function useCurrentUser(): CurrentUser {
  const session = useSyncExternalStore(subscribeSession, getSession)
  return {
    ...session,
    role: session.user?.role ?? 'GUEST',
    name: session.user?.full_name ?? null,
  }
}
