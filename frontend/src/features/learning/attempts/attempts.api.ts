import { apiGet } from '../../../shared/api/client'
import type { Page, PageParams } from '../../../shared/api/types'
import type { AttemptStatus, AttemptSummaryItem } from './attempts.types'

interface AttemptSummaryWire {
  id: string | number
  quiz_id: string | number
  quiz_title?: string | null
  course_id?: string | number | null
  course_title?: string | null
  status: AttemptStatus
  score?: number | null
  started_at: string
  submitted_at?: string | null
}

export const attemptKeys = {
  mine: (params?: PageParams) => ['attempts', 'mine', params] as const,
}

export async function getMyAttempts(params?: PageParams): Promise<Page<AttemptSummaryItem>> {
  try {
    const page = await apiGet<Page<AttemptSummaryWire>>('/users/me/attempts', {
      query: { ...params },
    })
    return {
      ...page,
      items: (page.items ?? []).map((wire) => ({
        id: String(wire.id),
        quizId: String(wire.quiz_id),
        quizTitle: wire.quiz_title ?? `Quiz #${wire.quiz_id}`,
        courseId: wire.course_id != null ? String(wire.course_id) : undefined,
        courseTitle: wire.course_title ?? undefined,
        status: wire.status,
        score: wire.score != null ? Number(wire.score) : null,
        startedAt: wire.started_at,
        submittedAt: wire.submitted_at ?? null,
      })),
    }
  } catch {
    // Nếu backend chưa triển khai endpoint này (trả 404), trả về danh sách rỗng để hiển thị empty state
    return {
      items: [],
      page: 1,
      page_size: 10,
      total: 0,
    }
  }
}
