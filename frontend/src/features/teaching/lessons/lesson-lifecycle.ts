import type { CourseStatus } from '../../marketplace/course.types'
import type { LessonListItem, LessonStatus } from './lesson.types'

export const lessonStatusLabels: Record<LessonStatus, string> = {
  DRAFT: 'Bản nháp',
  PUBLISHED: 'Đã xuất bản',
  ARCHIVED: 'Đã lưu trữ',
}

export function lessonActionDisabledReason(
  lesson: LessonListItem,
  courseStatus: CourseStatus,
  publishedCount: number,
  hasUnsavedChanges = false,
): string | null {
  if (courseStatus === 'Archived') return 'Khóa học đã lưu trữ, chỉ có thể xem lại nội dung.'
  if (lesson.status === 'ARCHIVED') return 'Bài học đã lưu trữ, không thể xuất bản lại.'
  if (hasUnsavedChanges) return 'Lưu nội dung và tài liệu đang chỉnh sửa trước khi đổi trạng thái.'
  if (lesson.status === 'PUBLISHED' && courseStatus === 'Published' && publishedCount <= 1) {
    return 'Khóa đang xuất bản cần ít nhất một bài hiển thị. Hãy xuất bản bài thay thế hoặc gỡ xuất bản khóa trước.'
  }
  return null
}
