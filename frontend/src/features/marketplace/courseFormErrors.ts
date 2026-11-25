import type { FormInstance } from 'antd'
import { isApiError } from '../../shared/api/client'
import type { CourseFormValues } from './course.types'

const COURSE_FIELD_MAP: Record<string, keyof CourseFormValues> = {
  title: 'title',
  description: 'description',
  categoryId: 'categoryId',
  category_id: 'categoryId',
  price: 'price',
}

const COURSE_FIELD_LABELS: Record<string, string> = {
  title: 'Tên khóa học',
  description: 'Mô tả',
  categoryId: 'Danh mục',
  category_id: 'Danh mục',
  price: 'Giá',
}

export function extractCourseErrorMessage(error: unknown, fallback: string): string {
  if (isApiError(error)) {
    if (error.details.length > 0) {
      return error.details
        .map((detail) => {
          const label = COURSE_FIELD_LABELS[detail.field]
          return label ? `${label}: ${detail.message}` : detail.message
        })
        .join(' · ')
    }
    if (error.message) {
      return error.message
    }
  }
  return error instanceof Error && error.message ? error.message : fallback
}

export function applyCourseFormErrors(
  error: unknown,
  form: FormInstance<CourseFormValues>,
): string | null {
  if (!isApiError(error) || error.details.length === 0) {
    return null
  }
  const fields = error.details
    .map((detail) => {
      const target = COURSE_FIELD_MAP[detail.field]
      return target ? { name: target, errors: [detail.message] } : null
    })
    .filter((item): item is { name: keyof CourseFormValues; errors: string[] } => item !== null)
  if (fields.length > 0) {
    form.setFields(fields)
  }
  return extractCourseErrorMessage(error, 'Dữ liệu không hợp lệ.')
}
