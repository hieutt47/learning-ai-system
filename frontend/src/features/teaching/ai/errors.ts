import { isApiError } from '../../../shared/api/client'

const errorMessages: Record<string, string> = {
  AI_TIMEOUT: 'AI phản hồi quá lâu. Vui lòng thử lại.',
  AI_PROVIDER_ERROR: 'Dịch vụ AI đang gặp lỗi.',
  AI_OUTPUT_INVALID: 'AI trả về nội dung không hợp lệ.',
  SOURCE_TEXT_EMPTY: 'Nguồn chưa có nội dung để AI đọc.',
  RATE_LIMITED: 'Bạn đã gửi quá nhiều yêu cầu.',
}

export function aiErrorText(error: unknown): string {
  if (isApiError(error)) return errorMessages[error.code] ?? error.message
  return error instanceof Error ? error.message : 'Không thể kết nối máy chủ.'
}
