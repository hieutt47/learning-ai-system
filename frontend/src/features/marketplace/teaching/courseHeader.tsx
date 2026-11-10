import type { ReactNode } from 'react'
import { CourseStatusTag } from '../CourseStatusTag'
import type { OwnedCourse } from '../course.types'

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
