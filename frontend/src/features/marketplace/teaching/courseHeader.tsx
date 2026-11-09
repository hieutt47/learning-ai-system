import type { ReactNode } from 'react'
import { CourseStatusTag } from '../CourseStatusTag'
import type { OwnedCourse } from '../course.types'

/**
 * Phần đầu chung của 3 tab khóa học (Thông tin · Bài học · Học viên): nhãn trạng
 * thái + nút thao tác. Tabs do `handle.tabs` của route cha lo, không lặp ở đây.
 *
 * Tách riêng để tab Bài học và Học viên (của người khác) không phải chép lại
 * logic nút xuất bản, và để ba tab luôn hiện cùng một bộ nút.
 */
export function CourseHeaderActions({
  course,
  actions,
}: {
  course: OwnedCourse
  actions?: ReactNode
}) {
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
      <CourseStatusTag status={course.status} />
      {actions}
    </span>
  )
}
