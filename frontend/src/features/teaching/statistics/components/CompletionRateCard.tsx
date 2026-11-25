import { Alert, Card, Col, Progress, Row, Space, Typography } from 'antd'
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
  const clampedPercent = Math.min(100, Math.max(0, completionRate))

  const noticeMessage =
    enrolledStudents === 0
      ? 'Khóa học chưa có học viên ghi danh nên tỷ lệ hoàn thành là 0%.'
      : quizCount === 0
        ? 'Khóa học chưa có quiz đang xuất bản (thuộc bài học đã xuất bản) nên tỷ lệ hoàn thành là 0%.'
        : null

  return (
    <Card title="Tỷ lệ hoàn thành khóa học">
      <Space orientation="vertical" size={16} style={{ width: '100%' }}>
        {noticeMessage ? (
          <Alert type="info" showIcon title={noticeMessage} />
        ) : (
          <Typography.Text type="secondary">
            {formatCount(completedStudents)} trên {formatCount(enrolledStudents)} học viên đã ghi danh hoàn thành khóa học.
          </Typography.Text>
        )}

        <Row gutter={[24, 16]} align="middle">
          <Col flex="none">
            <Progress
              type="circle"
              size={148}
              strokeWidth={10}
              percent={clampedPercent}
              strokeColor="var(--primary, #4F46E5)"
              format={() => (
                <div style={{ lineHeight: 1.25 }}>
                  <div style={{ fontSize: 22, fontWeight: 700 }}>{formatPercent(completionRate)}</div>
                  <div style={{ fontSize: 12, color: 'var(--text-3)' }}>hoàn thành</div>
                </div>
              )}
            />
          </Col>

          <Col flex="auto">
            <Space orientation="vertical" size={10} style={{ width: '100%' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <span
                  style={{
                    width: 10,
                    height: 10,
                    borderRadius: '50%',
                    background: 'var(--primary, #4F46E5)',
                    display: 'inline-block',
                    flexShrink: 0,
                  }}
                />
                <Typography.Text>
                  Đã hoàn thành: <strong>{formatCount(completedStudents)} học viên</strong>
                </Typography.Text>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <span
                  style={{
                    width: 10,
                    height: 10,
                    borderRadius: '50%',
                    background: 'var(--border-strong, #94A3B8)',
                    display: 'inline-block',
                    flexShrink: 0,
                  }}
                />
                <Typography.Text>
                  Chưa hoàn thành: <strong>{formatCount(uncompletedStudents)} học viên</strong>
                </Typography.Text>
              </div>
              <Typography.Paragraph type="secondary" style={{ margin: '4px 0 0', fontSize: 13 }}>
                Học viên được tính là hoàn thành khi đã nộp ít nhất 1 lần <strong>mọi quiz đang xuất bản</strong> thuộc
                các bài học đang hiển thị của khóa ({formatCount(quizCount)} quiz). Thay đổi danh sách quiz xuất bản có
                thể làm thay đổi tỷ lệ này.
              </Typography.Paragraph>
            </Space>
          </Col>
        </Row>
      </Space>
    </Card>
  )
}
