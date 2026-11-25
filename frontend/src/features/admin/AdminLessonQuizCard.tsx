import { CheckCircleFilled, EyeOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Alert, Button, Card, Drawer, Space, Table, Tag, Typography, type TableProps } from 'antd'
import { useState } from 'react'
import { EmptyState, ErrorState, LoadingState } from '../../shared/components/PageState'
import { formatDateTime } from '../marketplace/format'
import { getLessonQuizzes, getQuiz, quizKeys } from '../teaching/quizzes/quiz.api'
import type { QuizStatus, QuizSummaryItem } from '../teaching/quizzes/quiz.types'
import { optionLabel } from '../teaching/quizzes/quizValidation'

function statusTag(status: QuizStatus) {
  switch (status) {
    case 'DRAFT':
      return <Tag color="warning">Bản nháp</Tag>
    case 'PUBLISHED':
      return <Tag color="success">Đã xuất bản</Tag>
    case 'CLOSED':
      return <Tag color="default">Đã đóng</Tag>
  }
}

interface AdminLessonQuizCardProps {
  lessonId: string
}

/** Danh sách và chi tiết quiz chỉ đọc của một bài học dành cho ADMIN. */
export function AdminLessonQuizCard({ lessonId }: AdminLessonQuizCardProps) {
  const [selectedQuizId, setSelectedQuizId] = useState<string | null>(null)

  const quizzesQuery = useQuery({
    queryKey: quizKeys.lesson(lessonId),
    queryFn: () => getLessonQuizzes(lessonId),
    enabled: Boolean(lessonId),
  })

  const quizDetailQuery = useQuery({
    queryKey: quizKeys.detail(selectedQuizId ?? ''),
    queryFn: () => getQuiz(selectedQuizId!),
    enabled: Boolean(selectedQuizId),
  })

  const quizzes = quizzesQuery.data?.items ?? []

  const columns: TableProps<QuizSummaryItem>['columns'] = [
    {
      title: '#',
      key: 'index',
      width: 60,
      align: 'center',
      render: (_, __, index) => index + 1,
    },
    {
      title: 'Tên quiz',
      dataIndex: 'title',
      render: (title: string, quiz) => (
        <Button
          type="link"
          style={{ padding: 0, height: 'auto', textAlign: 'left', whiteSpace: 'normal', overflowWrap: 'anywhere' }}
          onClick={() => setSelectedQuizId(quiz.id)}
        >
          {title}
        </Button>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      width: 140,
      render: (status: QuizStatus) => statusTag(status),
    },
    {
      title: 'Số câu hỏi',
      dataIndex: 'questionCount',
      width: 120,
      align: 'center',
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 140,
      align: 'right',
      render: (_, quiz) => (
        <Button icon={<EyeOutlined />} onClick={() => setSelectedQuizId(quiz.id)}>
          Xem chi tiết
        </Button>
      ),
    },
  ]

  const detail = quizDetailQuery.data

  return (
    <>
      <Card
        title="Danh sách quiz của bài học"
        extra={<Typography.Text type="secondary">Chỉ đọc · {quizzes.length} quiz</Typography.Text>}
      >
        {quizzesQuery.isLoading ? (
          <LoadingState tip="Đang tải danh sách quiz..." />
        ) : quizzesQuery.isError ? (
          <ErrorState error={quizzesQuery.error} onRetry={() => void quizzesQuery.refetch()} />
        ) : quizzes.length === 0 ? (
          <EmptyState description="Bài học chưa có quiz nào." />
        ) : (
          <Table<QuizSummaryItem>
            rowKey="id"
            columns={columns}
            dataSource={quizzes}
            pagination={quizzes.length > 10 ? { pageSize: 10, showSizeChanger: false } : false}
            scroll={{ x: 'max-content' }}
          />
        )}
      </Card>

      <Drawer
        open={Boolean(selectedQuizId)}
        onClose={() => setSelectedQuizId(null)}
        title={detail ? detail.title : 'Chi tiết quiz'}
        extra={detail ? statusTag(detail.status) : undefined}
        size="large"
        styles={{ wrapper: { maxWidth: '100vw' } }}
        destroyOnHidden
      >
        {quizDetailQuery.isLoading ? (
          <LoadingState tip="Đang tải nội dung quiz..." />
        ) : quizDetailQuery.isError ? (
          <ErrorState error={quizDetailQuery.error} onRetry={() => void quizDetailQuery.refetch()} />
        ) : !detail ? null : (
          <Space orientation="vertical" size={16} style={{ width: '100%' }}>
            <Alert
              type="info"
              showIcon
              title="Chế độ xem chỉ đọc cho quản trị viên"
              description={`Tổng số câu hỏi: ${detail.questions.length} · Tạo lúc: ${formatDateTime(detail.createdAt)}`}
            />

            {detail.questions.length === 0 ? (
              <EmptyState description="Quiz chưa có câu hỏi nào." />
            ) : (
              detail.questions.map((question, questionIndex) => (
                <Card
                  key={question.id ?? questionIndex}
                  size="small"
                  title={(
                    <Space wrap size={8}>
                      <Typography.Text strong>Câu {questionIndex + 1}</Typography.Text>
                      {question.generatedByAi && <Tag color="purple">AI tạo</Tag>}
                    </Space>
                  )}
                >
                  <Space orientation="vertical" size={12} style={{ width: '100%' }}>
                    <Typography.Paragraph
                      style={{ margin: 0, whiteSpace: 'pre-wrap', overflowWrap: 'anywhere', fontWeight: 500 }}
                    >
                      {question.content}
                    </Typography.Paragraph>

                    <Space orientation="vertical" size={8} style={{ width: '100%' }}>
                      {question.options.map((option, optionIndex) => (
                        <div
                          key={option.id ?? optionIndex}
                          style={{
                            display: 'flex',
                            alignItems: 'flex-start',
                            justifyContent: 'space-between',
                            gap: 10,
                            padding: '8px 12px',
                            borderRadius: 8,
                            border: `1px solid ${option.isCorrect ? 'var(--success, #16a34a)' : 'var(--border, #e5e7eb)'}`,
                            background: option.isCorrect ? 'var(--success-soft, rgba(22, 163, 74, 0.08))' : 'transparent',
                          }}
                        >
                          <div style={{ display: 'flex', alignItems: 'flex-start', gap: 8, minWidth: 0, flex: 1 }}>
                            <Typography.Text strong style={{ flexShrink: 0 }}>
                              {optionLabel(optionIndex)}.
                            </Typography.Text>
                            <Typography.Text style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>
                              {option.content}
                            </Typography.Text>
                          </div>
                          {option.isCorrect && (
                            <Tag
                              color="success"
                              icon={<CheckCircleFilled />}
                              style={{ marginInlineEnd: 0, flexShrink: 0 }}
                            >
                              Đáp án đúng
                            </Tag>
                          )}
                        </div>
                      ))}
                    </Space>

                    {question.explanation && question.explanation.trim().length > 0 && (
                      <div
                        style={{
                          padding: '8px 12px',
                          borderRadius: 8,
                          background: 'var(--surface-2, rgba(148, 163, 184, 0.12))',
                        }}
                      >
                        <Typography.Text strong style={{ display: 'block', marginBottom: 4 }}>
                          Giải thích
                        </Typography.Text>
                        <Typography.Paragraph
                          style={{ margin: 0, whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}
                        >
                          {question.explanation}
                        </Typography.Paragraph>
                      </div>
                    )}
                  </Space>
                </Card>
              ))
            )}
          </Space>
        )}
      </Drawer>
    </>
  )
}
