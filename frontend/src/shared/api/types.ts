/**
 * Kiểu dùng chung theo contracts/api-conventions.md.
 * Giữ nguyên tên field snake_case như trên wire; không đổi sang camelCase.
 */

/** Mọi ID BIGINT trên API là string để không mất độ chính xác trong JavaScript. */
export type Id = string

/** Thời gian ISO 8601 UTC, ví dụ "2026-10-06T02:05:00Z". Đổi sang giờ địa phương khi hiển thị. */
export type IsoDateTime = string

/** Danh sách phân trang: page bắt đầu từ 1, page_size 1–100. */
export interface Page<T> {
  items: T[]
  page: number
  page_size: number
  total: number
}

export interface PageParams {
  page?: number
  page_size?: number
}

/** Mã lỗi baseline trong api-conventions.md (backend có thể trả thêm mã khác). */
export type ErrorCode =
  | 'VALIDATION_ERROR'
  | 'UNAUTHENTICATED'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'EMAIL_ALREADY_EXISTS'
  | 'ACCOUNT_INACTIVE'
  | 'INVALID_STATE'
  | 'COURSE_NOT_FREE'
  | 'ENROLLMENT_REQUIRED'
  | 'INVALID_QUIZ'
  | 'INVALID_ANSWER'
  | 'FILE_TOO_LARGE'
  | 'UNSUPPORTED_FILE_TYPE'
  | 'SOURCE_TEXT_EMPTY'
  | 'AI_OUTPUT_INVALID'
  | 'AI_PROVIDER_ERROR'
  | 'AI_TIMEOUT'
  | 'RATE_LIMITED'
  | 'RESOURCE_IN_USE'
  | 'NETWORK_ERROR'
  | (string & {})

export interface FieldErrorDetail {
  field: string
  message: string
}

/** Body lỗi chuẩn: {code, message, details?, request_id?}. */
export interface ApiErrorBody {
  code: ErrorCode
  message: string
  details?: FieldErrorDetail[]
  request_id?: string
}

export type Role = 'ADMIN' | 'TEACHER' | 'STUDENT'
