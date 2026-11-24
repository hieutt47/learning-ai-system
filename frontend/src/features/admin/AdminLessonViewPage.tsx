import { DownloadOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { App, Button, Card, Col, List, Row, Space, Typography } from 'antd'
import { useNavigate, useParams } from 'react-router'
import { EmptyState, ErrorState, LoadingState } from '../../shared/components/PageState'
import { usePageHeader } from '../../shared/components/pageHeader'
import { paths } from '../../shared/routing/paths'
import {
  downloadLessonDocument,
  getLearningLessons,
  getLesson,
  getLessonDocuments,
  lessonKeys,
} from '../teaching/lessons/lesson.api'
import { getYouTubeEmbedUrl } from '../teaching/lessons/youtube-url'

/**
 * Trang xem bài học chỉ đọc cho ADMIN: video, transcript và tài liệu.
 * Không có thao tác sửa/xóa/upload — quyền ghi vẫn chỉ thuộc giảng viên.
 */
export function AdminLessonViewPage() {
  const { courseId = '', lessonId = '' } = useParams()
  const navigate = useNavigate()
  const { message } = App.useApp()

  const lessonQuery = useQuery({
    queryKey: lessonKeys.detail(lessonId),
    queryFn: () => getLesson(lessonId),
    enabled: Boolean(lessonId),
  })
  const lessonsQuery = useQuery({
    queryKey: lessonKeys.course(courseId),
    queryFn: () => getLearningLessons(courseId),
    enabled: Boolean(courseId),
  })
  const documentsQuery = useQuery({
    queryKey: lessonKeys.documents(lessonId),
    queryFn: () => getLessonDocuments(lessonId),
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

  if (lessonQuery.isLoading || lessonsQuery.isLoading || documentsQuery.isLoading) {
    return <LoadingState tip="Đang tải bài học..." />
  }
  if (lessonQuery.isError) {
    return <ErrorState error={lessonQuery.error} onRetry={() => void lessonQuery.refetch()} />
  }
  if (lessonsQuery.isError) {
    return <ErrorState error={lessonsQuery.error} onRetry={() => void lessonsQuery.refetch()} />
  }

  const lesson = lessonQuery.data
  if (!lesson || lesson.courseId !== courseId) {
    return <ErrorState error={new Error('Bài học không thuộc khóa học này.')} />
  }
  const embedUrl = getYouTubeEmbedUrl(lesson.videoUrl)
  const documents = documentsQuery.data ?? []

  return (
    <Row gutter={[20, 20]}>
      <Col xs={24} lg={17}>
        <Space orientation="vertical" size={20} style={{ width: '100%' }}>
          <Card title={lesson.title}>
            {embedUrl ? (
              <iframe
                title={lesson.title}
                src={embedUrl}
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
                style={{ width: '100%', aspectRatio: '16 / 9', border: 0, borderRadius: 8 }}
              />
            ) : (
              <Typography.Paragraph>Giảng viên chưa cung cấp video hợp lệ.</Typography.Paragraph>
            )}
            {lesson.videoUrl && (
              <Button type="link" href={lesson.videoUrl} target="_blank" rel="noreferrer">
                Mở trên YouTube
              </Button>
            )}
          </Card>
          {lesson.transcriptText && (
            <Card title="Transcript">
              <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', margin: 0 }}>
                {lesson.transcriptText}
              </Typography.Paragraph>
            </Card>
          )}
          <Card title="Tài liệu bài học">
            {documentsQuery.isError ? (
              <ErrorState error={documentsQuery.error} onRetry={() => void documentsQuery.refetch()} />
            ) : documents.length === 0 ? (
              <EmptyState description="Bài học chưa có tài liệu." />
            ) : (
              <List
                dataSource={documents}
                renderItem={(document) => (
                  <List.Item
                    actions={[
                      <Button
                        key="download"
                        type="primary"
                        icon={<DownloadOutlined />}
                        onClick={() =>
                          void downloadLessonDocument(document.id, document.originalName).catch((error: Error) =>
                            message.error(error.message),
                          )
                        }
                      >
                        Tải xuống
                      </Button>,
                    ]}
                  >
                    <List.Item.Meta
                      title={document.originalName}
                      description={`${(document.sizeBytes / 1024).toFixed(1)} KB`}
                    />
                  </List.Item>
                )}
              />
            )}
          </Card>
        </Space>
      </Col>
      <Col xs={24} lg={7}>
        <Card title="Nội dung khóa học">
          <List
            dataSource={lessonsQuery.data ?? []}
            renderItem={(item) => (
              <List.Item
                onClick={() => navigate(paths.adminCourseLesson(courseId, item.id))}
                style={{ cursor: 'pointer', fontWeight: item.id === lessonId ? 700 : 400 }}
              >
                {item.orderIndex}. {item.title}
              </List.Item>
            )}
          />
        </Card>
      </Col>
    </Row>
  )
}
