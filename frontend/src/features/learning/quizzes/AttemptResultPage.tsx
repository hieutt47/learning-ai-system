import {
  CheckCircleFilled,
  CloseCircleFilled,
  HistoryOutlined,
  RedoOutlined,
} from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import {
  Button,
  Card,
  Col,
  Progress,
  Radio,
  Row,
  Space,
  Tag,
  Typography,
} from 'antd'
import { useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { ErrorState, LoadingState } from '../../../shared/components/PageState'
import { usePageHeader } from '../../../shared/components/pageHeader'
import { paths } from '../../../shared/routing/paths'
import { attemptKeys, getAttemptDetail } from '../attempts/attempts.api'

export function AttemptResultPage() {
  const { attemptId = '' } = useParams()
  const navigate = useNavigate()

  const [filterMode, setFilterMode] = useState<'all' | 'wrong'>('all')

  const attemptQuery = useQuery({
    queryKey: attemptKeys.detail(attemptId),
    queryFn: () => getAttemptDetail(attemptId),
    enabled: Boolean(attemptId),
  })

  const attempt = attemptQuery.data

  usePageHeader(
    attempt
      ? {
          title: `Kết quả: ${attempt.quizTitle}`,
          crumbs: [
            { label: 'Trang chủ', to: paths.home },
            { label: 'Khóa học của tôi', to: paths.myCourses },
            { label: 'Lịch sử làm bài', to: paths.attempts },
            { label: 'Kết quả' },
          ],
        }
      : null,
  )

  if (attemptQuery.isLoading) return <LoadingState tip="Đang tải kết quả bài thi…" />
  if (attemptQuery.isError || !attempt) {
    return (
      <ErrorState
        error={attemptQuery.error}
        onRetry={() => void attemptQuery.refetch()}
      />
    )
  }

  const score = attempt.score ?? 0
  const isPassed = score >= 50
  const isGreat = score >= 80

  const reviews = attempt.reviews ?? []
  const wrongReviews = reviews.filter((r) => !r.isCorrect)
  const displayedReviews = filterMode === 'wrong' ? wrongReviews : reviews

  let scoreColor = '#ff4d4f'
  if (isGreat) scoreColor = '#52c41a'
  else if (isPassed) scoreColor = '#faad14'

  return (
    <Space orientation="vertical" size={24} style={{ width: '100%' }}>
      {/* Thẻ tổng kết điểm số */}
      <Card>
        <Row gutter={[24, 24]} align="middle">
          <Col xs={24} sm={8} style={{ textAlign: 'center' }}>
            <Progress
              type="circle"
              percent={score}
              strokeColor={scoreColor}
              format={() => (
                <div>
                  <div style={{ fontSize: 28, fontWeight: 700, color: scoreColor }}>
                    {score}
                  </div>
                  <div style={{ fontSize: 13, color: 'rgba(0,0,0,0.45)' }}>Điểm</div>
                </div>
              )}
            />
          </Col>
          <Col xs={24} sm={16}>
            <Space orientation="vertical" size={12} style={{ width: '100%' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <Typography.Title level={3} style={{ margin: 0 }}>
                  {attempt.quizTitle}
                </Typography.Title>
                <Tag color={isGreat ? 'success' : isPassed ? 'warning' : 'error'} style={{ fontSize: 14, padding: '2px 10px' }}>
                  {isGreat ? 'Xuất sắc' : isPassed ? 'Đạt' : 'Chưa đạt'}
                </Tag>
              </div>

              <Typography.Text type="secondary" style={{ fontSize: 15 }}>
                Đúng <strong>{attempt.correctCount}</strong> / <strong>{attempt.totalQuestions}</strong> câu hỏi.
                {wrongReviews.length > 0 && ` (Sai ${wrongReviews.length} câu)`}
              </Typography.Text>

              <div style={{ marginTop: 8 }}>
                <Space wrap>
                  <Button
                    type="primary"
                    icon={<RedoOutlined />}
                    onClick={() => navigate(paths.takeQuiz(attempt.quizId))}
                  >
                    Làm lại bài
                  </Button>
                  <Button
                    icon={<HistoryOutlined />}
                    onClick={() => navigate(paths.attempts)}
                  >
                    Xem lịch sử làm bài
                  </Button>
                </Space>
              </div>
            </Space>
          </Col>
        </Row>
      </Card>

      {/* Xem lại chi tiết từng câu hỏi */}
      <Card
        title="Xem lại bài làm"
        extra={
          <Radio.Group
            value={filterMode}
            onChange={(e) => setFilterMode(e.target.value)}
            optionType="button"
            buttonStyle="solid"
          >
            <Radio.Button value="all">Tất cả ({reviews.length})</Radio.Button>
            <Radio.Button value="wrong">Câu sai ({wrongReviews.length})</Radio.Button>
          </Radio.Group>
        }
      >
        {displayedReviews.length === 0 ? (
          <div style={{ padding: '32px 0', textAlign: 'center', color: 'rgba(0,0,0,0.45)' }}>
            Không có câu sai nào trong bài làm!
          </div>
        ) : (
          <Space orientation="vertical" size={20} style={{ width: '100%' }}>
            {displayedReviews.map((review, idx) => (
              <Card
                key={review.questionId || idx}
                type="inner"
                title={
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span>Câu {idx + 1}</span>
                    {review.isCorrect ? (
                      <Tag color="success" icon={<CheckCircleFilled />}>
                        Chính xác
                      </Tag>
                    ) : (
                      <Tag color="error" icon={<CloseCircleFilled />}>
                        {review.selectedOptionId ? 'Chưa đúng' : 'Bỏ trống'}
                      </Tag>
                    )}
                  </div>
                }
              >
                <Typography.Paragraph style={{ fontSize: 15, fontWeight: 500, marginBottom: 16 }}>
                  {review.questionContent}
                </Typography.Paragraph>

                <Space orientation="vertical" size={8} style={{ width: '100%' }}>
                  {review.options.map((opt, oIndex) => {
                    const isSelected = opt.id === review.selectedOptionId
                    const isAnswerCorrect = opt.isCorrect

                    let bg = '#fafafa'
                    let border = '#f0f0f0'
                    let color = 'inherit'
                    let label = ''

                    if (isAnswerCorrect) {
                      bg = '#f6ffed'
                      border = '#b7eb8f'
                      color = '#389e0d'
                      label = ' (Đáp án đúng)'
                    } else if (isSelected && !review.isCorrect) {
                      bg = '#fff2f0'
                      border = '#ffccc7'
                      color = '#cf1322'
                      label = ' (Lựa chọn của bạn)'
                    }

                    return (
                      <div
                        key={opt.id || oIndex}
                        style={{
                          padding: '10px 14px',
                          borderRadius: 6,
                          background: bg,
                          border: `1px solid ${border}`,
                          color,
                          fontSize: 14,
                          display: 'flex',
                          alignItems: 'center',
                          gap: 10,
                        }}
                      >
                        <span style={{ fontWeight: 600 }}>
                          {String.fromCharCode(65 + oIndex)}.
                        </span>
                        <span style={{ flex: 1 }}>{opt.content}</span>
                        {label && <strong style={{ fontSize: 12 }}>{label}</strong>}
                      </div>
                    )
                  })}
                </Space>

                {review.explanation && (
                  <div
                    style={{
                      marginTop: 16,
                      padding: '10px 14px',
                      background: '#f0f5ff',
                      borderRadius: 6,
                      border: '1px solid #d6e4ff',
                      fontSize: 13,
                      color: '#1d39c4',
                    }}
                  >
                    <strong>Giải thích: </strong>
                    {review.explanation}
                  </div>
                )}
              </Card>
            ))}
          </Space>
        )}
      </Card>
    </Space>
  )
}
