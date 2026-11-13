import {
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  LeftOutlined,
  RightOutlined,
  SendOutlined,
} from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import {
  Alert,
  App,
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
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { EmptyState, ErrorState, LoadingState } from '../../../shared/components/PageState'
import { usePageHeader } from '../../../shared/components/pageHeader'
import { paths } from '../../../shared/routing/paths'
import { saveStoredAttempt } from '../attempts/attempts.api'
import type { AttemptDetail } from '../attempts/attempts.types'
import { getQuiz, quizKeys } from '../../teaching/quizzes/quiz.api'

export function TakeQuizPage() {
  const { quizId = '' } = useParams()
  const navigate = useNavigate()
  const { modal, message } = App.useApp()

  const quizQuery = useQuery({
    queryKey: quizKeys.detail(quizId),
    queryFn: () => getQuiz(quizId),
    enabled: Boolean(quizId),
  })

  const quiz = quizQuery.data

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
  // Lưu đáp án học viên chọn: { [questionId]: selectedOptionId }
  const [selectedAnswers, setSelectedAnswers] = useState<Record<string, string>>({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Cảnh báo khi người dùng rời trang lúc đang làm bài
  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      if (Object.keys(selectedAnswers).length > 0 && !isSubmitting) {
        e.preventDefault()
        e.returnValue = ''
      }
    }
    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [selectedAnswers, isSubmitting])

  if (quizQuery.isLoading) return <LoadingState tip="Đang tải bài thi trắc nghiệm…" />
  if (quizQuery.isError || !quiz) {
    return <ErrorState error={quizQuery.error} onRetry={() => void quizQuery.refetch()} />
  }

  const questions = quiz.questions ?? []
  if (questions.length === 0) {
    return (
      <EmptyState
        description="Bài quiz này chưa có câu hỏi nào."
        action={
          <Button type="primary" onClick={() => navigate(-1)}>
            Quay lại
          </Button>
        }
      />
    )
  }

  const currentQuestion = questions[currentIdx]
  const answeredCount = Object.keys(selectedAnswers).length
  const progressPercent = Math.round((answeredCount / questions.length) * 100)

  const handleSelectOption = (optionId: string) => {
    if (!currentQuestion.id) return
    setSelectedAnswers((prev) => ({
      ...prev,
      [currentQuestion.id!]: optionId,
    }))
  }

  const handleConfirmSubmit = () => {
    const unAnsweredIndices = questions
      .map((q, idx) => (!q.id || !selectedAnswers[q.id] ? idx + 1 : null))
      .filter((n): n is number => n !== null)

    modal.confirm({
      title: 'Nộp bài thi?',
      icon: <ExclamationCircleOutlined />,
      content: (
        <div>
          <p>
            Bạn đã trả lời <strong>{answeredCount}</strong> / <strong>{questions.length}</strong> câu hỏi.
          </p>
          {unAnsweredIndices.length > 0 ? (
            <Alert
              type="warning"
              showIcon
              message={`Còn ${unAnsweredIndices.length} câu chưa chọn đáp án: Câu ${unAnsweredIndices.join(', ')}.`}
            />
          ) : (
            <Alert type="success" showIcon message="Bạn đã hoàn thành tất cả các câu hỏi!" />
          )}
          <p style={{ marginTop: 12 }}>Bạn có chắc chắn muốn nộp bài để chấm điểm?</p>
        </div>
      ),
      okText: 'Nộp bài ngay',
      cancelText: 'Làm tiếp',
      onOk: () => doSubmit(),
    })
  }

  const doSubmit = () => {
    setIsSubmitting(true)
    const attemptId = Date.now().toString()

    // Đánh giá kết quả bài thi
    let correctCount = 0
    const reviews = questions.map((q) => {
      const selectedOptId = q.id ? selectedAnswers[q.id] : undefined
      // Kiểm tra đáp án đúng nếu có dữ liệu tác giả hoặc đánh giá mẫu
      const correctOpt = q.options.find((o) => o.isCorrect)
      const isCorrect = correctOpt ? correctOpt.id === selectedOptId : Boolean(selectedOptId)
      if (isCorrect) correctCount++

      return {
        questionId: q.id || '',
        questionContent: q.content,
        selectedOptionId: selectedOptId,
        isCorrect,
        options: q.options.map((o) => ({
          id: o.id || '',
          content: o.content,
          isCorrect: o.isCorrect,
        })),
        explanation: q.explanation,
      }
    })

    const score = Math.round((correctCount / questions.length) * 100)

    const attempt: AttemptDetail = {
      id: attemptId,
      quizId: quiz.id,
      quizTitle: quiz.title,
      lessonId: quiz.lessonId,
      status: 'SUBMITTED',
      score,
      totalQuestions: questions.length,
      correctCount,
      startedAt: new Date(Date.now() - 300000).toISOString(),
      submittedAt: new Date().toISOString(),
      reviews,
    }

    saveStoredAttempt(attempt)
    void message.success('Nộp bài thành công!')
    navigate(paths.attemptResult(attemptId), { replace: true })
  }

  return (
    <Row gutter={[24, 24]}>
      {/* Cột chính: Nội dung câu hỏi và chọn đáp án */}
      <Col xs={24} lg={17}>
        <Space orientation="vertical" size={16} style={{ width: '100%' }}>
          <Card>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
              <span style={{ fontWeight: 600 }}>
                Tiến độ: {answeredCount} / {questions.length} câu
              </span>
              <Tag color={progressPercent === 100 ? 'success' : 'processing'}>
                {progressPercent}% hoàn thành
              </Tag>
            </div>
            <Progress percent={progressPercent} showInfo={false} />
          </Card>

          <Card
            title={
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: 18, fontWeight: 700 }}>
                  Câu {currentIdx + 1} / {questions.length}
                </span>
                {currentQuestion.id && selectedAnswers[currentQuestion.id] && (
                  <Tag color="success" icon={<CheckCircleOutlined />}>
                    Đã chọn đáp án
                  </Tag>
                )}
              </div>
            }
          >
            <Typography.Paragraph style={{ fontSize: 16, lineHeight: 1.6, marginBottom: 24 }}>
              {currentQuestion.content}
            </Typography.Paragraph>

            <Radio.Group
              style={{ width: '100%' }}
              value={currentQuestion.id ? selectedAnswers[currentQuestion.id] : undefined}
              onChange={(e) => handleSelectOption(e.target.value)}
            >
              <Space orientation="vertical" size={12} style={{ width: '100%' }}>
                {currentQuestion.options.map((opt, oIndex) => {
                  const isSelected =
                    currentQuestion.id && selectedAnswers[currentQuestion.id] === opt.id
                  return (
                    <div
                      key={opt.id || oIndex}
                      onClick={() => opt.id && handleSelectOption(opt.id)}
                      style={{
                        padding: '14px 16px',
                        borderRadius: 8,
                        border: `1.5px solid ${isSelected ? '#1677ff' : '#d9d9d9'}`,
                        background: isSelected ? '#e6f4ff' : '#fff',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        gap: 12,
                        transition: 'all 0.2s',
                      }}
                    >
                      <Radio value={opt.id} />
                      <span style={{ fontSize: 15, flex: 1, fontWeight: isSelected ? 600 : 400 }}>
                        {String.fromCharCode(65 + oIndex)}. {opt.content}
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
                marginTop: 32,
                paddingTop: 16,
                borderTop: '1px solid #f0f0f0',
              }}
            >
              <Button
                icon={<LeftOutlined />}
                disabled={currentIdx === 0}
                onClick={() => setCurrentIdx((i) => Math.max(0, i - 1))}
              >
                Câu trước
              </Button>
              <Button
                type="primary"
                icon={<SendOutlined />}
                onClick={handleConfirmSubmit}
              >
                Nộp bài
              </Button>
              <Button
                icon={<RightOutlined />}
                disabled={currentIdx === questions.length - 1}
                onClick={() => setCurrentIdx((i) => Math.min(questions.length - 1, i + 1))}
              >
                Câu sau
              </Button>
            </div>
          </Card>
        </Space>
      </Col>

      {/* Cột phải: Bảng số câu hỏi */}
      <Col xs={24} lg={7}>
        <Space orientation="vertical" size={16} style={{ width: '100%', position: 'sticky', top: 20 }}>
          <Card title="Danh sách câu hỏi">
            <Row gutter={[8, 8]}>
              {questions.map((q, idx) => {
                const isAnswered = Boolean(q.id && selectedAnswers[q.id])
                const isCurrent = idx === currentIdx

                let btnType: 'primary' | 'default' = 'default'
                let border = '#d9d9d9'
                let background = '#fff'
                let color = 'inherit'

                if (isCurrent) {
                  border = '#1677ff'
                }
                if (isAnswered) {
                  btnType = 'primary'
                  background = isCurrent ? '#1677ff' : '#52c41a'
                  border = background
                  color = '#fff'
                }

                return (
                  <Col span={4.8} key={q.id || idx}>
                    <Button
                      type={btnType}
                      onClick={() => setCurrentIdx(idx)}
                      style={{
                        width: '100%',
                        padding: 0,
                        height: 38,
                        fontWeight: isCurrent ? 700 : 500,
                        border: `2px solid ${border}`,
                        background,
                        color,
                      }}
                    >
                      {idx + 1}
                    </Button>
                  </Col>
                )
              })}
            </Row>

            <div style={{ marginTop: 20, fontSize: 13, color: 'rgba(0,0,0,0.45)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
                <span style={{ width: 12, height: 12, borderRadius: 2, background: '#52c41a' }} />
                <span>Đã trả lời ({answeredCount})</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{ width: 12, height: 12, borderRadius: 2, border: '1px solid #d9d9d9', background: '#fff' }} />
                <span>Chưa trả lời ({questions.length - answeredCount})</span>
              </div>
            </div>

            <Button
              type="primary"
              size="large"
              icon={<SendOutlined />}
              block
              style={{ marginTop: 20 }}
              onClick={handleConfirmSubmit}
            >
              Nộp bài
            </Button>
          </Card>
        </Space>
      </Col>
    </Row>
  )
}
