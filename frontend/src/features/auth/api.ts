import { apiGet, apiPatch } from '../../shared/api/client'
import type { Id, Page, PageParams } from '../../shared/api/types'
import type { ProfileRequest, User } from '../../shared/auth/types'

export function getProfile(signal?: AbortSignal): Promise<User> {
  return apiGet<User>('/users/me', { signal })
}

export function updateProfile(request: ProfileRequest): Promise<User> {
  return apiPatch<User>('/users/me', request)
}

export function getUsers(params: PageParams & { email?: string }, signal?: AbortSignal): Promise<Page<User>> {
  return apiGet<Page<User>>('/users', { query: { ...params }, signal })
}

export function updateUserStatus(id: Id, is_active: boolean): Promise<User> {
  return apiPatch<User>(`/users/${id}/status`, { is_active })
}
