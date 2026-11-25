import { useQuery } from '@tanstack/react-query'
import { Card, Col, List, Row, Space, Tag, Typography } from 'antd'
import { Link, useParams } from 'react-router'
import { EmptyState, ErrorState, LoadingState } from '../../shared/components/PageState'
import { usePageHeader } from '../../shared/components/pageHeader'
import { paths } from '../../shared/routing/paths'
import { courseKeys, getOwnedCourse } from '../marketplace/course.api'
import { CourseStatusTag } from '../marketplace/CourseStatusTag'
import { formatDateTime } from '../marketplace/format'
import { getLearningLessons, lessonKeys } from '../teaching/lessons/lesson.api'

/**
 * Trang xem khóa học chỉ đọc cho ADMIN: thông tin khóa và danh sách bài học.
 * Mọi thao tác ghi (sửa, lưu trữ, thumbnail, soạn bài) vẫn chỉ thuộc giảng viên.
 */
export function AdminCourseViewPage() {
  const { courseId = '' } = useParams()
  const courseQuery = useQuery({
    queryKey: courseKeys.owned(courseId),
    queryFn: () => getOwnedCourse(courseId),
    enabled: Boolean(courseId),
  })
  const lessonsQuery = useQuery({
    queryKey: lessonKeys.course(courseId),
    queryFn: () => getLearningLessons(courseId),
    enabled: Boolean(courseId),
  })

  usePageHeader(
    courseQuery.data
      ? {
          title: courseQuery.data.title,
          crumbs: [
            { label: 'Khóa học', to: paths.adminCourses },
            { label: courseQuery.data.title },
          ],
        }
      : null,
  )

  if (courseQuery.isLoading) return <LoadingState tip="Đang tải khóa học..." />
  if (courseQuery.isError) {
    return <ErrorState error={courseQuery.error} onRetry={() => void courseQuery.refetch()} />
  }

  const course = courseQuery.data!
  const lessons = lessonsQuery.data ?? []

  return (
    <Space orientation="vertical" size={20} style={{ width: '100%' }}>
      <Row gutter={[24, 24]}>
        <Col xs={24} lg={16}>
          <Card styles={{ body: { display: 'flex', flexDirection: 'column', gap: 16 } }}>
            {course.thumbnailUrl ? (
              <img
                src={course.thumbnailUrl}
                alt={`Ảnh bìa khóa ${course.title}`}
                style={{ width: '100%', height: 280, objectFit: 'cover', display: 'block' }}
              />
            ) : (
              <div
                aria-hidden
                style={{
                  height: 280,
                  display: 'grid',
                  placeItems: 'center',
                  background: 'var(--surface-2, #eef2ff)',
                  color: 'var(--primary, #4338ca)',
                  fontWeight: 600,
                  fontSize: 14,
                }}
              >
                {course.categoryName}
              </div>
            )}
            <Tag color="blue" style={{ alignSelf: 'flex-start' }}>
              {course.categoryName}
            </Tag>
            <Typography.Title level={2} style={{ margin: 0 }}>
              {course.title}
            </Typography.Title>
            <Typography.Paragraph style={{ margin: 0, fontSize: 16 }}>
              {course.description}
            </Typography.Paragraph>
            <Typography.Text type="secondary">
              Giảng viên: {course.teacherName || `#${course.teacherId}`}
            </Typography.Text>
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card style={{ boxShadow: 'var(--shadow-pop, 0 8px 24px rgba(0,0,0,0.08))' }}>
            <Space orientation="vertical" size={16} style={{ width: '100%' }}>
              <div>
                <Typography.Text type="secondary">Trạng thái</Typography.Text>
                <div style={{ marginTop: 4 }}>
                  <CourseStatusTag status={course.status} />
                </div>
              </div>
              <div>
                <Typography.Text type="secondary">Học phí</Typography.Text>
                <div style={{ fontSize: 24, fontWeight: 700, color: 'var(--success-text, #15803d)' }}>
                  {Number(course.price) === 0 ? 'Miễn phí' : course.price}
                </div>
              </div>
              <Typography.Text type="secondary">
                Bạn đang xem với vai trò quản trị.
              </Typography.Text>
              <List
                size="small"
                dataSource={[
                  `${lessons.length} bài học`,
                  `${course.totalStudents} học viên đã ghi danh`,
                  `Cập nhật: ${formatDateTime(course.updatedAt)}`,
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
            <Card
              title="Đề cương"
              extra={<Typography.Text type="secondary">{lessons.length} bài học</Typography.Text>}
            >
              {lessonsQuery.isError ? (
                <ErrorState error={lessonsQuery.error} onRetry={() => void lessonsQuery.refetch()} />
              ) : lessons.length === 0 ? (
                <EmptyState description="Khóa học chưa có bài học." />
              ) : (
                <List
                  dataSource={lessons}
                  renderItem={(lesson, index) => (
                    <List.Item>
                      <Space>
                        <Tag>{index + 1}</Tag>
                        <Link to={paths.adminCourseLesson(course.id, lesson.id)}>{lesson.title}</Link>
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
