import type { CourseQuizFilter, CourseQuizStatistics, QuizStatus } from './statistics.types'

const numberFormatter = new Intl.NumberFormat('vi-VN')
const decimalFormatter = new Intl.NumberFormat('vi-VN', {
  minimumFractionDigits: 0,
  maximumFractionDigits: 2,
})

export function formatCount(value: number): string {
  return numberFormatter.format(value)
}

export function formatScore(value: number | null, fallback = 'Chưa có'): string {
  if (value === null || !Number.isFinite(value)) {
    return fallback
  }
  return decimalFormatter.format(value)
}

export function formatPercent(value: number): string {
  if (!Number.isFinite(value)) {
    return '0%'
  }
  return `${decimalFormatter.format(value)}%`
}

export function quizStatusMeta(status: QuizStatus): { label: string; color: string } {
  switch (status) {
    case 'PUBLISHED':
      return { label: 'Đang xuất bản', color: 'success' }
    case 'CLOSED':
      return { label: 'Đã đóng', color: 'default' }
    case 'DRAFT':
      return { label: 'Bản nháp', color: 'warning' }
  }
}

export function isActivePublishedQuiz(quiz: CourseQuizStatistics): boolean {
  return quiz.status === 'PUBLISHED' && quiz.lessonStatus === 'PUBLISHED'
}

export function filterCourseQuizzes(
  quizzes: readonly CourseQuizStatistics[],
  filter: CourseQuizFilter,
): CourseQuizStatistics[] {
  if (filter === 'all') {
    return [...quizzes]
  }
  return quizzes.filter(isActivePublishedQuiz)
}

export function scoreBadgeColor(score: number): string {
  if (score >= 80) return 'success'
  if (score >= 50) return 'warning'
  return 'error'
}
