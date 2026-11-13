import { CheckCircleOutlined, FormOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Button, Card, List, Space, Tag, Typography } from 'antd'
import { useNavigate, useParams } from 'react-router'
import { EmptyState, ErrorState, LoadingState } from '../../shared/components/PageState'
import { paths } from '../../shared/routing/paths'
import { getLessonQuizzes, quizKeys } from '../teaching/quizzes/quiz.api'
import type { QuizSummaryItem } from '../teaching/quizzes/quiz.types'

export function LearningQuizPage() {
  const { lessonId = '' } = useParams()
  const navigate = useNavigate()

  const quizzesQuery = useQuery({
    queryKey: quizKeys.lesson(lessonId),
    queryFn: () => getLessonQuizzes(lessonId),
    enabled: Boolean(lessonId),
  })

  if (quizzesQuery.isLoading) return <LoadingState tip="Đang tải danh sách quiz…" />
  if (quizzesQuery.isError) {
    return <ErrorState error={quizzesQuery.error} onRetry={() => void quizzesQuery.refetch()} />
  }

  // Phía học viên chỉ hiển thị các quiz đã PUBLISHED
  const quizzes = (quizzesQuery.data?.items ?? []).filter(
    (q) => q.status === 'PUBLISHED',
  )

  return (
    <Card title="Bài kiểm tra trắc nghiệm (Quiz)">
      {quizzes.length === 0 ? (
        <EmptyState
          description="Bài học này hiện chưa có bài trắc nghiệm nào."
        />
      ) : (
        <List<QuizSummaryItem>
          itemLayout="horizontal"
          dataSource={quizzes}
          renderItem={(quiz) => (
            <List.Item
              actions={[
                <Button
                  key="start"
                  type="primary"
                  icon={<FormOutlined />}
                  onClick={() => navigate(paths.takeQuiz(quiz.id))}
                >
                  Bắt đầu làm bài
                </Button>,
              ]}
            >
              <List.Item.Meta
                title={
                  <Space>
                    <span style={{ fontSize: 16, fontWeight: 600 }}>{quiz.title}</span>
                    <Tag color="success" icon={<CheckCircleOutlined />}>
                      Sẵn sàng
                    </Tag>
                  </Space>
                }
                description={
                  <Typography.Text type="secondary">
                    {quiz.questionCount} câu hỏi trắc nghiệm · Chấm điểm tự động ngay sau khi nộp
                  </Typography.Text>
                }
              />
            </List.Item>
          )}
        />
      )}
    </Card>
  )
}
