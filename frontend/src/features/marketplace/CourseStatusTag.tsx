import { Tag } from 'antd'
import type { CourseStatus } from './course.types'

const LABEL: Record<CourseStatus, string> = {
  Draft: 'Draft',
  Published: 'Published',
  Archived: 'Archived',
}

const COLOR: Record<CourseStatus, string> = {
  Draft: 'default',
  Published: 'success',
  Archived: 'warning',
}

/** Nhãn trạng thái dùng chung cho bảng giảng viên và admin, để hai chỗ không lệch màu/chữ. */
export function CourseStatusTag({ status }: { status: CourseStatus }) {
  return <Tag color={COLOR[status]}>{LABEL[status]}</Tag>
}
