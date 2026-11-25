import type { Id } from '../../../shared/api/types'
import type { LessonListItem } from './lesson.types'

export type MoveDirection = 'UP' | 'DOWN'

export function canMoveLesson(
  lessons: readonly LessonListItem[],
  lessonId: Id,
  direction: MoveDirection,
): boolean {
  const orderedLessons = [...lessons].sort((first, second) => first.orderIndex - second.orderIndex)
  const currentIndex = orderedLessons.findIndex((lesson) => lesson.id === lessonId)
  if (currentIndex === -1) return false

  const currentLesson = orderedLessons[currentIndex]
  if (currentLesson.status === 'ARCHIVED') return false

  const targetIndex = direction === 'UP' ? currentIndex - 1 : currentIndex + 1
  if (targetIndex < 0 || targetIndex >= orderedLessons.length) return false

  const targetLesson = orderedLessons[targetIndex]
  return targetLesson.status !== 'ARCHIVED'
}

export function reorderLessons(
  lessons: readonly LessonListItem[],
  lessonId: Id,
  direction: MoveDirection,
): LessonListItem[] {
  const orderedLessons = [...lessons].sort((first, second) => first.orderIndex - second.orderIndex)
  if (!canMoveLesson(orderedLessons, lessonId, direction)) {
    return orderedLessons
  }

  const currentIndex = orderedLessons.findIndex((lesson) => lesson.id === lessonId)
  const targetIndex = direction === 'UP' ? currentIndex - 1 : currentIndex + 1

  const currentLesson = orderedLessons[currentIndex]
  orderedLessons[currentIndex] = orderedLessons[targetIndex]
  orderedLessons[targetIndex] = currentLesson

  return orderedLessons.map((lesson, index) => ({
    ...lesson,
    orderIndex: index + 1,
  }))
}
