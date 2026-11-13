import { apiGet } from '../../../shared/api/client'
import type { Page, PageParams } from '../../../shared/api/types'
import type { AttemptDetail, AttemptStatus, AttemptSummaryItem } from './attempts.types'

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

const STORAGE_KEY = 'learningai_attempts'

function getStoredAttempts(): AttemptDetail[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

export function saveStoredAttempt(attempt: AttemptDetail): void {
  try {
    const attempts = getStoredAttempts()
    const index = attempts.findIndex((a) => a.id === attempt.id)
    if (index >= 0) {
      attempts[index] = attempt
    } else {
      attempts.unshift(attempt)
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(attempts))
  } catch {
    // ignore
  }
}

export function getStoredAttempt(attemptId: string): AttemptDetail | null {
  const attempts = getStoredAttempts()
  return attempts.find((a) => a.id === attemptId) ?? null
}

export const attemptKeys = {
  mine: (params?: PageParams) => ['attempts', 'mine', params] as const,
  detail: (attemptId: string) => ['attempts', 'detail', attemptId] as const,
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
    // Khi backend chưa cài đặt API attempts, đọc từ localStorage để người dùng thấy ngay lịch sử đã làm
    const stored = getStoredAttempts()
    const page = params?.page ?? 1
    const pageSize = params?.page_size ?? 10
    const start = (page - 1) * pageSize
    const paged = stored.slice(start, start + pageSize).map((a) => ({
      id: a.id,
      quizId: a.quizId,
      quizTitle: a.quizTitle,
      courseId: a.courseId,
      status: a.status,
      score: a.score,
      startedAt: a.startedAt,
      submittedAt: a.submittedAt,
    }))
    return {
      items: paged,
      page,
      page_size: pageSize,
      total: stored.length,
    }
  }
}

export async function getAttemptDetail(attemptId: string): Promise<AttemptDetail> {
  try {
    return await apiGet<AttemptDetail>(`/attempts/${attemptId}`)
  } catch {
    const local = getStoredAttempt(attemptId)
    if (local) return local
    throw new Error('Không tìm thấy thông tin lượt làm bài.')
  }
}
