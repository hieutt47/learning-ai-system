import type { LessonListItem } from './lesson.types'
import type { Id } from '../../../shared/api/types'

export type MoveDirection = 'UP' | 'DOWN'

export function reorderLessons(
  lessons: readonly LessonListItem[],
  lessonId: Id,
  direction: MoveDirection,
): LessonListItem[] {
  const orderedLessons = [...lessons].sort(
    (first, second) => first.orderIndex - second.orderIndex,
  )

  const currentIndex = orderedLessons.findIndex(
    (lesson) => lesson.id === lessonId,
  )

  if (currentIndex === -1) {
    return orderedLessons
  }

  const targetIndex =
    direction === 'UP' ? currentIndex - 1 : currentIndex + 1

  if (targetIndex < 0 || targetIndex >= orderedLessons.length) {
    return orderedLessons
  }

  const currentLesson = orderedLessons[currentIndex]
  orderedLessons[currentIndex] = orderedLessons[targetIndex]
  orderedLessons[targetIndex] = currentLesson

  return orderedLessons.map((lesson, index) => ({
    ...lesson,
    orderIndex: index + 1,
  }))
}
