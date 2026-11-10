import { apiGet, apiPatch, apiPost } from '../../shared/api/client'
import type { Id, Page, PageParams } from '../../shared/api/types'
import type { TeacherDecision, TeacherRequest, TeacherRequestParams } from './types'

export function getMyTeacherRequests(params: PageParams, signal?: AbortSignal): Promise<Page<TeacherRequest>> {
  return apiGet<Page<TeacherRequest>>('/teacher-requests/me', { query: { ...params }, signal })
}

export function createTeacherRequest(reason: string): Promise<TeacherRequest> {
  return apiPost<TeacherRequest>('/teacher-requests', { reason })
}

export function getTeacherRequests(params: TeacherRequestParams, signal?: AbortSignal): Promise<Page<TeacherRequest>> {
  return apiGet<Page<TeacherRequest>>('/teacher-requests', { query: { ...params }, signal })
}

export function decideTeacherRequest(id: Id, decision: TeacherDecision): Promise<TeacherRequest> {
  return apiPatch<TeacherRequest>(`/teacher-requests/${id}/decision`, decision)
}
