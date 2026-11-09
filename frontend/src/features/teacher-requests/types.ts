import type { Id, IsoDateTime, PageParams } from '../../shared/api/types'

export type TeacherRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export const TEACHER_REQUEST_STATUS_LABEL: Record<TeacherRequestStatus, string> = {
  PENDING: 'Đang chờ',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Bị từ chối',
}

export interface TeacherRequest {
  id: Id
  user_id: Id
  reason: string
  status: TeacherRequestStatus
  decided_by: Id | null
  decision_note: string | null
  decided_at: IsoDateTime | null
  created_at: IsoDateTime
}

export interface TeacherRequestParams extends PageParams {
  status?: TeacherRequestStatus
}

export interface TeacherDecision {
  status: 'APPROVED' | 'REJECTED'
  decision_note?: string | null
}
