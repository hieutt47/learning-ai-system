import { useSyncExternalStore } from 'react'
import type { Role } from '../api/types'

export type ViewerRole = Role | 'GUEST'

export interface CurrentUser {
  role: ViewerRole
  /** Tên hiển thị ở ô tài khoản; null khi là khách. */
  name: string | null
}

export const VIEWER_ROLES: ViewerRole[] = ['GUEST', 'STUDENT', 'TEACHER', 'ADMIN']

export const ROLE_LABEL: Record<Role, string> = {
  STUDENT: 'Sinh viên',
  TEACHER: 'Giảng viên',
  ADMIN: 'Admin',
}

// ---- Đổi vai trò khi dev (trang /dev/screens), lưu trong localStorage, chỉ có tác dụng khi `npm run dev` ----

const DEV_ROLE_KEY = 'las.devRole'
const listeners = new Set<() => void>()

function readDevRoleOverride(): string | null {
  if (!import.meta.env.DEV) {
    return null
  }
  try {
    return window.localStorage.getItem(DEV_ROLE_KEY)
  } catch {
    return null
  }
}

/** Đổi vai trò xem giao diện khi dev; null = quay về VITE_DEV_ROLE trong .env.local. */
export function setDevRoleOverride(role: ViewerRole | null): void {
  try {
    if (role) {
      window.localStorage.setItem(DEV_ROLE_KEY, role)
    } else {
      window.localStorage.removeItem(DEV_ROLE_KEY)
    }
  } catch {
    // localStorage bị chặn: bỏ qua.
  }
  listeners.forEach((notify) => notify())
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

/**
 * TODO(stub): người đang xem, chỉ để hiện menu và chặn trang theo vai trò ở phía UI.
 * Tạm lấy vai trò từ /dev/screens (localStorage) hoặc VITE_DEV_ROLE, tên từ VITE_DEV_USER_NAME;
 * khi auth xong thì thay bằng GET /users/me. Backend vẫn kiểm tra quyền thật.
 */
export function useCurrentUser(): CurrentUser {
  const override = useSyncExternalStore(subscribe, readDevRoleOverride, () => null)
  const raw = (override ?? import.meta.env.VITE_DEV_ROLE)?.toUpperCase() as ViewerRole | undefined
  const role = raw && VIEWER_ROLES.includes(raw) ? raw : 'GUEST'
  if (role === 'GUEST') {
    return { role, name: null }
  }
  const envName = import.meta.env.VITE_DEV_USER_NAME
  const name = !override && envName ? envName : `${ROLE_LABEL[role]} (dev)`
  return { role, name }
}
