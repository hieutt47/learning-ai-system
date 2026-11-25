import { useQuery } from '@tanstack/react-query'
import { Card, Typography } from 'antd'
import { useParams } from 'react-router'
import { usePageHeader } from '../../shared/components/pageHeader'
import { paths } from '../../shared/routing/paths'
import { getLesson, lessonKeys } from '../teaching/lessons/lesson.api'

interface AdminLessonSectionProps {
  title: string
  description: string
}

/**
 * Khung tạm cho các tab Quiz / AI & Flashcard của bài học khi ADMIN xem.
 * Chỉ dựng tiêu đề + tab; nội dung chi tiết do người phụ trách tiếp theo làm.
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

export function AdminLessonAiPage() {
  return (
    <AdminLessonSection
      title="AI & Flashcard"
      description="Phần AI và flashcard của bài học sẽ được hiển thị tại đây. Nội dung do người phụ trách tiếp theo hoàn thiện."
    />
  )
}
