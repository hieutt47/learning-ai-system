import { useQuery } from '@tanstack/react-query'
import { courseKeys, getOwnedCourse } from '../../marketplace/course.api'
import { getLesson, lessonKeys } from './lesson.api'

export function useLessonEditability(lessonId: string) {
  const lessonQuery = useQuery({
    queryKey: lessonKeys.detail(lessonId),
    queryFn: () => getLesson(lessonId),
    enabled: Boolean(lessonId),
  })
  const courseId = lessonQuery.data?.courseId ?? ''
  const courseQuery = useQuery({
    queryKey: courseKeys.owned(courseId),
    queryFn: () => getOwnedCourse(courseId),
    enabled: Boolean(courseId),
  })

  return {
    readOnly: lessonQuery.data?.status === 'ARCHIVED' || courseQuery.data?.status === 'Archived',
    loading: Boolean(lessonId) && (lessonQuery.isLoading || courseQuery.isLoading),
    error: lessonQuery.error ?? courseQuery.error,
    retry: () => {
      if (lessonQuery.isError) void lessonQuery.refetch()
      if (courseQuery.isError) void courseQuery.refetch()
    },
  }
}
