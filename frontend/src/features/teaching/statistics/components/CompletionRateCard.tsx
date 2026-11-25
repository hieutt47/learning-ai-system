import { Alert, Card, Space, Typography } from 'antd'
import { formatCount, formatPercent } from '../statistics.format'

interface CompletionRateCardProps {
  enrolledStudents: number
  completedStudents: number
  quizCount: number
  completionRate: number
}

export function CompletionRateCard({
  enrolledStudents,
  completedStudents,
  quizCount,
  completionRate,
}: CompletionRateCardProps) {
  const uncompletedStudents = Math.max(0, enrolledStudents - completedStudents)
  const clampedPercent = Math.min(100, Math.max(0, Number(completionRate) || 0))

  const noticeTitle =
    enrolledStudents === 0
      ? 'Khóa học chưa có học viên ghi danh nên tỷ lệ hoàn thành là 0%.'
      : quizCount === 0
        ? 'Khóa học chưa có quiz đang xuất bản (thuộc bài học đã xuất bản) nên chưa tính được hoàn thành (0%).'
        : null

  return (
    <Card
      title="Tỷ lệ hoàn thành"
      extra={
        enrolledStudents > 0 && quizCount > 0 ? (
          <Typography.Text type="secondary">
            {formatCount(completedStudents)} / {formatCount(enrolledStudents)} học viên
          </Typography.Text>
        ) : null
      }
    >
      <Space orientation="vertical" size={16} style={{ width: '100%' }}>
        {noticeTitle ? <Alert type="info" showIcon title={noticeTitle} /> : null}

        <div className="stats-donut-layout">
          <div
            className="stats-donut"
            role="img"
            aria-label={`Tỷ lệ hoàn thành ${formatPercent(clampedPercent)}`}
            style={{
              background: `conic-gradient(var(--primary) ${clampedPercent * 3.6}deg, var(--neutral-soft) 0)`,
            }}
          >
            <div className="stats-donut-inner">
              <strong>{formatPercent(clampedPercent)}</strong>
              <span>hoàn thành</span>
            </div>
          </div>

          <div className="stats-legend">
            <div className="stats-legend-item">
              <i className="stats-dot stats-dot-primary" />
              <span>Đã hoàn thành:</span>
              <strong>{formatCount(completedStudents)} học viên</strong>
            </div>
            <div className="stats-legend-item">
              <i className="stats-dot stats-dot-neutral" />
              <span>Chưa hoàn thành:</span>
              <strong>{formatCount(uncompletedStudents)} học viên</strong>
            </div>
            <p className="stats-help" style={{ marginTop: 4 }}>
              Học viên được tính là hoàn thành khi đã nộp ít nhất 1 lần{' '}
              <strong>mọi quiz đang xuất bản</strong> thuộc bài học hiển thị của khóa ({formatCount(quizCount)} quiz).
            </p>
          </div>
        </div>
      </Space>
    </Card>
  )
}
