import { ReloadOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Button, Card, Col, Row, Select, Space, Statistic, Typography } from 'antd'
import { Link, useSearchParams } from 'react-router'
import { EmptyState, ErrorState, LoadingState } from '../../../shared/components/PageState'
import { usePageHeader } from '../../../shared/components/pageHeader'
import { paths } from '../../../shared/routing/paths'
import { formatDateTime } from '../../../shared/utils/format'
import { CourseStatusTag } from '../../marketplace/CourseStatusTag'
import { listOwnedCourses } from '../../marketplace/course.api'
import { CompletionRateCard } from './components/CompletionRateCard'
import { CourseQuizzesTable } from './components/CourseQuizzesTable'
import { getCourseStatistics, statisticsKeys } from './statistics.api'
import { formatCount, formatPercent, formatScore } from './statistics.format'

export function CourseStatisticsPage() {
  usePageHeader({ title: 'Thống kê khóa học' })
  const [searchParams, setSearchParams] = useSearchParams()
  const requestedCourseId = searchParams.get('courseId') ?? undefined

  const coursesQuery = useQuery({
    queryKey: statisticsKeys.courseOptions,
    queryFn: () => listOwnedCourses({ page: 1, page_size: 100 }),
  })

  const courses = coursesQuery.data?.items ?? []
  const selectedCourse =
    courses.find((course) => course.id === requestedCourseId) ?? courses[0]
  const selectedCourseId = selectedCourse?.id

  const statsQuery = useQuery({
    queryKey: statisticsKeys.course(selectedCourseId ?? ''),
    queryFn: () => getCourseStatistics(selectedCourseId!),
    enabled: Boolean(selectedCourseId),
  })

  const handleSelectCourse = (courseId: string) => {
    setSearchParams({ courseId }, { replace: true })
  }

  if (coursesQuery.isLoading) {
    return <LoadingState tip="Đang tải danh sách khóa học…" />
  }
  if (coursesQuery.isError) {
    return <ErrorState error={coursesQuery.error} onRetry={() => void coursesQuery.refetch()} />
  }

  if (courses.length === 0) {
    return (
      <Card>
        <EmptyState
          description="Bạn chưa có khóa học nào để thống kê. Tạo khóa học, thêm bài học và xuất bản quiz để theo dõi kết quả học tập."
          action={
            <Link to={paths.teachingCourses}>
              <Button type="primary">Đi tới Khóa học giảng dạy</Button>
            </Link>
          }
        />
      </Card>
    )
  }

  const stats = statsQuery.data

  return (
    <Space orientation="vertical" size={20} style={{ width: '100%' }}>
      <Card>
        <Row gutter={[16, 12]} justify="space-between" align="middle">
          <Col xs={24} md={14}>
            <Space wrap align="center">
              <Typography.Text strong>Khóa học:</Typography.Text>
              <Select
                aria-label="Chọn khóa học"
                placeholder="Chọn khóa học"
                value={selectedCourseId}
                onChange={handleSelectCourse}
                style={{ width: 320, maxWidth: '100%' }}
                options={courses.map((course) => ({
                  label: course.title,
                  value: course.id,
                }))}
              />
              {selectedCourse && <CourseStatusTag status={selectedCourse.status} />}
            </Space>
          </Col>
          <Col xs={24} md={10} style={{ textAlign: 'right' }}>
            <Space wrap>
              {stats && (
                <Typography.Text type="secondary" style={{ fontSize: 13 }}>
                  Cập nhật lúc {formatDateTime(stats.generatedAt)}
                </Typography.Text>
              )}
              <Button
                icon={<ReloadOutlined />}
                loading={statsQuery.isFetching}
                onClick={() => void statsQuery.refetch()}
              >
                Làm mới
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      {statsQuery.isLoading ? (
        <LoadingState tip="Đang tổng hợp số liệu khóa học…" />
      ) : statsQuery.isError ? (
        <ErrorState error={statsQuery.error} onRetry={() => void statsQuery.refetch()} />
      ) : stats ? (
        <>
          <Row gutter={[16, 16]}>
            <Col xs={24} sm={12} lg={8} xl={4}>
              <Card>
                <Statistic title="Học viên ghi danh" value={formatCount(stats.enrolledStudents)} />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={8} xl={5}>
              <Card>
                <Statistic title="Quiz đang xuất bản" value={formatCount(stats.quizCount)} />
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  {formatCount(stats.publishedLessonCount)}/{formatCount(stats.lessonCount)} bài đang hiển thị
                </Typography.Text>
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={8} xl={5}>
              <Card>
                <Statistic title="Lượt đã nộp" value={formatCount(stats.submittedAttempts)} />
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  Tính mọi lượt đã nộp trong khóa
                </Typography.Text>
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={12} xl={5}>
              <Card>
                <Statistic title="Học viên hoàn thành" value={formatCount(stats.completedStudents)} />
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  {formatPercent(stats.completionRate)} học viên ghi danh
                </Typography.Text>
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={12} xl={5}>
              <Card>
                <Statistic
                  title="Điểm trung bình"
                  value={stats.averageScore === null ? 'Chưa có' : formatScore(stats.averageScore)}
                  suffix={stats.averageScore === null ? undefined : '/ 100'}
                />
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  Trung bình mọi lượt nộp
                </Typography.Text>
              </Card>
            </Col>
          </Row>

          <CompletionRateCard
            enrolledStudents={stats.enrolledStudents}
            completedStudents={stats.completedStudents}
            quizCount={stats.quizCount}
            completionRate={stats.completionRate}
          />

          <CourseQuizzesTable courseId={stats.courseId} quizzes={stats.quizzes} />
        </>
      ) : null}
    </Space>
  )
}
