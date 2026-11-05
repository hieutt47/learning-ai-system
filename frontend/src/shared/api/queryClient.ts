import { QueryClient } from '@tanstack/react-query'
import { isApiError } from './client'

/**
 * TanStack Query: cache dữ liệu GET, tự lo loading/error.
 * - GET chỉ thử lại 1 lần với lỗi mạng/5xx, không thử lại lỗi 4xx (sai quyền, không tồn tại...).
 * - Mutation (POST/PATCH/DELETE) KHÔNG tự thử lại: tạo nội dung, sinh AI có thể bị trùng hoặc tốn phí
 *   (api-conventions.md). Người dùng bấm lại thủ công.
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      refetchOnWindowFocus: false,
      retry: (failureCount, error) => {
        if (isApiError(error) && error.status >= 400 && error.status < 500) {
          return false
        }
        return failureCount < 1
      },
    },
    mutations: {
      retry: false,
    },
  },
})
