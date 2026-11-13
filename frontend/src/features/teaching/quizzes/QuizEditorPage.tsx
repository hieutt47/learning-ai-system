import {
  CheckCircleFilled,
  CloseCircleFilled,
  DeleteOutlined,
  PlusOutlined,
  SaveOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  App,
  Button,
  Card,
  Col,
  Divider,
  Input,
  Radio,
  Row,
  Space,
  Tag,
  Typography,
} from 'antd'
import { useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { ErrorState, LoadingState } from '../../../shared/components/PageState'
import { usePageHeader } from '../../../shared/components/pageHeader'
import { paths } from '../../../shared/routing/paths'
import {
  addQuestion,
  createQuiz,
  deleteQuestion,
  getQuiz,
  publishQuiz,
  quizKeys,
  updateQuestion,
  updateQuiz,
} from './quiz.api'
import type { QuizAuthorDetail, QuizOptionAuthor, QuizQuestionAuthor, QuizStatus } from './quiz.types'

interface LocalQuestion extends Omit<QuizQuestionAuthor, 'options'> {
  options: QuizOptionAuthor[]
  isNew?: boolean
}

export function QuizEditorPage() {
  const { quizId = '' } = useParams()

  const quizQuery = useQuery({
    queryKey: quizKeys.detail(quizId),
    queryFn: () => getQuiz(quizId),
    enabled: Boolean(quizId),
  })

  const quiz = quizQuery.data

  usePageHeader(
    quiz
      ? {
          title: `Soạn quiz: ${quiz.title}`,
          crumbs: [
            { label: 'Khóa học giảng dạy', to: paths.teachingCourses },
            { label: 'Quiz của bài', to: paths.teachingLessonQuizzes(quiz.lessonId) },
            { label: quiz.title },
          ],
        }
      : null,
  )

  if (quizQuery.isLoading) return <LoadingState tip="Đang tải nội dung quiz…" />
  if (quizQuery.isError || !quiz) {
    return <ErrorState error={quizQuery.error} onRetry={() => void quizQuery.refetch()} />
  }

  return <QuizEditorForm key={quiz.id} quiz={quiz} />
}

function QuizEditorForm({ quiz }: { quiz: QuizAuthorDetail }) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { message, modal } = App.useApp()

  const isReadOnly = quiz.status !== 'DRAFT'

  const [title, setTitle] = useState(quiz.title)
  const [questions, setQuestions] = useState<LocalQuestion[]>(() =>
    quiz.questions.length > 0
      ? quiz.questions.map((q) => ({
          ...q,
          options: q.options.map((opt) => ({ ...opt })),
        }))
      : [
          {
            content: '',
            explanation: '',
            orderIndex: 0,
            isNew: true,
            options: [
              { content: '', isCorrect: true, orderIndex: 0 },
              { content: '', isCorrect: false, orderIndex: 1 },
            ],
          },
        ],
  )
  const [deletedQuestionIds, setDeletedQuestionIds] = useState<string[]>([])
  const [isDirty, setIsDirty] = useState(false)

  // Checklist kiểm tra điều kiện xuất bản
  const validation = useMemo(() => {
    const hasQuestions = questions.length >= 1
    const validOptionCount = questions.every(
      (q) => q.options.length >= 2 && q.options.length <= 6,
    )
    const exactlyOneCorrect = questions.every(
      (q) => q.options.filter((o) => o.isCorrect).length === 1,
    )
    const allContentFilled = questions.every(
      (q) =>
        q.content.trim().length > 0 &&
        q.options.every((o) => o.content.trim().length > 0),
    )

    const canPublish =
      hasQuestions && validOptionCount && exactlyOneCorrect && allContentFilled

    return {
      hasQuestions,
      validOptionCount,
      exactlyOneCorrect,
      allContentFilled,
      canPublish,
    }
  }, [questions])

  // Mutation lưu quiz và câu hỏi
  const saveMutation = useMutation({
    mutationFn: async () => {
      if (title.trim() && title !== quiz.title) {
        await updateQuiz(quiz.id, { title: title.trim() })
      }

      for (const id of deletedQuestionIds) {
        await deleteQuestion(id)
      }

      for (let i = 0; i < questions.length; i++) {
        const q = questions[i]
        const payload = {
          content: q.content.trim(),
          explanation: q.explanation?.trim() || undefined,
          order_index: i,
          options: q.options.map((opt, optIndex) => ({
            content: opt.content.trim(),
            is_correct: opt.isCorrect,
            order_index: optIndex,
          })),
        }

        if (q.id && !q.isNew) {
          await updateQuestion(q.id, payload)
        } else {
          await addQuestion(quiz.id, payload)
        }
      }
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: quizKeys.detail(quiz.id) })
      await queryClient.invalidateQueries({ queryKey: quizKeys.lesson(quiz.lessonId) })
      setIsDirty(false)
      setDeletedQuestionIds([])
      void message.success('Đã lưu nội dung quiz.')
    },
    onError: (error: Error) => void message.error(error.message),
  })

  // Mutation xuất bản
  const publishMutation = useMutation({
    mutationFn: async () => {
      if (isDirty) {
        await saveMutation.mutateAsync()
      }
      return publishQuiz(quiz.id, 'PUBLISHED')
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: quizKeys.detail(quiz.id) })
      await queryClient.invalidateQueries({ queryKey: quizKeys.lesson(quiz.lessonId) })
      void message.success('Xuất bản quiz thành công! Sinh viên đã có thể làm bài.')
    },
    onError: (error: Error) => void message.error(error.message),
  })

  // Tạo bản nháp mới từ quiz đã đóng/xuất bản
  const cloneMutation = useMutation({
    mutationFn: async () => {
      const newQuiz = await createQuiz(quiz.lessonId, {
        title: `${quiz.title} (bản sao)`,
      })
      for (let i = 0; i < quiz.questions.length; i++) {
        const q = quiz.questions[i]
        await addQuestion(newQuiz.id, {
          content: q.content,
          explanation: q.explanation,
          order_index: i,
          options: q.options.map((opt, optIndex) => ({
            content: opt.content,
            is_correct: opt.isCorrect,
            order_index: optIndex,
          })),
        })
      }
      return newQuiz
    },
    onSuccess: (newQuiz) => {
      if (newQuiz) {
        void message.success('Đã tạo bản nháp mới từ quiz này.')
        navigate(paths.quizEditor(newQuiz.id))
      }
    },
    onError: (error: Error) => void message.error(error.message),
  })

  const handleAddQuestion = () => {
    setQuestions((prev) => [
      ...prev,
      {
        content: '',
        explanation: '',
        orderIndex: prev.length,
        isNew: true,
        options: [
          { content: '', isCorrect: true, orderIndex: 0 },
          { content: '', isCorrect: false, orderIndex: 1 },
        ],
      },
    ])
    setIsDirty(true)
  }

  const handleDeleteQuestion = (index: number) => {
    const target = questions[index]
    if (target.id && !target.isNew) {
      setDeletedQuestionIds((prev) => [...prev, target.id!])
    }
    setQuestions((prev) => prev.filter((_, i) => i !== index))
    setIsDirty(true)
  }

  const handleQuestionContentChange = (index: number, content: string) => {
    setQuestions((prev) =>
      prev.map((q, i) => (i === index ? { ...q, content } : q)),
    )
    setIsDirty(true)
  }

  const handleExplanationChange = (index: number, explanation: string) => {
    setQuestions((prev) =>
      prev.map((q, i) => (i === index ? { ...q, explanation } : q)),
    )
    setIsDirty(true)
  }

  const handleAddOption = (questionIndex: number) => {
    setQuestions((prev) =>
      prev.map((q, i) => {
        if (i !== questionIndex) return q
        if (q.options.length >= 6) return q
        return {
          ...q,
          options: [
            ...q.options,
            { content: '', isCorrect: false, orderIndex: q.options.length },
          ],
        }
      }),
    )
    setIsDirty(true)
  }

  const handleDeleteOption = (questionIndex: number, optionIndex: number) => {
    setQuestions((prev) =>
      prev.map((q, i) => {
        if (i !== questionIndex) return q
        if (q.options.length <= 2) return q
        const newOptions = q.options.filter((_, oIndex) => oIndex !== optionIndex)
        const hasCorrect = newOptions.some((o) => o.isCorrect)
        if (!hasCorrect && newOptions.length > 0) {
          newOptions[0].isCorrect = true
        }
        return {
          ...q,
          options: newOptions.map((opt, idx) => ({ ...opt, orderIndex: idx })),
        }
      }),
    )
    setIsDirty(true)
  }

  const handleOptionContentChange = (
    questionIndex: number,
    optionIndex: number,
    content: string,
  ) => {
    setQuestions((prev) =>
      prev.map((q, i) => {
        if (i !== questionIndex) return q
        return {
          ...q,
          options: q.options.map((opt, oIndex) =>
            oIndex === optionIndex ? { ...opt, content } : opt,
          ),
        }
      }),
    )
    setIsDirty(true)
  }

  const handleSetCorrectOption = (
    questionIndex: number,
    correctOptionIndex: number,
  ) => {
    setQuestions((prev) =>
      prev.map((q, i) => {
        if (i !== questionIndex) return q
        return {
          ...q,
          options: q.options.map((opt, oIndex) => ({
            ...opt,
            isCorrect: oIndex === correctOptionIndex,
          })),
        }
      }),
    )
    setIsDirty(true)
  }

  const handleConfirmPublish = () => {
    modal.confirm({
      title: 'Xuất bản quiz?',
      content:
        'Sau khi xuất bản, quiz sẽ hiển thị cho sinh viên làm bài và không thể sửa đổi nội dung câu hỏi được nữa.',
      okText: 'Xuất bản ngay',
      cancelText: 'Hủy',
      onOk: () => publishMutation.mutateAsync(),
    })
  }

  const statusBadge = (status: QuizStatus) => {
    switch (status) {
      case 'DRAFT':
        return <Tag color="warning">Bản nháp</Tag>
      case 'PUBLISHED':
        return <Tag color="success">Đã xuất bản</Tag>
      case 'CLOSED':
        return <Tag color="default">Đã đóng</Tag>
    }
  }

  return (
    <Row gutter={[24, 24]}>
      {/* Cột trái: Tên và danh sách câu hỏi */}
      <Col xs={24} lg={17}>
        <Space orientation="vertical" size={16} style={{ width: '100%' }}>
          {isReadOnly && (
            <Alert
              type="info"
              showIcon
              message={`Quiz ${
                quiz.status === 'PUBLISHED' ? 'đã xuất bản' : 'đã đóng'
              } nên ở chế độ chỉ đọc. Bấm "Tạo bản nháp mới" nếu bạn cần thay đổi câu hỏi.`}
            />
          )}

          <Card title="Thông tin quiz">
            <Typography.Text strong>Tên quiz</Typography.Text>
            <Input
              value={title}
              disabled={isReadOnly}
              onChange={(e) => {
                setTitle(e.target.value)
                setIsDirty(true)
              }}
              placeholder="Nhập tên quiz…"
              maxLength={200}
              style={{ marginTop: 8 }}
            />
          </Card>

          {/* Danh sách câu hỏi */}
          {questions.map((q, qIndex) => (
            <Card
              key={q.id || `new-${qIndex}`}
              title={
                <Space>
                  <span>Câu {qIndex + 1}</span>
                  {q.generatedByAi && (
                    <Tag color="purple" icon={<ThunderboltOutlined />}>
                      AI tạo
                    </Tag>
                  )}
                </Space>
              }
              extra={
                !isReadOnly && (
                  <Button
                    type="text"
                    danger
                    icon={<DeleteOutlined />}
                    disabled={questions.length <= 1}
                    onClick={() => handleDeleteQuestion(qIndex)}
                  >
                    Xóa câu
                  </Button>
                )
              }
            >
              <Space orientation="vertical" size={16} style={{ width: '100%' }}>
                <div>
                  <Typography.Text strong>
                    Nội dung câu hỏi <span style={{ color: 'red' }}>*</span>
                  </Typography.Text>
                  <Input.TextArea
                    value={q.content}
                    disabled={isReadOnly}
                    placeholder="Nhập nội dung câu hỏi…"
                    autoSize={{ minRows: 2, maxRows: 5 }}
                    onChange={(e) =>
                      handleQuestionContentChange(qIndex, e.target.value)
                    }
                    style={{ marginTop: 6 }}
                  />
                </div>

                <div>
                  <Typography.Text strong>
                    Các lựa chọn đáp án (chọn tròn để đặt làm đáp án đúng){' '}
                    <span style={{ color: 'red' }}>*</span>
                  </Typography.Text>
                  <div style={{ marginTop: 8 }}>
                    <Radio.Group
                      style={{ width: '100%' }}
                      value={q.options.findIndex((o) => o.isCorrect)}
                      onChange={(e) =>
                        handleSetCorrectOption(qIndex, e.target.value)
                      }
                      disabled={isReadOnly}
                    >
                      <Space orientation="vertical" size={12} style={{ width: '100%' }}>
                        {q.options.map((opt, oIndex) => (
                          <Row
                            key={opt.id || `opt-${oIndex}`}
                            gutter={8}
                            align="middle"
                            style={{ width: '100%' }}
                          >
                            <Col flex="32px">
                              <Radio value={oIndex} />
                            </Col>
                            <Col flex="auto">
                              <Input
                                value={opt.content}
                                disabled={isReadOnly}
                                placeholder={`Nội dung lựa chọn ${oIndex + 1}…`}
                                onChange={(e) =>
                                  handleOptionContentChange(
                                    qIndex,
                                    oIndex,
                                    e.target.value,
                                  )
                                }
                              />
                            </Col>
                            {!isReadOnly && (
                              <Col flex="32px">
                                <Button
                                  type="text"
                                  danger
                                  icon={<DeleteOutlined />}
                                  disabled={q.options.length <= 2}
                                  onClick={() =>
                                    handleDeleteOption(qIndex, oIndex)
                                  }
                                />
                              </Col>
                            )}
                          </Row>
                        ))}
                      </Space>
                    </Radio.Group>
                  </div>

                  {!isReadOnly && q.options.length < 6 && (
                    <Button
                      type="dashed"
                      size="small"
                      icon={<PlusOutlined />}
                      onClick={() => handleAddOption(qIndex)}
                      style={{ marginTop: 10 }}
                    >
                      Thêm đáp án ({q.options.length}/6)
                    </Button>
                  )}
                </div>

                <div>
                  <Typography.Text type="secondary">
                    Giải thích (hiển thị cho học viên sau khi nộp bài)
                  </Typography.Text>
                  <Input.TextArea
                    value={q.explanation ?? ''}
                    disabled={isReadOnly}
                    placeholder="Giải thích vì sao đáp án này đúng…"
                    autoSize={{ minRows: 2, maxRows: 4 }}
                    onChange={(e) =>
                      handleExplanationChange(qIndex, e.target.value)
                    }
                    style={{ marginTop: 6 }}
                  />
                </div>
              </Space>
            </Card>
          ))}

          {!isReadOnly && (
            <Button
              type="dashed"
              icon={<PlusOutlined />}
              onClick={handleAddQuestion}
              style={{ width: '100%', height: 48 }}
            >
              Thêm câu hỏi mới
            </Button>
          )}
        </Space>
      </Col>

      {/* Cột phải: Sidebar điều kiện xuất bản & Hành động */}
      <Col xs={24} lg={7}>
        <Space orientation="vertical" size={16} style={{ width: '100%', position: 'sticky', top: 20 }}>
          <Card title="Trạng thái">
            <Space orientation="vertical" size={12} style={{ width: '100%' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span>Hiện tại:</span>
                {statusBadge(quiz.status)}
              </div>
              <div>
                <Typography.Text type="secondary">Tổng số câu hỏi: </Typography.Text>
                <strong>{questions.length}</strong>
              </div>

              <Divider style={{ margin: '8px 0' }} />

              {quiz.status === 'DRAFT' ? (
                <Space orientation="vertical" size={10} style={{ width: '100%' }}>
                  <Button
                    type="default"
                    icon={<SaveOutlined />}
                    block
                    loading={saveMutation.isPending}
                    onClick={() => saveMutation.mutate()}
                  >
                    Lưu nháp
                  </Button>
                  <Button
                    type="primary"
                    block
                    disabled={!validation.canPublish || publishMutation.isPending}
                    loading={publishMutation.isPending}
                    onClick={handleConfirmPublish}
                  >
                    Xuất bản quiz
                  </Button>
                </Space>
              ) : (
                <Button
                  type="primary"
                  block
                  loading={cloneMutation.isPending}
                  onClick={() => cloneMutation.mutate()}
                >
                  Tạo bản nháp mới
                </Button>
              )}
            </Space>
          </Card>

          {quiz.status === 'DRAFT' && (
            <Card title="Điều kiện xuất bản">
              <Space orientation="vertical" size={10} style={{ width: '100%' }}>
                <CheckItem
                  passed={validation.hasQuestions}
                  text="Có ít nhất 1 câu hỏi"
                />
                <CheckItem
                  passed={validation.validOptionCount}
                  text="Mỗi câu có từ 2 đến 6 đáp án"
                />
                <CheckItem
                  passed={validation.exactlyOneCorrect}
                  text="Mỗi câu có đúng 1 đáp án đúng"
                />
                <CheckItem
                  passed={validation.allContentFilled}
                  text="Câu hỏi và đáp án không được để trống"
                />
              </Space>
            </Card>
          )}
        </Space>
      </Col>
    </Row>
  )
}

function CheckItem({ passed, text }: { passed: boolean; text: string }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
      {passed ? (
        <CheckCircleFilled style={{ color: '#52c41a', fontSize: 16 }} />
      ) : (
        <CloseCircleFilled style={{ color: '#ff4d4f', fontSize: 16 }} />
      )}
      <span style={{ color: passed ? 'inherit' : 'rgba(0, 0, 0, 0.45)' }}>{text}</span>
    </div>
  )
}
