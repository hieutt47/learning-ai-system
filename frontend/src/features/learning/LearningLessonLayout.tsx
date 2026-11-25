import { ArrowLeftOutlined, ArrowRightOutlined, ExportOutlined, PlayCircleOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Button, Card, Col, Row, Space, Tag } from 'antd'
import { Link, Outlet, useNavigate, useParams } from 'react-router'
import { isApiError } from '../../shared/api/client'
import { EmptyState, ErrorState, LoadingState } from '../../shared/components/PageState'
import { usePageHeader } from '../../shared/components/pageHeader'
import { paths } from '../../shared/routing/paths'
import { getLearningLessons, getLesson, lessonKeys } from '../teaching/lessons/lesson.api'
import { getYouTubeEmbedUrl } from '../teaching/lessons/youtube-url'
import '../teaching/lessons/lessons-ui.css'

export function LearningLessonLayout() {
  const { courseId = '', lessonId = '' } = useParams()
  const navigate = useNavigate()
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

  usePageHeader(
    lessonQuery.data
      ? {
          title: lessonQuery.data.title,
          crumbs: [
            { label: 'Khóa học của tôi', to: paths.myCourses },
            { label: lessonQuery.data.title },
          ],
        }
      : null,
  )

  if (lessonQuery.isLoading || lessonsQuery.isLoading) {
    return <LoadingState tip="Đang tải nội dung bài học…" />
  }

  const enrollmentRequiredError = [lessonQuery.error, lessonsQuery.error].find(
    (err) => isApiError(err) && err.code === 'ENROLLMENT_REQUIRED',
  )
  if (enrollmentRequiredError) {
    return (
      <Card>
        <EmptyState
          description="Bạn chưa ghi danh khóa học này."
          action={
            <Link to={paths.courseDetail(courseId)}>
              <Button type="primary">Ghi danh miễn phí</Button>
            </Link>
          }
        />
      </Card>
    )
  }

  if (lessonQuery.isError) return <ErrorState error={lessonQuery.error} onRetry={() => void lessonQuery.refetch()} />
  if (lessonsQuery.isError) return <ErrorState error={lessonsQuery.error} onRetry={() => void lessonsQuery.refetch()} />

  const lesson = lessonQuery.data
  if (!lesson || lesson.courseId !== courseId) {
    return <ErrorState error={new Error('Bài học không thuộc khóa học này.')} />
  }

  const lessons = lessonsQuery.data ?? []
  const currentIndex = lessons.findIndex((item) => item.id === lessonId)
  const prevLesson = currentIndex > 0 ? lessons[currentIndex - 1] : null
  const nextLesson = currentIndex >= 0 && currentIndex < lessons.length - 1 ? lessons[currentIndex + 1] : null
  const embedUrl = getYouTubeEmbedUrl(lesson.videoUrl)

  return (
    <Row gutter={[20, 20]} align="top">
      <Col xs={24} lg={17}>
        <Card
          className="r3-card"
          title={lesson.title}
          extra={
            currentIndex >= 0 ? (
              <Tag color="blue" style={{ margin: 0, fontWeight: 600 }}>
                Bài {currentIndex + 1} / {lessons.length}
              </Tag>
            ) : null
          }
        >
          {embedUrl ? (
            <div className="r3-video-frame">
              <iframe
                title={lesson.title}
                src={embedUrl}
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
              />
            </div>
          ) : (
            <div className="r3-video-empty">
              <span className="r3-video-empty-icon" aria-hidden="true">
                <PlayCircleOutlined />
              </span>
              <strong>Chưa có video bài giảng</strong>
              <span>Giảng viên chưa cung cấp liên kết YouTube hợp lệ cho bài học này.</span>
            </div>
          )}
          <div className="r3-learn-bar">
            <div>
              {lesson.videoUrl ? (
                <Button
                  type="link"
                  icon={<ExportOutlined />}
                  href={lesson.videoUrl}
                  target="_blank"
                  rel="noreferrer"
                  style={{ paddingInline: 0 }}
                >
                  Mở trên YouTube
                </Button>
              ) : null}
            </div>
            <Space size={8}>
              <Button
                icon={<ArrowLeftOutlined />}
                disabled={!prevLesson}
                onClick={() => prevLesson && navigate(paths.learnLesson(courseId, prevLesson.id))}
              >
                Bài trước
              </Button>
              <Button
                type="primary"
                icon={<ArrowRightOutlined />}
                iconPlacement="end"
                disabled={!nextLesson}
                onClick={() => nextLesson && navigate(paths.learnLesson(courseId, nextLesson.id))}
              >
                Bài tiếp theo
              </Button>
            </Space>
          </div>
        </Card>
        <div style={{ marginTop: 20 }}>
          <Outlet />
        </div>
      </Col>
      <Col xs={24} lg={7}>
        <div className="r3-learn-sidebar">
          <Card className="r3-card" title={`Nội dung khóa học (${lessons.length})`}>
            <div className="r3-playlist">
              {lessons.map((item, index) => {
                const active = item.id === lessonId
                return (
                  <button
                    key={item.id}
                    type="button"
                    className={`r3-playlist-item${active ? ' is-active' : ''}`}
                    aria-current={active ? 'page' : undefined}
                    onClick={() => navigate(paths.learnLesson(courseId, item.id))}
                  >
                    <span className="r3-playlist-num">{index + 1}</span>
                    <span className="r3-playlist-title">{item.title}</span>
                  </button>
                )
              })}
            </div>
          </Card>
        </div>
      </Col>
    </Row>
  )
}
