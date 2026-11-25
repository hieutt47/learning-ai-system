import {
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  LeftOutlined,
  RightOutlined,
  SendOutlined,
} from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, App, Button, Card, Col, Modal, Progress, Radio, Row, Space, Tag, Typography, theme } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { EmptyState, ErrorState, LoadingState } from '../../../shared/components/PageState'
import { usePageHeader } from '../../../shared/components/pageHeader'
import { paths } from '../../../shared/routing/paths'
import { getStudentQuiz, quizKeys } from '../../teaching/quizzes/quiz.api'
import { submitAttempt } from '../attempts/attempts.api'
import { activeAttemptKey, activeAttemptOptions } from '../attempts/activeAttempt'

export function TakeQuizPage() {
  const { quizId = '' } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { message } = App.useApp()
  const { token } = theme.useToken()

  const quizQuery = useQuery({
    queryKey: [...quizKeys.detail(quizId), 'student'],
    queryFn: () => getStudentQuiz(quizId),
    enabled: Boolean(quizId),
  })
  const attemptQuery = useQuery(activeAttemptOptions(quizId))

  // Release only after leaving the page. Removing an observed query during submit
  // can recreate it on a render and execute the start POST again.
  useEffect(() => () => {
    queryClient.removeQueries({ queryKey: activeAttemptKey(quizId), exact: true })
  }, [queryClient, quizId])

  const quiz = quizQuery.data
  const attempt = attemptQuery.data

  usePageHeader(
    quiz
      ? {
          title: `Làm bài: ${quiz.title}`,
          crumbs: [
            { label: 'Trang chủ', to: paths.home },
            { label: 'Khóa học của tôi', to: paths.myCourses },
            { label: quiz.title },
          ],
        }
      : null,
  )

  const [currentIdx, setCurrentIdx] = useState(0)
  const [selectedAnswers, setSelectedAnswers] = useState<Record<string, string>>({})
  const [confirmOpen, setConfirmOpen] = useState(false)

  const submitMutation = useMutation({
    mutationFn: () => {
      if (!attempt || !quiz) throw new Error('Chưa khởi tạo được lượt làm bài.')
      return submitAttempt(
        attempt.id,
        quiz.questions.map((question) => ({
          question_id: question.id,
          selected_option_id: selectedAnswers[question.id] ?? null,
        })),
      )
    },
    onSuccess: (result) => {
      setConfirmOpen(false)
      void queryClient.invalidateQueries({ queryKey: ['attempts', 'mine'] })
      void message.success('Nộp bài thành công!')
      navigate(paths.attemptResult(result.id), { replace: true })
    },
    onError: (error: Error) => {
      setConfirmOpen(false)
      void message.error(error.message)
    },
  })

  const isSubmitting = submitMutation.isPending

  useEffect(() => {
    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      if (Object.keys(selectedAnswers).length > 0 && !submitMutation.isSuccess) {
        event.preventDefault()
        event.returnValue = ''
      }
    }
    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [selectedAnswers, submitMutation.isSuccess])

  if (quizQuery.isLoading || attemptQuery.isLoading) {
    return <LoadingState tip="Đang khởi tạo lượt làm bài…" />
  }
  if (quizQuery.isError) {
    return <ErrorState error={quizQuery.error} onRetry={() => void quizQuery.refetch()} />
  }
  if (attemptQuery.isError) {
    return <ErrorState error={attemptQuery.error} onRetry={() => void attemptQuery.refetch()} />
  }
  if (!quiz || !attempt) {
    return <ErrorState error={new Error('Không thể khởi tạo lượt làm bài.')} />
  }

  const questions = quiz.questions
  if (questions.length === 0) {
    return (
      <EmptyState
        description="Bài quiz này chưa có câu hỏi nào."
        action={<Button type="primary" onClick={() => navigate(-1)}>Quay lại</Button>}
      />
    )
  }

  const currentQuestion = questions[currentIdx]
  const answeredCount = Object.keys(selectedAnswers).length
  const progressPercent = Math.round((answeredCount / questions.length) * 100)

  const selectAnswer = (questionId: string, optionId: string) => {
    if (isSubmitting) return
    setSelectedAnswers((previous) => ({
      ...previous,
      [questionId]: optionId,
    }))
  }

  const navigateToQuestion = (nextIndex: number) => {
    if (isSubmitting) return
    setCurrentIdx(Math.max(0, Math.min(questions.length - 1, nextIndex)))
  }

  const openConfirmSubmit = () => {
    if (isSubmitting) return
    setConfirmOpen(true)
  }

  const unAnsweredIndices = questions
    .map((question, index) => (!selectedAnswers[question.id] ? index + 1 : null))
    .filter((number): number is number => number !== null)

  return (
    <>
      <Row gutter={[24, 24]}>
        <Col xs={24} lg={17}>
          <Space orientation="vertical" size={16} style={{ width: '100%' }}>
            <Card>
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  flexWrap: 'wrap',
                  gap: 8,
                  marginBottom: 8,
                }}
              >
                <span style={{ fontWeight: 600 }}>Tiến độ: {answeredCount} / {questions.length} câu</span>
                <Tag color={progressPercent === 100 ? 'success' : 'processing'}>{progressPercent}% hoàn thành</Tag>
              </div>
              <Progress percent={progressPercent} showInfo={false} />
            </Card>

            <Card
              title={
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    flexWrap: 'wrap',
                    gap: 8,
                  }}
                >
                  <span style={{ fontSize: 18, fontWeight: 700 }}>Câu {currentIdx + 1} / {questions.length}</span>
                  {selectedAnswers[currentQuestion.id] && (
                    <Tag color="success" icon={<CheckCircleOutlined />}>Đã chọn đáp án</Tag>
                  )}
                </div>
              }
            >
              <Typography.Paragraph
                style={{
                  fontSize: 16,
                  lineHeight: 1.6,
                  marginBottom: 24,
                  overflowWrap: 'anywhere',
                  wordBreak: 'break-word',
                }}
              >
                {currentQuestion.content}
              </Typography.Paragraph>

              <Radio.Group
                style={{ width: '100%' }}
                disabled={isSubmitting}
                value={selectedAnswers[currentQuestion.id]}
                onChange={(event) => selectAnswer(currentQuestion.id, event.target.value)}
              >
                <Space orientation="vertical" size={12} style={{ width: '100%' }}>
                  {currentQuestion.options.map((option, optionIndex) => {
                    const selected = selectedAnswers[currentQuestion.id] === option.id
                    return (
                      <div
                        key={option.id}
                        aria-disabled={isSubmitting || undefined}
                        onClick={() => selectAnswer(currentQuestion.id, option.id)}
                        style={{
                          padding: '14px 16px',
                          borderRadius: 8,
                          border: `1.5px solid ${selected ? token.colorPrimary : token.colorBorder}`,
                          background: selected ? token.colorPrimaryBg : token.colorBgContainer,
                          color: token.colorText,
                          cursor: isSubmitting ? 'not-allowed' : 'pointer',
                          opacity: isSubmitting ? 0.72 : 1,
                          display: 'flex',
                          alignItems: 'flex-start',
                          gap: 12,
                        }}
                      >
                        <Radio value={option.id} disabled={isSubmitting} style={{ marginTop: 2 }} />
                        <span
                          style={{
                            fontSize: 15,
                            flex: 1,
                            minWidth: 0,
                            fontWeight: selected ? 600 : 400,
                            overflowWrap: 'anywhere',
                            wordBreak: 'break-word',
                          }}
                        >
                          {String.fromCharCode(65 + optionIndex)}. {option.content}
                        </span>
                      </div>
                    )
                  })}
                </Space>
              </Radio.Group>

              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  flexWrap: 'wrap',
                  gap: 8,
                  marginTop: 32,
                  paddingTop: 16,
                  borderTop: `1px solid ${token.colorSplit}`,
                }}
              >
                <Button
                  icon={<LeftOutlined />}
                  disabled={currentIdx === 0 || isSubmitting}
                  onClick={() => navigateToQuestion(currentIdx - 1)}
                >
                  Câu trước
                </Button>
                <Button
                  type="primary"
                  icon={<SendOutlined />}
                  loading={isSubmitting}
                  disabled={isSubmitting}
                  onClick={openConfirmSubmit}
                >
                  Nộp bài
                </Button>
                <Button
                  icon={<RightOutlined />}
                  disabled={currentIdx === questions.length - 1 || isSubmitting}
                  onClick={() => navigateToQuestion(currentIdx + 1)}
                >
                  Câu sau
                </Button>
              </div>
            </Card>
          </Space>
        </Col>

        <Col xs={24} lg={7}>
          <Card title="Danh sách câu hỏi" style={{ position: 'sticky', top: 20 }}>
            <Row gutter={[8, 8]}>
              {questions.map((question, index) => {
                const answered = Boolean(selectedAnswers[question.id])
                const current = index === currentIdx
                return (
                  <Col span={4} key={question.id}>
                    <Button
                      type="default"
                      aria-current={current ? 'step' : undefined}
                      disabled={isSubmitting}
                      onClick={() => navigateToQuestion(index)}
                      style={{
                        width: '100%',
                        padding: 0,
                        height: 38,
                        fontWeight: current ? 700 : 500,
                        border: `2px solid ${current ? token.colorPrimary : answered ? token.colorSuccessBorder : token.colorBorder}`,
                        background: answered ? token.colorSuccessBg : token.colorBgContainer,
                        color: answered ? token.colorSuccessText : token.colorText,
                      }}
                    >
                      {index + 1}
                    </Button>
                  </Col>
                )
              })}
            </Row>
            <Button
              type="primary"
              size="large"
              icon={<SendOutlined />}
              block
              style={{ marginTop: 20 }}
              loading={isSubmitting}
              disabled={isSubmitting}
              onClick={openConfirmSubmit}
            >
              Nộp bài
            </Button>
          </Card>
        </Col>
      </Row>

      <Modal
        title={
          <Space>
            <ExclamationCircleOutlined style={{ color: token.colorWarning }} />
            <span>Nộp bài thi?</span>
          </Space>
        }
        open={confirmOpen}
        okText="Nộp bài ngay"
        cancelText="Làm tiếp"
        confirmLoading={isSubmitting}
        okButtonProps={{ disabled: isSubmitting }}
        cancelButtonProps={{ disabled: isSubmitting }}
        closable={!isSubmitting}
        maskClosable={!isSubmitting}
        keyboard={!isSubmitting}
        onOk={() => {
          if (!isSubmitting) submitMutation.mutate()
        }}
        onCancel={() => {
          if (!isSubmitting) setConfirmOpen(false)
        }}
      >
        <div>
          <p>Bạn đã trả lời <strong>{answeredCount}</strong> / <strong>{questions.length}</strong> câu hỏi.</p>
          {unAnsweredIndices.length > 0 ? (
            <Alert
              type="warning"
              showIcon
              message={`Còn ${unAnsweredIndices.length} câu chưa chọn đáp án: Câu ${unAnsweredIndices.join(', ')}.`}
            />
          ) : (
            <Alert type="success" showIcon message="Bạn đã hoàn thành tất cả các câu hỏi!" />
          )}
          <p style={{ marginTop: 12 }}>Bạn có chắc chắn muốn nộp bài để hệ thống chấm điểm?</p>
        </div>
      </Modal>
    </>
  )
}
