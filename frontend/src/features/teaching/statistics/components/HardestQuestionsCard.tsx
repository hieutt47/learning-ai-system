import { Button, Card, Typography } from 'antd'
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
      title={
        <div>
          <span>Câu bị sai nhiều nhất</span>
          <Typography.Paragraph
            type="secondary"
            style={{ margin: 0, fontWeight: 'normal', fontSize: 13, whiteSpace: 'normal' }}
          >
            Sắp xếp theo số lượt làm sai giảm dần trên tổng {formatCount(submittedAttempts)} lượt đã nộp · câu bỏ trống
            được tính là sai.
          </Typography.Paragraph>
        </div>
      }
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
        <div className="stats-bars">
          {visible.map((question) => {
            const widthPct = Math.min(100, Math.max(0, question.wrongRate))
            return (
              <div key={question.questionId} className="stats-bar-row">
                <div className="stats-bar-label">
                  <strong>Câu {question.orderIndex + 1}</strong>
                  {question.questionContent}
                </div>
                <div className="stats-bar-track" aria-hidden="true">
                  <span className="stats-bar-fill tone-danger" style={{ width: `${widthPct}%` }} />
                </div>
                <div className="stats-bar-count">
                  {formatCount(question.wrongCount)}{' '}
                  <span>lượt sai ({formatPercent(question.wrongRate)})</span>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </Card>
  )
}
