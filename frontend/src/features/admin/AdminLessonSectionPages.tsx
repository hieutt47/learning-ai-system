import { useQuery } from '@tanstack/react-query'
import { Card, Space, Typography } from 'antd'
import { useParams } from 'react-router'
import { ErrorState, LoadingState } from '../../shared/components/PageState'
import { usePageHeader } from '../../shared/components/pageHeader'
import { paths } from '../../shared/routing/paths'
import { FlashcardTable } from '../teaching/ai/FlashcardTable'
import { getLesson, lessonKeys } from '../teaching/lessons/lesson.api'
import { AiGenerationHistoryCard } from './AiGenerationHistoryCard'

interface AdminLessonSectionProps {
  title: string
  description: string
}

/**
 * Khung tạm cho tab Quiz của bài học khi ADMIN xem.
 */
function AdminLessonSection({ title, description }: AdminLessonSectionProps) {
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

  return (
    <Card title={title}>
      <Typography.Paragraph style={{ margin: 0 }}>{description}</Typography.Paragraph>
    </Card>
  )
}

export function AdminLessonQuizPage() {
  return (
    <AdminLessonSection
      title="Quiz"
      description="Phần quiz của bài học sẽ được hiển thị tại đây. Nội dung do người phụ trách tiếp theo hoàn thiện."
    />
  )
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
