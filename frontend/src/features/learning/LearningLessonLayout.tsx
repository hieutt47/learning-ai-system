import { useQuery } from '@tanstack/react-query'
import { Button, Card, Col, List, Row, Typography } from 'antd'
import { Outlet, useNavigate, useParams } from 'react-router'
import { ErrorState, LoadingState } from '../../shared/components/PageState'
import { paths } from '../../shared/routing/paths'
import { getLearningLessons, getLesson, lessonKeys } from '../teaching/lessons/lesson.api'
import { getYouTubeEmbedUrl } from '../teaching/lessons/youtube-url'

export function LearningLessonLayout() {
  const { courseId = '', lessonId = '' } = useParams()
  const navigate = useNavigate()
  const lessonQuery = useQuery({ queryKey: lessonKeys.detail(lessonId), queryFn: () => getLesson(lessonId), enabled: Boolean(lessonId) })
  const lessonsQuery = useQuery({ queryKey: lessonKeys.course(courseId), queryFn: () => getLearningLessons(courseId), enabled: Boolean(courseId) })

  if (lessonQuery.isLoading || lessonsQuery.isLoading) return <LoadingState tip="Đang tải nội dung bài học…" />
  if (lessonQuery.isError) return <ErrorState error={lessonQuery.error} onRetry={() => void lessonQuery.refetch()} />
  if (lessonsQuery.isError) return <ErrorState error={lessonsQuery.error} onRetry={() => void lessonsQuery.refetch()} />

  const lesson = lessonQuery.data
  if (!lesson || lesson.courseId !== courseId) return <ErrorState error={new Error('Bài học không thuộc khóa học này.')} />
  const embedUrl = getYouTubeEmbedUrl(lesson.videoUrl)

  return (
    <Row gutter={[20, 20]}>
      <Col xs={24} lg={17}>
        <Card title={lesson.title}>
          {embedUrl ? (
            <iframe title={lesson.title} src={embedUrl} allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowFullScreen style={{ width: '100%', aspectRatio: '16 / 9', border: 0, borderRadius: 8 }} />
          ) : (
            <Typography.Paragraph>Giảng viên chưa cung cấp video hợp lệ.</Typography.Paragraph>
          )}
          {lesson.videoUrl && <Button type="link" href={lesson.videoUrl} target="_blank" rel="noreferrer">Mở trên YouTube</Button>}
        </Card>
        <div style={{ marginTop: 20 }}><Outlet /></div>
      </Col>
      <Col xs={24} lg={7}>
        <Card title="Nội dung khóa học">
          <List dataSource={lessonsQuery.data ?? []} renderItem={(item) => (
            <List.Item onClick={() => navigate(paths.learnLesson(courseId, item.id))} style={{ cursor: 'pointer', fontWeight: item.id === lessonId ? 700 : 400 }}>
              {item.orderIndex}. {item.title}
            </List.Item>
          )} />
        </Card>
      </Col>
    </Row>
  )
}
