import { Button, Card, Progress, Space, Typography } from 'antd'
import { useState } from 'react'
import { EmptyState } from '../../../../shared/components/PageState'
import { formatCount, formatPercent } from '../statistics.format'
import type { HardestQuestion } from '../statistics.types'

interface HardestQuestionsCardProps {
  questions: readonly HardestQuestion[]
  submittedAttempts: number
}

const DEFAULT_VISIBLE_COUNT = 5

export function HardestQuestionsCard({ questions, submittedAttempts }: HardestQuestionsCardProps) {
  const [showAll, setShowAll] = useState(false)
  const visible = showAll ? questions : questions.slice(0, DEFAULT_VISIBLE_COUNT)

  return (
    <Card
      title="Câu bị sai nhiều nhất"
      extra={
        questions.length > DEFAULT_VISIBLE_COUNT ? (
          <Button type="link" size="small" onClick={() => setShowAll((prev) => !prev)}>
            {showAll ? 'Thu gọn Top 5' : `Xem tất cả (${questions.length})`}
          </Button>
        ) : null
      }
    >
      {questions.length === 0 ? (
        <EmptyState description="Chưa ghi nhận câu trả lời sai nào trong các lượt đã nộp." />
      ) : (
        <Space orientation="vertical" size={16} style={{ width: '100%' }}>
          <Typography.Text type="secondary" style={{ fontSize: 13 }}>
            Xếp hạng theo số lượt trả lời sai (gồm cả câu bỏ trống) trên tổng {formatCount(submittedAttempts)} lượt đã
            nộp.
          </Typography.Text>

          {visible.map((question) => (
            <div key={question.questionId} style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', gap: 12 }}>
                <Typography.Text>
                  <strong>Câu {question.orderIndex + 1}:</strong> {question.questionContent}
                </Typography.Text>
                <Typography.Text strong style={{ whiteSpace: 'nowrap', flexShrink: 0 }}>
                  {formatCount(question.wrongCount)} lượt sai ({formatPercent(question.wrongRate)})
                </Typography.Text>
              </div>
              <Progress
                percent={Math.min(100, Math.max(0, question.wrongRate))}
                showInfo={false}
                strokeColor="var(--danger, #DC2626)"
                size="small"
              />
            </div>
          ))}
        </Space>
      )}
    </Card>
  )
}
