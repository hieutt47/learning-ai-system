import { useMutation, useQueryClient } from '@tanstack/react-query'
import { App } from 'antd'
import { isApiError } from '../../../shared/api/client'
import type { Id } from '../../../shared/api/types'
import { changeLessonStatus } from './lesson.api'

export type LessonStatusChange = { lessonId: Id; status: 'PUBLISHED' | 'ARCHIVED' }

export function useLessonStatusMutation() {
  const queryClient = useQueryClient()
  const { message } = App.useApp()

  return useMutation({
    mutationFn: ({ lessonId, status }: LessonStatusChange) => changeLessonStatus(lessonId, status),
    onSuccess: async (lesson) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['lessons'] }),
        queryClient.invalidateQueries({ queryKey: ['courses'] }),
      ])
      void message.success(lesson.status === 'PUBLISHED' ? 'Bài học đã hiển thị cho học viên.' : 'Đã lưu trữ bài học, lịch sử quiz được giữ lại.')
    },
    onError: (error: Error, change) => {
      const text = isApiError(error) && error.code === 'INVALID_STATE' && change.status === 'ARCHIVED'
        ? 'Không thể lưu trữ bài học cuối của khóa đang xuất bản. Hãy xuất bản bài thay thế hoặc gỡ xuất bản khóa trước.'
        : error.message
      void message.error(text)
    },
  })
}
