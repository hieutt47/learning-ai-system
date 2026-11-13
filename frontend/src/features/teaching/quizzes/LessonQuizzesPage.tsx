import {
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  PlusOutlined,
  StopOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Card, Form, Input, Modal, Space, Table, Tag, Tooltip, Typography, type TableProps } from 'antd'
import { useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { EmptyState, ErrorState, LoadingState } from '../../../shared/components/PageState'
import { paths } from '../../../shared/routing/paths'
import { createQuiz, deleteQuiz, getLessonQuizzes, publishQuiz, quizKeys } from './quiz.api'
import type { QuizStatus, QuizSummaryItem } from './quiz.types'

const statusTag = (status: QuizStatus) => {
  switch (status) {
    case 'DRAFT':
      return <Tag color="warning">Bản nháp</Tag>
    case 'PUBLISHED':
      return <Tag color="success">Đã xuất bản</Tag>
    case 'CLOSED':
      return <Tag color="default">Đã đóng</Tag>
  }
}

export function LessonQuizzesPage() {
  const { lessonId = '' } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { message, modal } = App.useApp()

  const [isModalOpen, setIsModalOpen] = useState(false)
  const [form] = Form.useForm<{ title: string }>()

  const quizzesQuery = useQuery({
    queryKey: quizKeys.lesson(lessonId),
    queryFn: () => getLessonQuizzes(lessonId),
    enabled: Boolean(lessonId),
  })

  const quizzes = quizzesQuery.data?.items ?? []

  const createMutation = useMutation({
    mutationFn: (title: string) => createQuiz(lessonId, { title }),
    onSuccess: (newQuiz) => {
      void message.success('Đã tạo quiz nháp.')
      setIsModalOpen(false)
      form.resetFields()
      navigate(paths.quizEditor(newQuiz.id))
    },
    onError: (error: Error) => void message.error(error.message),
  })

  const deleteMutation = useMutation({
    mutationFn: deleteQuiz,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: quizKeys.lesson(lessonId) })
      void message.success('Đã xóa quiz nháp.')
    },
    onError: (error: Error) => void message.error(error.message),
  })

  const closeMutation = useMutation({
    mutationFn: (quizId: string) => publishQuiz(quizId, 'CLOSED'),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: quizKeys.lesson(lessonId) })
      void message.success('Đã đóng quiz thành công.')
    },
    onError: (error: Error) => void message.error(error.message),
  })

  const confirmDelete = (quiz: QuizSummaryItem) => {
    modal.confirm({
      title: 'Xóa quiz nháp?',
      content: `Bản nháp “${quiz.title}” sẽ bị xóa vĩnh viễn. Không thể hoàn tác.`,
      okText: 'Xóa',
      okButtonProps: { danger: true },
      cancelText: 'Hủy',
      onOk: () => deleteMutation.mutateAsync(quiz.id),
    })
  }

  const confirmClose = (quiz: QuizSummaryItem) => {
    modal.confirm({
      title: 'Đóng quiz?',
      content: 'Sinh viên sẽ không làm quiz này được nữa, nhưng kết quả đã nộp vẫn được giữ.',
      okText: 'Đóng quiz',
      cancelText: 'Hủy',
      onOk: () => closeMutation.mutateAsync(quiz.id),
    })
  }

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
          style={{ padding: 0 }}
          onClick={() => navigate(paths.quizEditor(quiz.id))}
        >
          {title}
        </Button>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      width: 140,
      align: 'center',
      render: (status: QuizStatus) => statusTag(status),
    },
    {
      title: 'Số câu',
      dataIndex: 'questionCount',
      width: 100,
      align: 'center',
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 160,
      render: (_, quiz) => (
        <Space>
          {quiz.status === 'DRAFT' ? (
            <>
              <Tooltip title="Chỉnh sửa câu hỏi">
                <Button
                  type="text"
                  icon={<EditOutlined />}
                  onClick={() => navigate(paths.quizEditor(quiz.id))}
                />
              </Tooltip>
              <Tooltip title="Xóa quiz nháp">
                <Button
                  type="text"
                  danger
                  icon={<DeleteOutlined />}
                  disabled={deleteMutation.isPending}
                  onClick={() => confirmDelete(quiz)}
                />
              </Tooltip>
            </>
          ) : (
            <>
              <Tooltip title="Xem chi tiết">
                <Button
                  type="text"
                  icon={<EyeOutlined />}
                  onClick={() => navigate(paths.quizEditor(quiz.id))}
                />
              </Tooltip>
              {quiz.status === 'PUBLISHED' && (
                <Tooltip title="Đóng quiz">
                  <Button
                    type="text"
                    icon={<StopOutlined />}
                    disabled={closeMutation.isPending}
                    onClick={() => confirmClose(quiz)}
                  />
                </Tooltip>
              )}
            </>
          )}
        </Space>
      ),
    },
  ]

  if (quizzesQuery.isLoading) return <LoadingState tip="Đang tải danh sách quiz…" />
  if (quizzesQuery.isError) {
    return <ErrorState error={quizzesQuery.error} onRetry={() => void quizzesQuery.refetch()} />
  }

  const actions = (
    <Space>
      <Button
        icon={<ThunderboltOutlined />}
        onClick={() => navigate(paths.teachingLessonAi(lessonId))}
      >
        Sinh quiz bằng AI
      </Button>
      <Button
        type="primary"
        icon={<PlusOutlined />}
        onClick={() => setIsModalOpen(true)}
      >
        Tạo quiz
      </Button>
    </Space>
  )

  return (
    <>
      <Card
        title={
          <div>
            <span>Quiz của bài ({quizzes.length})</span>
            <Typography.Paragraph type="secondary" style={{ marginBottom: 0, fontWeight: 'normal', fontSize: 13 }}>
              Quiz đã xuất bản không sửa được — tạo bản nháp mới nếu cần thay đổi.
            </Typography.Paragraph>
          </div>
        }
        extra={actions}
      >
        {quizzes.length === 0 ? (
          <EmptyState
            description="Bài học chưa có quiz nào."
            action={actions}
          />
        ) : (
          <Table<QuizSummaryItem>
            rowKey="id"
            columns={columns}
            dataSource={quizzes}
            pagination={false}
          />
        )}
      </Card>

      <Modal
        title="Tạo quiz mới"
        open={isModalOpen}
        okText="Tạo và soạn câu hỏi"
        cancelText="Hủy"
        confirmLoading={createMutation.isPending}
        onOk={() => form.submit()}
        onCancel={() => {
          setIsModalOpen(false)
          form.resetFields()
        }}
      >
        <Form<{ title: string }>
          form={form}
          layout="vertical"
          onFinish={(values) => createMutation.mutate(values.title.trim())}
        >
          <Form.Item
            name="title"
            label="Tên quiz"
            rules={[{ required: true, whitespace: true, message: 'Vui lòng nhập tên quiz.' }]}
          >
            <Input placeholder="VD: Trắc nghiệm kiến thức bài 1" maxLength={200} showCount autoFocus />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
