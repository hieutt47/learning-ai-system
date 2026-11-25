import {
  ArrowLeftOutlined,
  BarChartOutlined,
  BookOutlined,
  CheckCircleOutlined,
  FileDoneOutlined,
  FormOutlined,
  ReloadOutlined,
  TeamOutlined,
  TrophyOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Alert, Button, Card, Select, Space, Table, Tag, Typography, type TableProps } from 'antd'
import { type ReactNode, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { apiGet } from '../../../shared/api/client'
import { EmptyState, ErrorState, LoadingState } from '../../../shared/components/PageState'
import { usePageHeader } from '../../../shared/components/pageHeader'
import { paths } from '../../../shared/routing/paths'
import { formatDateTime } from '../../../shared/utils/format'
import { listOwnedCourses } from '../../marketplace/course.api'
import { getCourseLessons } from '../lessons/lesson.api'
import { getLessonQuizzes, getQuiz } from '../quizzes/quiz.api'
import type { QuizStatus } from '../quizzes/quiz.types'
import './statistics.css'

interface CourseStats {
  course_id: string
  enrolled_students: number
  quiz_count: number
  submitted_attempts: number
  completed_students: number
  completion_rate: number
  generated_at: string
}

interface QuizStats {
  quiz_id: string
  submitted_attempts: number
  unique_students: number
  average_score: number | null
  generated_at: string
  best_scores: { student_id: string; score: number }[]
  hardest_questions: { question_id: string; wrong_count: number }[]
}

interface SystemStats {
  active_users: number
  teachers: number
  published_courses: number
  enrollments: number
  submitted_attempts: number
  generated_at: string
}

interface CourseQuizStatRow {
  id: string
  title: string
  lessonTitle: string
  status: QuizStatus
  questionCount: number
  submittedAttempts: number
  averageScore: number | null
}

function formatNumber(n: number): string {
  return new Intl.NumberFormat('vi-VN').format(n)
}

function formatPercent(value: number): string {
  return `${Number(value).toFixed(2).replace('.', ',')}%`
}

function scoreTag(score: number | null) {
  if (score == null) {
    return <Typography.Text type="secondary">—</Typography.Text>
  }
  const color = score >= 80 ? 'success' : score >= 50 ? 'warning' : 'error'
  return (
    <Tag color={color} style={{ fontWeight: 600, margin: 0 }}>
      {Number(score).toFixed(2).replace(/\.00$/, '')} / 100
    </Tag>
  )
}

function quizStatusTag(status: QuizStatus) {
  switch (status) {
    case 'PUBLISHED':
      return <Tag color="success">Đã xuất bản</Tag>
    case 'CLOSED':
      return <Tag color="default">Đã đóng</Tag>
    case 'DRAFT':
      return <Tag color="warning">Bản nháp</Tag>
  }
}

function KpiCard({
  label,
  value,
  suffix,
  sub,
  icon,
  tone = 'primary',
}: {
  label: string
  value: string | number
  suffix?: string
  sub?: string
  icon: ReactNode
  tone?: 'primary' | 'success' | 'warning' | 'info'
}) {
  return (
    <div className="stats-kpi-card">
      <div className="stats-kpi-head">
        <span className="stats-kpi-label">{label}</span>
        <span className={`stats-kpi-icon tone-${tone}`} aria-hidden="true">
          {icon}
        </span>
      </div>
      <div className="stats-kpi-value">
        {value}
        {suffix ? <small> {suffix}</small> : null}
      </div>
      {sub ? <span className="stats-kpi-sub">{sub}</span> : null}
    </div>
  )
}

async function fetchCourseQuizRows(courseId: string): Promise<CourseQuizStatRow[]> {
  const lessons = await getCourseLessons(courseId)
  const visibleLessons = lessons.filter((lesson) => lesson.status === 'PUBLISHED')
  const rows: CourseQuizStatRow[] = []

  const quizPages = await Promise.all(
    visibleLessons.map(async (lesson) => ({
      lesson,
      page: await getLessonQuizzes(lesson.id, 1, 100),
    })),
  )

  const publishedQuizzes = quizPages.flatMap(({ lesson, page }) =>
    page.items
      .filter((quiz) => quiz.status === 'PUBLISHED' || quiz.status === 'CLOSED')
      .map((quiz) => ({ lesson, quiz })),
  )

  const statsList = await Promise.all(
    publishedQuizzes.map(async ({ lesson, quiz }) => {
      try {
        const s = await apiGet<QuizStats>(`/statistics/quizzes/${quiz.id}`)
        return {
          id: quiz.id,
          title: quiz.title,
          lessonTitle: lesson.title,
          status: quiz.status,
          questionCount: quiz.questionCount,
          submittedAttempts: s.submitted_attempts,
          averageScore: s.average_score,
        } satisfies CourseQuizStatRow
      } catch {
        return {
          id: quiz.id,
          title: quiz.title,
          lessonTitle: lesson.title,
          status: quiz.status,
          questionCount: quiz.questionCount,
          submittedAttempts: 0,
          averageScore: null,
        } satisfies CourseQuizStatRow
      }
    }),
  )

  rows.push(...statsList)
  return rows
}

export function CourseStatisticsPage() {
  const navigate = useNavigate()
  usePageHeader({
    title: 'Thống kê khóa học',
    crumbs: [{ label: 'Khóa học giảng dạy', to: paths.teachingCourses }, { label: 'Thống kê khóa học' }],
  })

  const [courseId, setCourseId] = useState<string>()
  const courses = useQuery({
    queryKey: ['statistics', 'courses', 'options'],
    queryFn: () => listOwnedCourses({ page: 1, page_size: 100 }),
  })
  const selected = courseId ?? courses.data?.items[0]?.id

  const stats = useQuery({
    queryKey: ['statistics', 'course', selected],
    enabled: Boolean(selected),
    queryFn: () => apiGet<CourseStats>(`/statistics/courses/${selected}`),
  })

  const courseQuizzes = useQuery({
    queryKey: ['statistics', 'course', selected, 'quizzes'],
    enabled: Boolean(selected),
    queryFn: () => fetchCourseQuizRows(selected!),
  })

  if (courses.isLoading) return <LoadingState tip="Đang tải danh sách khóa học…" />
  if (courses.isError) return <ErrorState error={courses.error} onRetry={() => void courses.refetch()} />

  if (!selected) {
    return (
      <Card>
        <EmptyState
          description="Bạn chưa có khóa học nào để thống kê."
          action={
            <Button type="primary" onClick={() => navigate(paths.teachingCourses)}>
              Đến khóa học giảng dạy
            </Button>
          }
        />
      </Card>
    )
  }

  const data = stats.data
  const completionPct = data ? Math.max(0, Math.min(100, Number(data.completion_rate) || 0)) : 0
  const incompleteStudents = data ? Math.max(0, data.enrolled_students - data.completed_students) : 0

  const quizColumns: TableProps<CourseQuizStatRow>['columns'] = [
    {
      title: 'Tên quiz',
      dataIndex: 'title',
      width: 280,
      render: (title: string, row) => (
        <Button
          type="link"
          style={{
            padding: 0,
            height: 'auto',
            textAlign: 'left',
            whiteSpace: 'normal',
            wordBreak: 'break-word',
            maxWidth: 260,
          }}
          onClick={() => navigate(paths.quizStatistics(row.id))}
        >
          {title}
        </Button>
      ),
    },
    {
      title: 'Bài học',
      dataIndex: 'lessonTitle',
      width: 170,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      width: 130,
      align: 'center',
      render: (status: QuizStatus) => quizStatusTag(status),
    },
    {
      title: 'Số câu',
      dataIndex: 'questionCount',
      width: 90,
      align: 'center',
    },
    {
      title: 'Lượt nộp',
      dataIndex: 'submittedAttempts',
      width: 110,
      align: 'center',
      render: (n: number) => <strong>{formatNumber(n)}</strong>,
    },
    {
      title: 'Điểm trung bình',
      dataIndex: 'averageScore',
      width: 150,
      align: 'center',
      render: (score: number | null) => scoreTag(score),
    },
    {
      title: 'Hành động',
      key: 'action',
      width: 130,
      align: 'right',
      render: (_, row) => (
        <Button
          type="link"
          size="small"
          icon={<BarChartOutlined />}
          onClick={() => navigate(paths.quizStatistics(row.id))}
        >
          Xem chi tiết
        </Button>
      ),
    },
  ]

  return (
    <div className="stats-page">
      <div className="stats-toolbar">
        <span className="stats-updated">
          {data
            ? `Cập nhật lúc ${formatDateTime(data.generated_at)} (giờ Việt Nam)`
            : 'Đang tải số liệu thống kê…'}
        </span>
        <div className="stats-toolbar-right">
          <Typography.Text strong>Khóa học:</Typography.Text>
          <Select
            aria-label="Chọn khóa học"
            placeholder="Chọn khóa học"
            value={selected}
            onChange={setCourseId}
            style={{ width: 280, maxWidth: '100%' }}
            options={courses.data?.items.map((c) => ({ label: c.title, value: c.id }))}
          />
          <Button
            icon={<ReloadOutlined />}
            loading={stats.isFetching || courseQuizzes.isFetching}
            onClick={() => {
              void stats.refetch()
              void courseQuizzes.refetch()
            }}
          >
            Làm mới
          </Button>
        </div>
      </div>

      {stats.isLoading ? (
        <LoadingState tip="Đang tổng hợp số liệu khóa học…" />
      ) : stats.isError ? (
        <ErrorState error={stats.error} onRetry={() => void stats.refetch()} />
      ) : data ? (
        <>
          <div className="stats-kpi-grid">
            <KpiCard
              label="Học viên đã ghi danh"
              value={formatNumber(data.enrolled_students)}
              icon={<TeamOutlined />}
              tone="primary"
            />
            <KpiCard
              label="Quiz đang xuất bản"
              value={formatNumber(data.quiz_count)}
              icon={<FormOutlined />}
              tone="info"
            />
            <KpiCard
              label="Lượt nộp bài"
              value={formatNumber(data.submitted_attempts)}
              icon={<FileDoneOutlined />}
              tone="warning"
            />
            <KpiCard
              label="Học viên hoàn thành"
              value={formatNumber(data.completed_students)}
              icon={<CheckCircleOutlined />}
              tone="success"
            />
          </div>

          <Card
            title="Tỷ lệ hoàn thành"
            extra={
              data.enrolled_students > 0 && data.quiz_count > 0 ? (
                <Typography.Text type="secondary">
                  {formatNumber(data.completed_students)} / {formatNumber(data.enrolled_students)} học viên
                </Typography.Text>
              ) : null
            }
          >
            <Space orientation="vertical" size={16} style={{ width: '100%' }}>
              {data.enrolled_students === 0 ? (
                <Alert
                  type="info"
                  showIcon
                  message="Khóa học chưa có học viên ghi danh nên tỷ lệ hoàn thành là 0%."
                />
              ) : data.quiz_count === 0 ? (
                <Alert
                  type="info"
                  showIcon
                  message="Khóa học chưa có quiz nào được xuất bản nên chưa tính được hoàn thành (0%)."
                />
              ) : null}

              <div className="stats-donut-layout">
                <div
                  className="stats-donut"
                  role="img"
                  aria-label={`Tỷ lệ hoàn thành ${formatPercent(completionPct)}`}
                  style={{
                    background: `conic-gradient(var(--primary) ${completionPct * 3.6}deg, var(--neutral-soft) 0)`,
                  }}
                >
                  <div className="stats-donut-inner">
                    <strong>{formatPercent(completionPct)}</strong>
                    <span>hoàn thành</span>
                  </div>
                </div>

                <div className="stats-legend">
                  <div className="stats-legend-item">
                    <i className="stats-dot stats-dot-primary" />
                    <span>Đã hoàn thành:</span>
                    <strong>{formatNumber(data.completed_students)} học viên</strong>
                  </div>
                  <div className="stats-legend-item">
                    <i className="stats-dot stats-dot-neutral" />
                    <span>Chưa hoàn thành:</span>
                    <strong>{formatNumber(incompleteStudents)} học viên</strong>
                  </div>
                  <p className="stats-help" style={{ marginTop: 4 }}>
                    Học viên được tính là hoàn thành khi đã nộp ít nhất 1 lần{' '}
                    <strong>mọi quiz đang xuất bản</strong> của khóa ({formatNumber(data.quiz_count)} quiz).
                  </p>
                </div>
              </div>
            </Space>
          </Card>

          <Card
            className="page-table-card"
            title={
              <div>
                <span>Quiz của khóa</span>
                <Typography.Paragraph
                  type="secondary"
                  style={{ margin: 0, fontWeight: 'normal', fontSize: 13, whiteSpace: 'normal' }}
                >
                  {formatNumber(data.quiz_count)} quiz đang xuất bản · {formatNumber(data.submitted_attempts)} lượt nộp · Điểm trung bình tính trên các lượt đã nộp (thang 100).
                </Typography.Paragraph>
              </div>
            }
          >
            {courseQuizzes.isLoading ? (
              <LoadingState tip="Đang tải thống kê từng quiz…" />
            ) : courseQuizzes.isError ? (
              <ErrorState error={courseQuizzes.error} onRetry={() => void courseQuizzes.refetch()} />
            ) : (courseQuizzes.data?.length ?? 0) === 0 ? (
              <EmptyState description="Khóa học chưa có quiz nào được xuất bản. Xuất bản quiz trong bài học để xem thống kê chi tiết tại đây." />
            ) : (
              <Table<CourseQuizStatRow>
                rowKey="id"
                columns={quizColumns}
                dataSource={courseQuizzes.data}
                pagination={false}
                scroll={{ x: 860 }}
              />
            )}
          </Card>
        </>
      ) : null}
    </div>
  )
}

export function QuizStatisticsPage() {
  const { quizId = '' } = useParams()
  const navigate = useNavigate()

  const stats = useQuery({
    queryKey: ['statistics', 'quiz', quizId],
    queryFn: () => apiGet<QuizStats>(`/statistics/quizzes/${quizId}`),
    enabled: Boolean(quizId),
  })

  const quizDetail = useQuery({
    queryKey: ['statistics', 'quiz', quizId, 'detail'],
    queryFn: () => getQuiz(quizId),
    enabled: Boolean(quizId),
    retry: false,
  })

  const quizTitle = quizDetail.data?.title ?? `Quiz #${quizId}`

  usePageHeader({
    title: `Thống kê quiz: ${quizTitle}`,
    crumbs: [
      { label: 'Thống kê khóa học', to: paths.teachingStatistics },
      { label: quizTitle },
    ],
  })

  if (stats.isLoading) return <LoadingState tip="Đang tải thống kê quiz…" />
  if (stats.isError) return <ErrorState error={stats.error} onRetry={() => void stats.refetch()} />
  if (!stats.data) return null

  const data = stats.data
  const questionMap = new Map(
    (quizDetail.data?.questions ?? []).map((q, idx) => [
      q.id ?? String(idx),
      { number: idx + 1, content: q.content },
    ]),
  )

  const maxWrong = Math.max(1, ...data.hardest_questions.map((item) => item.wrong_count))

  const bestScoreColumns: TableProps<QuizStats['best_scores'][number]>['columns'] = [
    {
      title: '#',
      key: 'rank',
      width: 64,
      align: 'center',
      render: (_, __, idx) => idx + 1,
    },
    {
      title: 'Học viên',
      dataIndex: 'student_id',
      render: (studentId: string) => (
        <Space size={8}>
          <UserOutlined style={{ color: 'var(--text-2)' }} />
          <strong>Sinh viên #{studentId}</strong>
        </Space>
      ),
    },
    {
      title: 'Điểm cao nhất',
      dataIndex: 'score',
      width: 240,
      render: (score: number) => {
        const pct = Math.max(0, Math.min(100, Number(score) || 0))
        const barColor = pct >= 80 ? 'var(--success)' : pct >= 50 ? 'var(--warning)' : 'var(--danger)'
        return (
          <div className="stats-score-cell">
            {scoreTag(score)}
            <span className="stats-score-mini" aria-hidden="true">
              <i style={{ width: `${pct}%`, background: barColor }} />
            </span>
          </div>
        )
      },
    },
  ]

  return (
    <div className="stats-page">
      <div className="stats-toolbar">
        <Space wrap size={10}>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(paths.teachingStatistics)}>
            Quay lại thống kê
          </Button>
          {quizDetail.data ? quizStatusTag(quizDetail.data.status) : null}
          {quizDetail.data ? (
            <span className="stats-updated">
              {quizDetail.data.questions.length} câu hỏi · Cập nhật lúc {formatDateTime(data.generated_at)} (giờ Việt Nam)
            </span>
          ) : (
            <span className="stats-updated">
              Cập nhật lúc {formatDateTime(data.generated_at)} (giờ Việt Nam)
            </span>
          )}
        </Space>
        <Button
          icon={<ReloadOutlined />}
          loading={stats.isFetching}
          onClick={() => void stats.refetch()}
        >
          Làm mới
        </Button>
      </div>

      <div className="stats-kpi-grid">
        <KpiCard
          label="Lượt đã nộp"
          value={formatNumber(data.submitted_attempts)}
          icon={<FileDoneOutlined />}
          tone="primary"
        />
        <KpiCard
          label="Học viên đã làm"
          value={formatNumber(data.unique_students)}
          icon={<TeamOutlined />}
          tone="info"
        />
        <KpiCard
          label="Điểm trung bình"
          value={data.average_score != null ? Number(data.average_score).toFixed(2).replace(/\.00$/, '') : '—'}
          suffix={data.average_score != null ? '/ 100' : undefined}
          sub="Tính trên mọi lượt đã nộp"
          icon={<TrophyOutlined />}
          tone="success"
        />
      </div>

      {data.submitted_attempts === 0 ? (
        <Card>
          <EmptyState
            description="Chưa có lượt nộp nào cho bài quiz này. Khi sinh viên nộp bài, thống kê chi tiết sẽ xuất hiện tại đây."
          />
        </Card>
      ) : (
        <>
          <Card
            title={
              <div>
                <span>Câu bị sai nhiều nhất</span>
                <Typography.Paragraph
                  type="secondary"
                  style={{ margin: 0, fontWeight: 'normal', fontSize: 13, whiteSpace: 'normal' }}
                >
                  Sắp xếp theo số lượt làm sai giảm dần · câu bỏ trống được tính là sai.
                </Typography.Paragraph>
              </div>
            }
          >
            {data.hardest_questions.length === 0 ? (
              <EmptyState description="Tuyệt vời! Chưa có câu hỏi nào bị làm sai." />
            ) : (
              <div className="stats-bars">
                {data.hardest_questions.map((item) => {
                  const meta = questionMap.get(item.question_id)
                  const widthPct = Math.max(4, Math.round((item.wrong_count / maxWrong) * 100))
                  return (
                    <div key={item.question_id} className="stats-bar-row">
                      <div className="stats-bar-label">
                        <strong>{meta ? `Câu ${meta.number}` : `Câu #${item.question_id}`}</strong>
                        {meta ? <span>{meta.content}</span> : null}
                      </div>
                      <div className="stats-bar-track" aria-hidden="true">
                        <i className="stats-bar-fill tone-danger" style={{ width: `${widthPct}%` }} />
                      </div>
                      <div className="stats-bar-count">
                        {formatNumber(item.wrong_count)} <span>lượt sai</span>
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </Card>

          <Card
            className="page-table-card"
            title={
              <div>
                <span>Điểm cao nhất theo học viên</span>
                <Typography.Paragraph
                  type="secondary"
                  style={{ margin: 0, fontWeight: 'normal', fontSize: 13, whiteSpace: 'normal' }}
                >
                  Lượt có điểm cao nhất của mỗi học viên ({formatNumber(data.best_scores.length)} học viên).
                </Typography.Paragraph>
              </div>
            }
          >
            <Table
              rowKey="student_id"
              columns={bestScoreColumns}
              dataSource={data.best_scores}
              scroll={{ x: 'max-content' }}
              pagination={{
                pageSize: 10,
                hideOnSinglePage: true,
                showTotal: (total) => `Tổng ${formatNumber(total)} học viên`,
              }}
            />
          </Card>
        </>
      )}
    </div>
  )
}

export function SystemStatisticsPage() {
  usePageHeader({ title: 'Thống kê hệ thống' })
  const stats = useQuery({
    queryKey: ['statistics', 'system'],
    queryFn: () => apiGet<SystemStats>('/statistics/system'),
  })

  if (stats.isLoading) return <LoadingState tip="Đang tải thống kê hệ thống…" />
  if (stats.isError) return <ErrorState error={stats.error} onRetry={() => void stats.refetch()} />
  if (!stats.data) return null

  const d = stats.data
  const totalUsers = d.active_users
  const teacherCount = Math.min(d.teachers, totalUsers)
  const otherUsers = Math.max(0, totalUsers - teacherCount)
  const teacherPct = totalUsers > 0 ? (teacherCount / totalUsers) * 100 : 0
  const otherPct = totalUsers > 0 ? (otherUsers / totalUsers) * 100 : 0

  return (
    <div className="stats-page">
      <div className="stats-toolbar">
        <span className="stats-updated">
          Cập nhật lúc {formatDateTime(d.generated_at)} (giờ Việt Nam)
        </span>
        <Button
          icon={<ReloadOutlined />}
          loading={stats.isFetching}
          onClick={() => void stats.refetch()}
        >
          Làm mới
        </Button>
      </div>

      <div className="stats-kpi-grid">
        <KpiCard
          label="Tài khoản hoạt động"
          value={formatNumber(d.active_users)}
          icon={<TeamOutlined />}
          tone="primary"
        />
        <KpiCard
          label="Giảng viên"
          value={formatNumber(d.teachers)}
          icon={<UserOutlined />}
          tone="info"
        />
        <KpiCard
          label="Khóa học đang xuất bản"
          value={formatNumber(d.published_courses)}
          icon={<BookOutlined />}
          tone="success"
        />
        <KpiCard
          label="Lượt ghi danh"
          value={formatNumber(d.enrollments)}
          icon={<CheckCircleOutlined />}
          tone="warning"
        />
        <KpiCard
          label="Lượt nộp bài"
          value={formatNumber(d.submitted_attempts)}
          icon={<FileDoneOutlined />}
          tone="primary"
        />
      </div>

      <Card
        title={
          <div>
            <span>Cơ cấu người dùng hoạt động</span>
            <Typography.Paragraph
              type="secondary"
              style={{ margin: 0, fontWeight: 'normal', fontSize: 13, whiteSpace: 'normal' }}
            >
              Tỷ lệ giảng viên trên tổng số tài khoản đang hoạt động.
            </Typography.Paragraph>
          </div>
        }
      >
        <div className="stats-donut-layout">
          <div
            className="stats-donut"
            role="img"
            aria-label={`Giảng viên chiếm ${teacherPct.toFixed(1)}%`}
            style={{
              background:
                totalUsers > 0
                  ? `conic-gradient(var(--primary) ${teacherPct * 3.6}deg, var(--info) 0)`
                  : 'var(--neutral-soft)',
            }}
          >
            <div className="stats-donut-inner">
              <strong>{formatNumber(totalUsers)}</strong>
              <span>đang hoạt động</span>
            </div>
          </div>

          <div className="stats-legend">
            <div className="stats-legend-item">
              <i className="stats-dot stats-dot-info" />
              <span>Sinh viên &amp; quản trị khác:</span>
              <strong>
                {formatNumber(otherUsers)} · {otherPct.toFixed(1).replace('.', ',')}%
              </strong>
            </div>
            <div className="stats-legend-item">
              <i className="stats-dot stats-dot-primary" />
              <span>Giảng viên:</span>
              <strong>
                {formatNumber(d.teachers)} · {teacherPct.toFixed(1).replace('.', ',')}%
              </strong>
            </div>
          </div>
        </div>
      </Card>

      <Card
        title={
          <div>
            <span>Cách tính số liệu</span>
            <Typography.Paragraph
              type="secondary"
              style={{ margin: 0, fontWeight: 'normal', fontSize: 13, whiteSpace: 'normal' }}
            >
              Số liệu tổng hợp tức thời tại thời điểm cập nhật.
            </Typography.Paragraph>
          </div>
        }
      >
        <dl className="stats-dl">
          <dt>Tài khoản hoạt động</dt>
          <dd>Tổng số tài khoản đang ở trạng thái hoạt động (không bị khóa) trên hệ thống.</dd>
          <dt>Giảng viên</dt>
          <dd>Tổng số tài khoản có vai trò Giảng viên trong hệ thống.</dd>
          <dt>Khóa học đang xuất bản</dt>
          <dd>Khóa học đang xuất bản và chưa lưu trữ (không tính bản nháp hoặc đã lưu trữ).</dd>
          <dt>Lượt ghi danh</dt>
          <dd>Tổng số lượt sinh viên đã ghi danh vào các khóa học.</dd>
          <dt>Lượt nộp bài</dt>
          <dd>Tổng số lượt nộp quiz đã hoàn tất (trạng thái SUBMITTED) trên toàn hệ thống.</dd>
        </dl>
      </Card>
    </div>
  )
}
