import { Tag } from 'antd'
import { TEACHER_REQUEST_STATUS_LABEL, type TeacherRequestStatus } from './types'

const COLORS: Record<TeacherRequestStatus, string> = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'error',
}

export function TeacherRequestStatusTag({ status }: { status: TeacherRequestStatus }) {
  return <Tag color={COLORS[status]}>{TEACHER_REQUEST_STATUS_LABEL[status]}</Tag>
}
