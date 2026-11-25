import {
  CheckCircleOutlined,
  FileDoneOutlined,
  FormOutlined,
  ReloadOutlined,
  TeamOutlined,
  TrophyOutlined,
} from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Button, Card, Select, Space } from 'antd'
import { Link, useSearchParams } from 'react-router'
import { EmptyState, ErrorState, LoadingState } from '../../../shared/components/PageState'
import { usePageHeader } from '../../../shared/components/pageHeader'
import { paths } from '../../../shared/routing/paths'
import { formatDateTime } from '../../../shared/utils/format'
import { CourseStatusTag } from '../../marketplace/CourseStatusTag'
import { listOwnedCourses } from '../../marketplace/course.api'
import { CompletionRateCard } from './components/CompletionRateCard'
import { CourseQuizzesTable } from './components/CourseQuizzesTable'
import { KpiCard } from './components/KpiCard'
import { getCourseStatistics, statisticsKeys } from './statistics.api'
import { formatCount, formatPercent, formatScore } from './statistics.format'
import './statistics.css'

export function CourseStatisticsPage() {
  usePageHeader({
    title: 'Thống kê khóa học',
    crumbs: [{ label: 'Khóa học giảng dạy', to: paths.teachingCourses }, { label: 'Thống kê khóa học' }],
  })
  const [searchParams, setSearchParams] = useSearchParams()
  const requestedCourseId = searchParams.get('courseId') ?? undefined

  const coursesQuery = useQuery({
    queryKey: statisticsKeys.courseOptions,
    queryFn: () => listOwnedCourses({ page: 1, page_size: 100 }),
  })

  const courses = coursesQuery.data?.items ?? []
  const selectedCourse = courses.find((course) => course.id === requestedCourseId) ?? courses[0]
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
    <div className="stats-page">
      <div className="stats-toolbar">
        <Space wrap size={10} align="center">
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
          {selectedCourse ? <CourseStatusTag status={selectedCourse.status} /> : null}
        </Space>

        <div className="stats-toolbar-right">
          {stats ? (
            <span className="stats-updated">
              Cập nhật lúc {formatDateTime(stats.generatedAt)} (giờ Việt Nam)
            </span>
          ) : null}
          <Button
            icon={<ReloadOutlined />}
            loading={statsQuery.isFetching}
            onClick={() => void statsQuery.refetch()}
          >
            Làm mới
          </Button>
        </div>
      </div>

      {statsQuery.isLoading ? (
        <LoadingState tip="Đang tổng hợp số liệu khóa học…" />
      ) : statsQuery.isError ? (
        <ErrorState error={statsQuery.error} onRetry={() => void statsQuery.refetch()} />
      ) : stats ? (
        <>
          <div className="stats-kpi-grid">
            <KpiCard
              label="Học viên ghi danh"
              value={formatCount(stats.enrolledStudents)}
              icon={<TeamOutlined />}
              tone="primary"
            />
            <KpiCard
              label="Quiz đang xuất bản"
              value={formatCount(stats.quizCount)}
              sub={`${formatCount(stats.publishedLessonCount)}/${formatCount(stats.lessonCount)} bài đang hiển thị`}
              icon={<FormOutlined />}
              tone="info"
            />
            <KpiCard
              label="Lượt nộp bài"
              value={formatCount(stats.submittedAttempts)}
              sub="Tính mọi lượt đã nộp"
              icon={<FileDoneOutlined />}
              tone="warning"
            />
            <KpiCard
              label="Học viên hoàn thành"
              value={formatCount(stats.completedStudents)}
              sub={`${formatPercent(stats.completionRate)} học viên ghi danh`}
              icon={<CheckCircleOutlined />}
              tone="success"
            />
            <KpiCard
              label="Điểm trung bình"
              value={stats.averageScore === null ? '—' : formatScore(stats.averageScore)}
              suffix={stats.averageScore === null ? undefined : '/ 100'}
              sub="Trung bình mọi lượt nộp"
              icon={<TrophyOutlined />}
              tone="primary"
            />
          </div>

          <CompletionRateCard
            enrolledStudents={stats.enrolledStudents}
            completedStudents={stats.completedStudents}
            quizCount={stats.quizCount}
            completionRate={stats.completionRate}
          />

          <CourseQuizzesTable
            courseId={stats.courseId}
            publishedQuizCount={stats.quizCount}
            totalSubmittedAttempts={stats.submittedAttempts}
            quizzes={stats.quizzes}
          />
        </>
      ) : null}
    </div>
  )
}
