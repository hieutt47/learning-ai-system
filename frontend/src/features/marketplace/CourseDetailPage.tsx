import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Card, Col, List, Row, Space, Tag, Typography } from 'antd'
import { Link, useParams } from 'react-router'
import { useCurrentUser } from '../../shared/auth/currentUser'
import { EmptyState, ErrorState, LoadingState } from '../../shared/components/PageState'
import { usePageHeader } from '../../shared/components/pageHeader'
import { paths } from '../../shared/routing/paths'
import { courseKeys, enroll, getMyCourses, getPublicCourse } from './course.api'
import { CourseCover } from './CourseCard'

/**
 * Chi tiết khóa học (prototype #course): giới thiệu, đề cương, thẻ ghi danh bên phải.
 *
 * Nút hành động phụ thuộc người xem: khách → đăng nhập rồi quay lại; sinh viên →
 * ghi danh hoặc vào học; giảng viên/admin → không ghi danh được, chỉ báo.
 */
export function CourseDetailPage() {
  const { courseId = '' } = useParams()
  const queryClient = useQueryClient()
  const { message } = App.useApp()
  const viewer = useCurrentUser()

  const courseQuery = useQuery({
    queryKey: courseKeys.publicDetail(courseId),
    queryFn: () => getPublicCourse(courseId),
    enabled: Boolean(courseId),
  })

  // Đã ghi danh chưa: dựa vào "Khóa học của tôi", cùng nguồn với trang đó nên
  // sau khi ghi danh cả hai trang cùng cập nhật.
  const mineQuery = useQuery({
    queryKey: courseKeys.mine,
    queryFn: () => getMyCourses({ page: 1, page_size: 100 }),
    enabled: viewer.role === 'STUDENT',
  })

  usePageHeader(
    courseQuery.data
      ? {
          title: courseQuery.data.title,
          crumbs: [
            { label: 'Khám phá khóa học', to: paths.explore },
            { label: courseQuery.data.category.name, to: paths.explore },
            { label: courseQuery.data.title },
          ],
        }
      : null,
  )

  const enrollMutation = useMutation({
    mutationFn: () => enroll(courseId),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: courseKeys.mine }),
        queryClient.invalidateQueries({ queryKey: courseKeys.publicDetail(courseId) }),
      ])
      void message.success('Ghi danh thành công.')
    },
    onError: (error: Error) => void message.error(error.message),
  })

  if (courseQuery.isLoading) return <LoadingState tip="Đang tải khóa học…" />
  if (courseQuery.isError) return <ErrorState error={courseQuery.error} onRetry={() => void courseQuery.refetch()} />

  const course = courseQuery.data!
  const enrolled = mineQuery.data?.items.some((item) => item.id === course.id) ?? false
  const firstLessonId = course.outline[0]?.id

  const primaryAction = (() => {
    if (viewer.role === 'GUEST') {
      return (
        <Link to={`${paths.login}?redirect=${encodeURIComponent(paths.courseDetail(course.id))}`}>
          <Button type="primary" block>
            Ghi danh miễn phí
          </Button>
        </Link>
      )
    }
    if (viewer.role !== 'STUDENT') {
      return (
        <Typography.Text type="secondary">
          Chỉ sinh viên mới ghi danh được. Bạn đang xem với vai trò {viewer.role === 'TEACHER' ? 'giảng viên' : 'quản trị'}.
        </Typography.Text>
      )
    }
    if (enrolled && firstLessonId) {
      return (
        <Link to={paths.learnLesson(course.id, firstLessonId)}>
          <Button type="primary" block>
            Vào học
          </Button>
        </Link>
      )
    }
    return (
      <Button type="primary" block loading={enrollMutation.isPending} onClick={() => enrollMutation.mutate()}>
        Ghi danh miễn phí
      </Button>
    )
  })()

  return (
    <Space orientation="vertical" size={20} style={{ width: '100%' }}>
      <Row gutter={[24, 24]}>
        <Col xs={24} lg={16}>
          <Card styles={{ body: { display: 'flex', flexDirection: 'column', gap: 16 } }}>
            <CourseCover course={course} height={280} />
            <Tag color="blue" style={{ alignSelf: 'flex-start' }}>
              {course.category.name}
            </Tag>
            <Typography.Title level={2} style={{ margin: 0 }}>
              {course.title}
            </Typography.Title>
            <Typography.Paragraph style={{ margin: 0, fontSize: 16 }}>
              {course.description}
            </Typography.Paragraph>
            <Typography.Text type="secondary">Giảng viên: {course.teacher.fullName}</Typography.Text>
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card style={{ boxShadow: 'var(--shadow-pop, 0 8px 24px rgba(0,0,0,0.08))' }}>
            <Space orientation="vertical" size={16} style={{ width: '100%' }}>
              <div>
                <Typography.Text type="secondary">Học phí</Typography.Text>
                <div style={{ fontSize: 24, fontWeight: 700, color: 'var(--success-text, #15803d)' }}>
                  Miễn phí
                </div>
              </div>
              {primaryAction}
              <List
                size="small"
                dataSource={[
                  `${course.lessonCount} bài học`,
                  `${course.totalStudents} học viên đã ghi danh`,
                  'Học theo tiến độ của bạn',
                ]}
                renderItem={(item) => <List.Item>{item}</List.Item>}
              />
            </Space>
          </Card>
        </Col>
      </Row>

      <Row gutter={[24, 24]}>
        <Col xs={24} lg={16}>
          <Space orientation="vertical" size={20} style={{ width: '100%' }}>
            <Card title="Giới thiệu khóa học">
              <Typography.Paragraph style={{ margin: 0 }}>{course.description}</Typography.Paragraph>
            </Card>
            <Card title="Đề cương" extra={<Typography.Text type="secondary">{course.outline.length} bài học</Typography.Text>}>
              {course.outline.length === 0 ? (
                <EmptyState description="Giảng viên chưa thêm bài học." />
              ) : (
                <List
                  dataSource={course.outline}
                  renderItem={(lesson, index) => (
                    <List.Item>
                      <Space>
                        <Tag>{index + 1}</Tag>
                        <span>{lesson.title}</span>
                      </Space>
                    </List.Item>
                  )}
                />
              )}
            </Card>
          </Space>
        </Col>
      </Row>
    </Space>
  )
}
