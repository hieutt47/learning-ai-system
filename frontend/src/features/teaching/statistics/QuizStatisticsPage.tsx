import {
  ArrowLeftOutlined,
  BarChartOutlined,
  FileDoneOutlined,
  ReloadOutlined,
  TeamOutlined,
  TrophyOutlined,
} from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Button, Card, Space, Tag } from 'antd'
import { Link, useParams, useSearchParams } from 'react-router'
import { EmptyState, ErrorState, LoadingState } from '../../../shared/components/PageState'
import { usePageHeader } from '../../../shared/components/pageHeader'
import { paths } from '../../../shared/routing/paths'
import { formatDateTime } from '../../../shared/utils/format'
import { BestScoresTable } from './components/BestScoresTable'
import { HardestQuestionsCard } from './components/HardestQuestionsCard'
import { KpiCard } from './components/KpiCard'
import { getQuizStatistics, statisticsKeys } from './statistics.api'
import { formatCount, formatScore, quizStatusMeta } from './statistics.format'
import './statistics.css'

export function QuizStatisticsPage() {
  const { quizId = '' } = useParams()
  const [searchParams] = useSearchParams()
  const statsQuery = useQuery({
    queryKey: statisticsKeys.quiz(quizId),
    queryFn: () => getQuizStatistics(quizId),
    enabled: Boolean(quizId),
  })

  const data = statsQuery.data
  const backCourseId = data?.courseId || searchParams.get('courseId') || ''
  const backPath = backCourseId
    ? `${paths.teachingStatistics}?courseId=${encodeURIComponent(backCourseId)}`
    : paths.teachingStatistics

  usePageHeader(
    data
      ? {
          title: `Thống kê quiz: ${data.quizTitle}`,
          crumbs: [
            { label: 'Thống kê khóa học', to: backPath },
            ...(data.courseTitle ? [{ label: data.courseTitle, to: backPath }] : []),
            { label: data.quizTitle },
          ],
        }
      : {
          title: 'Thống kê quiz',
          crumbs: [{ label: 'Thống kê khóa học', to: backPath }, { label: 'Thống kê quiz' }],
        },
  )

  if (statsQuery.isLoading) {
    return <LoadingState tip="Đang tải thống kê quiz…" />
  }
  if (statsQuery.isError) {
    return <ErrorState error={statsQuery.error} onRetry={() => void statsQuery.refetch()} />
  }
  if (!data) {
    return null
  }

  const statusMeta = quizStatusMeta(data.quizStatus)

  return (
    <div className="stats-page">
      <div className="stats-toolbar">
        <Space wrap size={10}>
          <Link to={backPath}>
            <Button icon={<ArrowLeftOutlined />}>Quay lại thống kê</Button>
          </Link>
          <Tag color={statusMeta.color}>{statusMeta.label}</Tag>
          {data.lessonStatus === 'ARCHIVED' ? <Tag>Bài học đã lưu trữ</Tag> : null}
          <span className="stats-updated">
            {data.lessonTitle ? `Bài học: ${data.lessonTitle} · ` : ''}
            {formatCount(data.questionCount)} câu hỏi · Cập nhật lúc {formatDateTime(data.generatedAt)} (giờ Việt Nam)
          </span>
        </Space>

        <Button
          icon={<ReloadOutlined />}
          loading={statsQuery.isFetching}
          onClick={() => void statsQuery.refetch()}
        >
          Làm mới
        </Button>
      </div>

      <div className="stats-kpi-grid">
        <KpiCard
          label="Lượt đã nộp"
          value={formatCount(data.submittedAttempts)}
          sub="Không tính lượt đang làm dở"
          icon={<FileDoneOutlined />}
          tone="primary"
        />
        <KpiCard
          label="Học viên đã làm"
          value={formatCount(data.uniqueStudents)}
          icon={<TeamOutlined />}
          tone="info"
        />
        <KpiCard
          label="Điểm trung bình"
          value={data.averageScore === null ? '—' : formatScore(data.averageScore)}
          suffix={data.averageScore === null ? undefined : '/ 100'}
          sub="Tính trên mọi lượt đã nộp"
          icon={<TrophyOutlined />}
          tone="success"
        />
        <KpiCard
          label="Cao nhất / Thấp nhất"
          value={
            data.highestScore === null || data.lowestScore === null
              ? '—'
              : `${formatScore(data.highestScore)} / ${formatScore(data.lowestScore)}`
          }
          sub="Thang điểm 0–100"
          icon={<BarChartOutlined />}
          tone="warning"
        />
      </div>

      {data.submittedAttempts === 0 ? (
        <Card>
          <EmptyState
            description="Chưa có lượt nộp nào cho bài quiz này. Khi sinh viên nộp bài, thống kê chi tiết sẽ xuất hiện tại đây."
            action={
              <Link to={backPath}>
                <Button type="primary">Quay lại thống kê khóa học</Button>
              </Link>
            }
          />
        </Card>
      ) : (
        <>
          <HardestQuestionsCard
            questions={data.hardestQuestions}
            submittedAttempts={data.submittedAttempts}
          />
          <BestScoresTable bestScores={data.bestScores} />
        </>
      )}
    </div>
  )
}
