import { useQuery } from '@tanstack/react-query'
import { Space } from 'antd'
import { useParams } from 'react-router'
import { ErrorState, LoadingState } from '../../shared/components/PageState'
import { usePageHeader } from '../../shared/components/pageHeader'
import { paths } from '../../shared/routing/paths'
import { FlashcardTable } from '../teaching/ai/FlashcardTable'
import { getLesson, lessonKeys } from '../teaching/lessons/lesson.api'
import { AdminLessonQuizCard } from './AdminLessonQuizCard'
import { AiGenerationHistoryCard } from './AiGenerationHistoryCard'

/** Tab Quiz chỉ đọc của một bài học dành cho ADMIN. */
export function AdminLessonQuizPage() {
  const { courseId = '', lessonId = '' } = useParams()
  const lessonQuery = useQuery({
    queryKey: lessonKeys.detail(lessonId),
    queryFn: () => getLesson(lessonId),
    enabled: Boolean(lessonId),
  })

  usePageHeader(
    lessonQuery.data
      ? {
          title: lessonQuery.data.title,
          crumbs: [
            { label: 'Khóa học', to: paths.adminCourse(courseId) },
            { label: lessonQuery.data.title },
          ],
        }
      : null,
  )

  if (lessonQuery.isLoading) return <LoadingState tip="Đang tải bài học..." />
  if (lessonQuery.isError) {
    return <ErrorState error={lessonQuery.error} onRetry={() => void lessonQuery.refetch()} />
  }
  if (!lessonQuery.data || lessonQuery.data.courseId !== courseId) {
    return <ErrorState error={new Error('Bài học không thuộc khóa học này.')} />
  }

  return <AdminLessonQuizCard lessonId={lessonId} />
}

/** Tab AI & Flashcard chỉ đọc của một bài học dành cho ADMIN. */
export function AdminLessonAiPage() {
  const { courseId = '', lessonId = '' } = useParams()
  const lessonQuery = useQuery({
    queryKey: lessonKeys.detail(lessonId),
    queryFn: () => getLesson(lessonId),
    enabled: Boolean(lessonId),
  })

  usePageHeader(
    lessonQuery.data
      ? {
          title: lessonQuery.data.title,
          crumbs: [
            { label: 'Khóa học', to: paths.adminCourse(courseId) },
            { label: lessonQuery.data.title },
          ],
        }
      : null,
  )

  if (lessonQuery.isLoading) return <LoadingState tip="Đang tải bài học..." />
  if (lessonQuery.isError) {
    return <ErrorState error={lessonQuery.error} onRetry={() => void lessonQuery.refetch()} />
  }

  return (
    <Space orientation="vertical" size={20} style={{ width: '100%' }}>
      <AiGenerationHistoryCard lessonId={lessonId} />
      <FlashcardTable lessonId={lessonId} readOnly />
    </Space>
  )
}
