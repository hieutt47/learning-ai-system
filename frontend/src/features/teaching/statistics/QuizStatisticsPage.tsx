import { ArrowLeftOutlined, ReloadOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Button, Card, Col, Row, Space, Statistic, Tag, Typography } from 'antd'
import { Link, useParams, useSearchParams } from 'react-router'
import { EmptyState, ErrorState, LoadingState } from '../../../shared/components/PageState'
import { usePageHeader } from '../../../shared/components/pageHeader'
import { paths } from '../../../shared/routing/paths'
import { formatDateTime } from '../../../shared/utils/format'
import { BestScoresTable } from './components/BestScoresTable'
import { HardestQuestionsCard } from './components/HardestQuestionsCard'
import { getQuizStatistics, statisticsKeys } from './statistics.api'
import { formatCount, formatScore, quizStatusMeta } from './statistics.format'

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
          title: data.quizTitle,
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
    <Space orientation="vertical" size={20} style={{ width: '100%' }}>
      <Card>
        <Row gutter={[16, 12]} justify="space-between" align="middle">
          <Col xs={24} md={16}>
            <Space orientation="vertical" size={4}>
              <Space wrap align="center">
                <Link to={backPath}>
                  <Button size="small" icon={<ArrowLeftOutlined />}>
                    Quay lại
                  </Button>
                </Link>
                <Typography.Title level={4} style={{ margin: 0 }}>
                  {data.quizTitle}
                </Typography.Title>
                <Tag color={statusMeta.color}>{statusMeta.label}</Tag>
                {data.lessonStatus === 'ARCHIVED' && <Tag>Bài học đã lưu trữ</Tag>}
              </Space>
              <Typography.Text type="secondary" style={{ fontSize: 13 }}>
                {data.courseTitle ? `${data.courseTitle} · ` : ''}
                {data.lessonTitle ? `Bài học: ${data.lessonTitle} · ` : ''}
                {formatCount(data.questionCount)} câu hỏi
              </Typography.Text>
            </Space>
          </Col>
          <Col xs={24} md={8} style={{ textAlign: 'right' }}>
            <Space wrap>
              <Typography.Text type="secondary" style={{ fontSize: 13 }}>
                Cập nhật lúc {formatDateTime(data.generatedAt)}
              </Typography.Text>
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

      {data.submittedAttempts === 0 ? (
        <Card>
          <EmptyState
            description="Chưa có lượt nộp nào cho quiz này. Khi học viên nộp bài, thống kê điểm và câu bị sai nhiều nhất sẽ hiển thị tại đây."
            action={
              <Link to={backPath}>
                <Button type="primary">Quay lại thống kê khóa học</Button>
              </Link>
            }
          />
        </Card>
      ) : (
        <>
          <Row gutter={[16, 16]}>
            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic title="Lượt đã nộp" value={formatCount(data.submittedAttempts)} />
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  Không tính lượt đang làm dở
                </Typography.Text>
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic title="Học viên đã nộp" value={formatCount(data.uniqueStudents)} />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic
                  title="Điểm trung bình"
                  value={formatScore(data.averageScore)}
                  suffix={data.averageScore === null ? undefined : '/ 100'}
                />
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  Tính trên mọi lượt đã nộp
                </Typography.Text>
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic
                  title="Cao nhất / Thấp nhất"
                  value={
                    data.highestScore === null || data.lowestScore === null
                      ? '—'
                      : `${formatScore(data.highestScore)} / ${formatScore(data.lowestScore)}`
                  }
                />
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  Thang điểm 0–100
                </Typography.Text>
              </Card>
            </Col>
          </Row>

          <HardestQuestionsCard
            questions={data.hardestQuestions}
            submittedAttempts={data.submittedAttempts}
          />

          <BestScoresTable bestScores={data.bestScores} />
        </>
      )}
    </Space>
  )
}
