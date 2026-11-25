import {
  ArrowDownOutlined,
  ArrowUpOutlined,
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
  Tooltip,
  Typography,
  theme,
} from 'antd'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { isApiError } from '../../../shared/api/client'
import { ErrorState, LoadingState } from '../../../shared/components/PageState'
import { usePageHeader } from '../../../shared/components/pageHeader'
import { paths } from '../../../shared/routing/paths'
import { useLessonEditability } from '../lessons/useLessonEditability'
import {
  createQuiz,
  getQuiz,
  publishQuiz,
  quizKeys,
} from './quiz.api'
import {
  buildCloneTitle,
  formatDuplicateGroupMessage,
  MAX_EXPLANATION_LENGTH,
  MAX_OPTION_CONTENT_LENGTH,
  MAX_QUESTION_CONTENT_LENGTH,
  MAX_QUIZ_TITLE_LENGTH,
  optionLabel,
  validateQuizDraft,
} from './quizValidation'
import { createDraftSaver } from './saveQuizDraft'
import type { QuizAuthorDetail, QuizOptionAuthor, QuizQuestionAuthor, QuizStatus } from './quiz.types'

interface LocalQuestion extends Omit<QuizQuestionAuthor, 'options'> {
  options: QuizOptionAuthor[]
  isNew?: boolean
}

interface CloneRecoveryState {
  targetQuizId: string | null
  cloneTitle: string
  phase: 'create_uncertain' | 'copy_failed'
  errorMessage: string
}

function formatServerDraftError(error: Error): string {
  if (!isApiError(error)) {
    return error.message || 'Không thể lưu bản nháp. Vui lòng thử lại.'
  }
  if (error.status === 409) {
    return `Xung đột trạng thái khi lưu bản nháp: ${error.message}`
  }
  if (error.status === 400 && error.message === 'Request body is invalid') {
    return 'Máy chủ từ chối dữ liệu bản nháp (Request body is invalid). Vui lòng kiểm tra lại tiêu đề, nội dung câu hỏi và các đáp án rồi thử lưu lại.'
  }
  return error.message || 'Máy chủ từ chối dữ liệu bản nháp. Vui lòng kiểm tra lại và thử lại.'
}

function focusTargetById(targetId: string) {
  const element = document.getElementById(targetId)
  if (element) {
    element.scrollIntoView({ behavior: 'smooth', block: 'center' })
    element.focus()
  }
}

export function QuizEditorPage() {
  const { quizId = '' } = useParams()

  const quizQuery = useQuery({
    queryKey: quizKeys.detail(quizId),
    queryFn: () => getQuiz(quizId),
    enabled: Boolean(quizId),
  })

  const quiz = quizQuery.data
  const editability = useLessonEditability(quiz?.lessonId ?? '')

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
  if (editability.loading) return <LoadingState tip="Đang tải trạng thái bài học…" />
  if (editability.error) return <ErrorState error={editability.error} onRetry={editability.retry} />

  return <QuizEditorForm key={quiz.id} quiz={quiz} forceReadOnly={editability.readOnly} />
}

function QuizEditorForm({ quiz, forceReadOnly }: { quiz: QuizAuthorDetail; forceReadOnly: boolean }) {
  const { token } = theme.useToken()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { message, modal } = App.useApp()

  const [isSaving, setIsSaving] = useState(false)
  const [isPublishing, setIsPublishing] = useState(false)
  const [saveUncertain, setSaveUncertain] = useState(false)
  const [serverError, setServerError] = useState<string | null>(null)
  const [showValidationSummary, setShowValidationSummary] = useState(false)
  const [cloneRecovery, setCloneRecovery] = useState<CloneRecoveryState | null>(null)
  const [saveDraft] = useState(() => createDraftSaver())
  const cloneSaverRef = useRef<ReturnType<typeof createDraftSaver> | null>(null)

  const isStatusLocked = quiz.status !== 'DRAFT'
  const isBusy = isSaving || isPublishing
  const isInputDisabled = forceReadOnly || isStatusLocked || isBusy || saveUncertain

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
  const [isDirty, setIsDirty] = useState(false)

  // Cảnh báo khi người dùng rời trang lúc chưa lưu
  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      if (isDirty) {
        e.preventDefault()
        e.returnValue = ''
      }
    }
    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [isDirty])

  // Validation đồng bộ backend (tiêu đề, số câu/đáp án, bỏ trống, độ dài, đáp án trùng chuẩn hóa)
  const validation = useMemo(() => validateQuizDraft(title, questions), [title, questions])

  const markEdited = () => {
    setIsDirty(true)
    setServerError(null)
  }

  /**
   * Thực thi lưu nháp nguyên tử.
   * - Khi saveUncertain=true: replay ĐÚNG pending snapshot + request_id đã giữ trong saveDraft,
   *   không chạy validation mới rồi đổi key hoặc xóa pending.
   * - Khi saveUncertain=false: kiểm tra client validation trước khi gọi mạng.
   */
  const executeDraftSave = async (): Promise<QuizAuthorDetail | null> => {
    if (!saveUncertain && !validation.isValid) {
      setShowValidationSummary(true)
      const firstIssue = validation.issues[0]
      if (firstIssue) {
        focusTargetById(firstIssue.targetId)
      }
      return null
    }
    setIsSaving(true)
    setServerError(null)
    try {
      const saved = await saveDraft(quiz.id, title, questions)
      setTitle(saved.title)
      setQuestions(saved.questions.map((q) => ({ ...q, isNew: false })))
      queryClient.setQueryData(quizKeys.detail(quiz.id), saved)
      await queryClient.invalidateQueries({ queryKey: quizKeys.lesson(quiz.lessonId) })
      setSaveUncertain(false)
      setIsDirty(false)
      setShowValidationSummary(false)
      setServerError(null)
      return saved
    } catch (error) {
      const err = error instanceof Error ? error : new Error('Không thể lưu bản nháp.')
      const uncertain = !(isApiError(err) && err.status >= 400 && err.status < 500)
      setSaveUncertain(uncertain)
      if (!uncertain) {
        setServerError(formatServerDraftError(err))
      }
      throw err
    } finally {
      setIsSaving(false)
    }
  }

  const saveMutation = useMutation({
    mutationFn: executeDraftSave,
    onSuccess: (saved) => {
      if (saved) {
        void message.success('Đã lưu nội dung quiz thành công.')
      }
    },
    onError: (error: Error) => {
      const uncertain = !(isApiError(error) && error.status >= 400 && error.status < 500)
      void message.error(
        uncertain
          ? 'Chưa xác nhận được kết quả lưu. Bấm "Thử lưu lại" để gửi lại đúng lần lưu trước, không tạo trùng.'
          : formatServerDraftError(error),
      )
    },
  })

  // Mutation xuất bản: nếu dirty thì lưu trước trong cùng chuỗi, chỉ báo 1 lỗi duy nhất khi thất bại.
  const publishMutation = useMutation({
    mutationFn: async () => {
      if (!validation.isValid) {
        setShowValidationSummary(true)
        const firstIssue = validation.issues[0]
        if (firstIssue) {
          focusTargetById(firstIssue.targetId)
        }
        return null
      }
      setIsPublishing(true)
      try {
        if (isDirty) {
          const saved = await executeDraftSave()
          if (!saved) return null
        }
        return await publishQuiz(quiz.id, 'PUBLISHED')
      } finally {
        setIsPublishing(false)
      }
    },
    onSuccess: async (published) => {
      if (!published) return
      await queryClient.invalidateQueries({ queryKey: quizKeys.detail(quiz.id) })
      await queryClient.invalidateQueries({ queryKey: quizKeys.lesson(quiz.lessonId) })
      void message.success('Xuất bản quiz thành công! Sinh viên đã có thể bắt đầu làm bài.')
    },
    onError: (error: Error) => {
      const uncertain = !(isApiError(error) && error.status >= 400 && error.status < 500)
      const displayMsg = uncertain && isDirty
        ? 'Chưa xác nhận được kết quả lưu nháp nên chưa xuất bản. Bấm "Thử lưu lại" để gửi lại đúng lần lưu trước.'
        : formatServerDraftError(error)
      if (!uncertain) {
        setServerError(displayMsg)
      }
      void message.error(displayMsg)
    },
  })

  // Tạo bản nháp mới từ quiz đã đóng/xuất bản:
  // - Giới hạn 200 Unicode code points (an toàn emoji)
  // - Giữ destination quiz ID và request_id của bước copy nội dung (PUT /draft)
  //   để retry KHÔNG tạo thêm bản sao mới nếu bước sao chép câu hỏi thất bại.
  const cloneMutation = useMutation({
    mutationFn: async () => {
      const cloneTitle = cloneRecovery?.cloneTitle ?? buildCloneTitle(quiz.title)
      let targetQuizId = cloneRecovery?.targetQuizId ?? null

      if (!targetQuizId) {
        try {
          const newQuiz = await createQuiz(quiz.lessonId, { title: cloneTitle })
          targetQuizId = newQuiz.id
          cloneSaverRef.current = createDraftSaver()
        } catch (error) {
          const err = error instanceof Error ? error : new Error('Không thể tạo bản nháp mới.')
          const uncertain = !(isApiError(err) && err.status >= 400 && err.status < 500)
          if (uncertain) {
            setCloneRecovery({
              targetQuizId: null,
              cloneTitle,
              phase: 'create_uncertain',
              errorMessage:
                'Không nhận được phản hồi khi tạo bản nháp mới. Hãy kiểm tra danh sách quiz của bài trước khi thử lại để tránh tạo trùng bản sao.',
            })
          }
          throw err
        }
      }

      if (quiz.questions.length > 0) {
        try {
          const saver = cloneSaverRef.current ?? createDraftSaver()
          cloneSaverRef.current = saver
          const clonedQuestions: QuizQuestionAuthor[] = quiz.questions.map((q, index) => ({
            content: q.content,
            explanation: q.explanation,
            orderIndex: index,
            options: q.options.map((opt, optIndex) => ({
              content: opt.content,
              isCorrect: opt.isCorrect,
              orderIndex: optIndex,
            })),
          }))
          await saver(targetQuizId, cloneTitle, clonedQuestions)
        } catch (error) {
          const err = error instanceof Error ? error : new Error('Không thể sao chép câu hỏi sang bản nháp mới.')
          setCloneRecovery({
            targetQuizId,
            cloneTitle,
            phase: 'copy_failed',
            errorMessage: `Đã tạo bản nháp mới (ID #${targetQuizId}) nhưng chưa sao chép đủ câu hỏi: ${formatServerDraftError(err)}`,
          })
          throw err
        }
      }

      setCloneRecovery(null)
      cloneSaverRef.current = null
      return targetQuizId
    },
    onSuccess: async (newQuizId) => {
      await queryClient.invalidateQueries({ queryKey: quizKeys.lesson(quiz.lessonId) })
      void message.success('Đã tạo bản nháp mới từ quiz này.')
      navigate(paths.quizEditor(newQuizId))
    },
    onError: (error: Error) => {
      void message.error(formatServerDraftError(error))
    },
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
    markEdited()
  }

  const handleDeleteQuestion = (index: number) => {
    modal.confirm({
      title: `Xóa câu ${index + 1}?`,
      content: 'Nội dung câu hỏi này sẽ bị xóa khỏi bản nháp.',
      okText: 'Xóa câu',
      okButtonProps: { danger: true },
      cancelText: 'Hủy',
      onOk: () => {
        setQuestions((prev) => prev.filter((_, i) => i !== index))
        markEdited()
      },
    })
  }

  const handleMoveQuestion = (index: number, direction: 'UP' | 'DOWN') => {
    const targetIndex = direction === 'UP' ? index - 1 : index + 1
    if (targetIndex < 0 || targetIndex >= questions.length) return
    setQuestions((prev) => {
      const copy = [...prev]
      const temp = copy[index]
      copy[index] = copy[targetIndex]
      copy[targetIndex] = temp
      return copy.map((q, idx) => ({ ...q, orderIndex: idx }))
    })
    markEdited()
  }

  const handleQuestionContentChange = (index: number, content: string) => {
    setQuestions((prev) =>
      prev.map((q, i) => (i === index ? { ...q, content } : q)),
    )
    markEdited()
  }

  const handleExplanationChange = (index: number, explanation: string) => {
    setQuestions((prev) =>
      prev.map((q, i) => (i === index ? { ...q, explanation } : q)),
    )
    markEdited()
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
    markEdited()
  }

  const handleDeleteOption = (questionIndex: number, optionIndex: number) => {
    setQuestions((prev) =>
      prev.map((q, i) => {
        if (i !== questionIndex) return q
        if (q.options.length <= 2) return q
        const remaining = q.options.filter((_, oIndex) => oIndex !== optionIndex)
        const hasCorrect = remaining.some((o) => o.isCorrect)
        return {
          ...q,
          options: remaining.map((opt, idx) => ({
            ...opt,
            isCorrect: !hasCorrect && idx === 0 ? true : opt.isCorrect,
            orderIndex: idx,
          })),
        }
      }),
    )
    markEdited()
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
    markEdited()
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
    markEdited()
  }

  const handleConfirmPublish = () => {
    if (!validation.isValid) {
      setShowValidationSummary(true)
      const firstIssue = validation.issues[0]
      if (firstIssue) focusTargetById(firstIssue.targetId)
      return
    }
    modal.confirm({
      title: 'Xuất bản quiz?',
      content:
        'Sau khi xuất bản, quiz sẽ hiển thị cho sinh viên làm bài và không thể sửa đổi nội dung câu hỏi được nữa.',
      okText: 'Xuất bản ngay',
      cancelText: 'Hủy',
      onOk: () => publishMutation.mutateAsync().catch(() => undefined),
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

  const shouldShowSummary =
    !forceReadOnly &&
    quiz.status === 'DRAFT' &&
    !saveUncertain &&
    !validation.isValid &&
    (showValidationSummary || !validation.noDuplicateOptions)

  return (
    <Row gutter={[24, 24]}>
      {/* Cột trái: Tên và danh sách câu hỏi */}
      <Col xs={24} lg={17}>
        <Space orientation="vertical" size={16} style={{ width: '100%' }}>
          {forceReadOnly && (
            <Alert
              type="warning"
              showIcon
              message="Bài học hoặc khóa học đã lưu trữ: quiz chỉ xem, không thể chỉnh sửa hoặc tạo bản nháp mới."
            />
          )}

          {!forceReadOnly && quiz.status === 'PUBLISHED' && (
            <Alert
              type="info"
              showIcon
              message='Quiz đã xuất bản nên ở chế độ chỉ đọc. Bấm "Tạo bản nháp mới" nếu bạn cần thay đổi câu hỏi.'
            />
          )}

          {!forceReadOnly && quiz.status === 'CLOSED' && (
            <Alert
              type="info"
              showIcon
              message='Quiz đã đóng nên ở chế độ chỉ đọc. Bấm "Tạo bản nháp mới" nếu bạn cần thay đổi câu hỏi.'
            />
          )}

          {!forceReadOnly && quiz.status === 'DRAFT' && saveUncertain && (
            <Alert
              type="warning"
              showIcon
              message="Chưa xác nhận được kết quả lưu nháp"
              description='Kết nối bị gián đoạn hoặc máy chủ chưa phản hồi. Trình soạn thảo tạm khóa để bảo vệ dữ liệu — bấm "Thử lưu lại" để gửi lại đúng lần lưu trước mà không tạo trùng.'
            />
          )}

          {!forceReadOnly && quiz.status === 'DRAFT' && !saveUncertain && isSaving && (
            <Alert
              type="info"
              showIcon
              message="Đang lưu bản nháp quiz…"
              description="Trình soạn thảo tạm khóa trong lúc hệ thống xác nhận dữ liệu."
            />
          )}

          {serverError && (
            <Alert
              type="error"
              showIcon
              closable
              onClose={() => setServerError(null)}
              message="Không thể lưu / xuất bản quiz"
              description={serverError}
            />
          )}

          {cloneRecovery && (
            <Alert
              type="error"
              showIcon
              message={
                cloneRecovery.phase === 'copy_failed'
                  ? 'Đã tạo bản nháp nhưng chưa sao chép đủ câu hỏi'
                  : 'Chưa xác nhận được kết quả tạo bản nháp mới'
              }
              description={
                <Space orientation="vertical" size={8} style={{ width: '100%' }}>
                  <span>{cloneRecovery.errorMessage}</span>
                  <Space wrap>
                    {cloneRecovery.targetQuizId && (
                      <>
                        <Button
                          type="primary"
                          size="small"
                          loading={cloneMutation.isPending}
                          onClick={() => cloneMutation.mutate()}
                        >
                          Thử sao chép lại nội dung
                        </Button>
                        <Button
                          size="small"
                          onClick={() => navigate(paths.quizEditor(cloneRecovery.targetQuizId!))}
                        >
                          Mở bản nháp đã tạo
                        </Button>
                      </>
                    )}
                    {!cloneRecovery.targetQuizId && (
                      <Button
                        size="small"
                        onClick={() => navigate(paths.teachingLessonQuizzes(quiz.lessonId))}
                      >
                        Xem danh sách quiz của bài
                      </Button>
                    )}
                  </Space>
                </Space>
              }
            />
          )}

          {shouldShowSummary && (
            <Alert
              type="error"
              showIcon
              message={`Vui lòng sửa ${validation.issues.length} lỗi trước khi lưu hoặc xuất bản`}
              description={
                <ul style={{ margin: '4px 0 0', paddingLeft: 18 }}>
                  {validation.issues.map((issue) => (
                    <li key={issue.key}>
                      <Button
                        type="link"
                        size="small"
                        style={{ padding: 0, height: 'auto', textAlign: 'left', whiteSpace: 'normal' }}
                        onClick={() => focusTargetById(issue.targetId)}
                      >
                        {issue.message}
                      </Button>
                    </li>
                  ))}
                </ul>
              }
            />
          )}

          <Card title="Thông tin quiz">
            <label htmlFor="quiz-title-input" style={{ fontWeight: 600, display: 'block' }}>
              Tên bài kiểm tra (Quiz) <span style={{ color: token.colorError }}>*</span>
            </label>
            <Input
              id="quiz-title-input"
              value={title}
              disabled={isInputDisabled}
              status={
                (showValidationSummary || isDirty) && validation.titleError ? 'error' : undefined
              }
              aria-invalid={
                ((showValidationSummary || isDirty) && Boolean(validation.titleError)) || undefined
              }
              aria-describedby={validation.titleError ? 'quiz-title-error' : undefined}
              onChange={(e) => {
                setTitle(e.target.value)
                markEdited()
              }}
              placeholder="Nhập tên quiz…"
              maxLength={MAX_QUIZ_TITLE_LENGTH}
              showCount
              style={{ marginTop: 8 }}
            />
            {(showValidationSummary || isDirty) && validation.titleError && (
              <Typography.Text
                id="quiz-title-error"
                type="danger"
                style={{ display: 'block', marginTop: 6, fontSize: 13 }}
              >
                {validation.titleError}
              </Typography.Text>
            )}
          </Card>

          {/* Danh sách câu hỏi */}
          {questions.map((q, qIndex) => {
            const qVal = validation.questions[qIndex]
            const showContentErr =
              (showValidationSummary || isDirty) && (qVal.contentBlank || qVal.contentTooLong)
            const contentErrorId = `q-${qIndex}-content-error`
            const explanationErrorId = `q-${qIndex}-explanation-error`
            const hasQuestionError =
              qVal.duplicateGroups.length > 0 || (showValidationSummary && !qVal.isValid)

            return (
              <Card
                key={q.id || `new-${qIndex}`}
                style={hasQuestionError ? { borderColor: token.colorErrorBorder } : undefined}
                title={
                  <Space wrap>
                    <span style={{ fontSize: 16, fontWeight: 700 }}>Câu {qIndex + 1}</span>
                    {q.generatedByAi && (
                      <Tag color="purple" icon={<ThunderboltOutlined />}>
                        AI tạo
                      </Tag>
                    )}
                  </Space>
                }
                extra={
                  <Space>
                    {!isInputDisabled && (
                      <>
                        <Tooltip title="Chuyển lên">
                          <Button
                            type="text"
                            aria-label={`Chuyển câu ${qIndex + 1} lên`}
                            icon={<ArrowUpOutlined />}
                            disabled={qIndex === 0}
                            onClick={() => handleMoveQuestion(qIndex, 'UP')}
                          />
                        </Tooltip>
                        <Tooltip title="Chuyển xuống">
                          <Button
                            type="text"
                            aria-label={`Chuyển câu ${qIndex + 1} xuống`}
                            icon={<ArrowDownOutlined />}
                            disabled={qIndex === questions.length - 1}
                            onClick={() => handleMoveQuestion(qIndex, 'DOWN')}
                          />
                        </Tooltip>
                        <Button
                          type="text"
                          danger
                          icon={<DeleteOutlined />}
                          disabled={questions.length <= 1}
                          onClick={() => handleDeleteQuestion(qIndex)}
                        >
                          Xóa câu
                        </Button>
                      </>
                    )}
                  </Space>
                }
              >
                <Space orientation="vertical" size={16} style={{ width: '100%' }}>
                  <div>
                    <label htmlFor={`q-${qIndex}-content`} style={{ fontWeight: 600, display: 'block' }}>
                      Nội dung câu hỏi <span style={{ color: token.colorError }}>*</span>
                    </label>
                    <Input.TextArea
                      id={`q-${qIndex}-content`}
                      value={q.content}
                      disabled={isInputDisabled}
                      status={showContentErr ? 'error' : undefined}
                      aria-invalid={showContentErr || undefined}
                      aria-describedby={showContentErr ? contentErrorId : undefined}
                      placeholder="Nhập nội dung câu hỏi…"
                      autoSize={{ minRows: 2, maxRows: 5 }}
                      onChange={(e) =>
                        handleQuestionContentChange(qIndex, e.target.value)
                      }
                      style={{ marginTop: 6 }}
                    />
                    {showContentErr && (
                      <Typography.Text
                        id={contentErrorId}
                        type="danger"
                        style={{ display: 'block', marginTop: 4, fontSize: 13 }}
                      >
                        {qVal.contentBlank
                          ? `Câu ${qIndex + 1}: vui lòng nhập nội dung câu hỏi.`
                          : `Câu ${qIndex + 1}: nội dung câu hỏi tối đa ${MAX_QUESTION_CONTENT_LENGTH} ký tự.`}
                      </Typography.Text>
                    )}
                  </div>

                  <div>
                    <div
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        flexWrap: 'wrap',
                        gap: 8,
                      }}
                    >
                      <Typography.Text strong>
                        Các lựa chọn đáp án <span style={{ color: token.colorError }}>*</span>
                      </Typography.Text>
                      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                        (Chọn nút tròn để đánh dấu đáp án đúng)
                      </Typography.Text>
                    </div>
                    <div style={{ marginTop: 8 }}>
                      <Radio.Group
                        style={{ width: '100%' }}
                        value={q.options.findIndex((o) => o.isCorrect)}
                        onChange={(e) =>
                          handleSetCorrectOption(qIndex, e.target.value)
                        }
                        disabled={isInputDisabled}
                      >
                        <Space orientation="vertical" size={10} style={{ width: '100%' }}>
                          {q.options.map((opt, oIndex) => {
                            const isCorrect = opt.isCorrect
                            const isBlank =
                              showValidationSummary && qVal.blankOptionIndices.includes(oIndex)
                            const isTooLong = qVal.tooLongOptionIndices.includes(oIndex)
                            const dupGroup = qVal.duplicateGroups.find((g) =>
                              g.optionIndices.includes(oIndex),
                            )
                            const optError = isBlank
                              ? `Câu ${qIndex + 1}: đáp án ${optionLabel(oIndex)} không được để trống.`
                              : isTooLong
                                ? `Câu ${qIndex + 1}: đáp án ${optionLabel(oIndex)} tối đa ${MAX_OPTION_CONTENT_LENGTH} ký tự.`
                                : dupGroup
                                  ? formatDuplicateGroupMessage(dupGroup, qIndex)
                                  : null
                            const optErrorId = `q-${qIndex}-opt-${oIndex}-error`

                            return (
                              <div key={opt.id || `opt-${oIndex}`}>
                                <div
                                  onClick={() => !isInputDisabled && handleSetCorrectOption(qIndex, oIndex)}
                                  style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: 12,
                                    padding: '8px 12px',
                                    borderRadius: 8,
                                    border: `1.5px solid ${
                                      optError
                                        ? token.colorErrorBorder
                                        : isCorrect
                                          ? token.colorSuccessBorder
                                          : token.colorBorderSecondary
                                    }`,
                                    background: optError
                                      ? token.colorErrorBg
                                      : isCorrect
                                        ? token.colorSuccessBg
                                        : token.colorFillAlter,
                                    cursor: isInputDisabled ? 'default' : 'pointer',
                                    transition: 'all 0.2s',
                                  }}
                                >
                                  <Radio
                                    value={oIndex}
                                    aria-label={`Câu ${qIndex + 1}, chọn đáp án ${optionLabel(oIndex)} là đúng`}
                                  />
                                  <span style={{ fontWeight: 600, width: 24 }}>
                                    {optionLabel(oIndex)}.
                                  </span>
                                  <Input
                                    id={`q-${qIndex}-opt-${oIndex}`}
                                    value={opt.content}
                                    disabled={isInputDisabled}
                                    status={optError ? 'error' : undefined}
                                    aria-invalid={Boolean(optError) || undefined}
                                    aria-describedby={optError ? optErrorId : undefined}
                                    aria-label={`Câu ${qIndex + 1}, đáp án ${optionLabel(oIndex)}`}
                                    placeholder={`Nhập lựa chọn ${optionLabel(oIndex)}…`}
                                    style={{ flex: 1, minWidth: 0 }}
                                    onClick={(e) => e.stopPropagation()}
                                    onChange={(e) =>
                                      handleOptionContentChange(
                                        qIndex,
                                        oIndex,
                                        e.target.value,
                                      )
                                    }
                                  />
                                  {!isInputDisabled && (
                                    <Tooltip title="Xóa lựa chọn này">
                                      <Button
                                        type="text"
                                        danger
                                        aria-label={`Xóa đáp án ${optionLabel(oIndex)} câu ${qIndex + 1}`}
                                        icon={<DeleteOutlined />}
                                        disabled={q.options.length <= 2}
                                        onClick={(e) => {
                                          e.stopPropagation()
                                          handleDeleteOption(qIndex, oIndex)
                                        }}
                                      />
                                    </Tooltip>
                                  )}
                                </div>
                                {optError && (
                                  <Typography.Text
                                    id={optErrorId}
                                    type="danger"
                                    style={{ display: 'block', marginLeft: 40, marginTop: 4, fontSize: 12 }}
                                  >
                                    {optError}
                                  </Typography.Text>
                                )}
                              </div>
                            )
                          })}
                        </Space>
                      </Radio.Group>
                    </div>

                    {!isInputDisabled && q.options.length < 6 && (
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
                    <label
                      htmlFor={`q-${qIndex}-explanation`}
                      style={{ color: token.colorTextSecondary, display: 'block' }}
                    >
                      Giải thích (hiển thị cho học viên sau khi nộp bài)
                    </label>
                    <Input.TextArea
                      id={`q-${qIndex}-explanation`}
                      value={q.explanation ?? ''}
                      disabled={isInputDisabled}
                      status={qVal.explanationTooLong ? 'error' : undefined}
                      aria-invalid={qVal.explanationTooLong || undefined}
                      aria-describedby={qVal.explanationTooLong ? explanationErrorId : undefined}
                      placeholder="Giải thích vì sao đáp án này đúng…"
                      autoSize={{ minRows: 2, maxRows: 4 }}
                      onChange={(e) =>
                        handleExplanationChange(qIndex, e.target.value)
                      }
                      style={{ marginTop: 6 }}
                    />
                    {qVal.explanationTooLong && (
                      <Typography.Text
                        id={explanationErrorId}
                        type="danger"
                        style={{ display: 'block', marginTop: 4, fontSize: 12 }}
                      >
                        Câu {qIndex + 1}: giải thích tối đa {MAX_EXPLANATION_LENGTH} ký tự.
                      </Typography.Text>
                    )}
                  </div>
                </Space>
              </Card>
            )
          })}

          {!isInputDisabled && (
            <Button
              id="quiz-add-question-btn"
              type="dashed"
              icon={<PlusOutlined />}
              onClick={handleAddQuestion}
              style={{ width: '100%', height: 48, fontSize: 15 }}
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

              {!forceReadOnly && (quiz.status === 'DRAFT' ? (
                <Space orientation="vertical" size={10} style={{ width: '100%' }}>
                  <Button
                    type="default"
                    icon={<SaveOutlined />}
                    block
                    loading={saveMutation.isPending}
                    disabled={isBusy}
                    onClick={() => saveMutation.mutate()}
                  >
                    {saveUncertain ? 'Thử lưu lại' : 'Lưu nháp'}
                  </Button>
                  <Button
                    type="primary"
                    block
                    disabled={!validation.canPublish || isBusy || saveUncertain}
                    loading={publishMutation.isPending}
                    onClick={handleConfirmPublish}
                  >
                    Xuất bản quiz
                  </Button>
                </Space>
              ) : (
                <Space orientation="vertical" size={8} style={{ width: '100%' }}>
                  <Button
                    type="primary"
                    block
                    loading={cloneMutation.isPending}
                    disabled={cloneRecovery?.phase === 'create_uncertain'}
                    onClick={() => cloneMutation.mutate()}
                  >
                    {cloneRecovery?.targetQuizId ? 'Thử sao chép lại nội dung' : 'Tạo bản nháp mới'}
                  </Button>
                  {cloneRecovery?.targetQuizId && (
                    <Button
                      block
                      onClick={() => navigate(paths.quizEditor(cloneRecovery.targetQuizId!))}
                    >
                      Mở bản nháp đã tạo
                    </Button>
                  )}
                </Space>
              ))}
            </Space>
          </Card>

          {quiz.status === 'DRAFT' && (
            <Card title="Điều kiện xuất bản">
              <Space orientation="vertical" size={12} style={{ width: '100%' }}>
                <CheckItem
                  passed={validation.validTitle}
                  text="Tên quiz hợp lệ (1–200 ký tự)"
                  details={validation.titleError ?? undefined}
                />
                <CheckItem
                  passed={validation.hasQuestions}
                  text="Có ít nhất 1 câu hỏi"
                />
                <CheckItem
                  passed={validation.validOptionCount}
                  text="Mỗi câu có từ 2 đến 6 đáp án"
                  details={
                    validation.invalidOptionCounts.length > 0
                      ? `Câu chưa đúng: Câu ${validation.invalidOptionCounts.join(', ')}`
                      : undefined
                  }
                />
                <CheckItem
                  passed={validation.exactlyOneCorrect}
                  text="Mỗi câu có đúng 1 đáp án đúng"
                  details={
                    validation.invalidCorrectAnswers.length > 0
                      ? `Câu chưa có đáp án đúng: Câu ${validation.invalidCorrectAnswers.join(', ')}`
                      : undefined
                  }
                />
                <CheckItem
                  passed={validation.allContentFilled && validation.allLengthsValid}
                  text="Câu hỏi và đáp án không để trống và đúng độ dài"
                  details={
                    validation.emptyContents.length > 0
                      ? `Câu còn để trống: Câu ${validation.emptyContents.join(', ')}`
                      : validation.lengthErrorQuestions.length > 0
                        ? `Câu vượt quá độ dài: Câu ${validation.lengthErrorQuestions.join(', ')}`
                        : undefined
                  }
                />
                <CheckItem
                  passed={validation.noDuplicateOptions}
                  text="Các đáp án trong cùng câu không trùng nội dung"
                  details={
                    validation.duplicateSummaries.length > 0
                      ? validation.duplicateSummaries.join('; ')
                      : undefined
                  }
                />
              </Space>
            </Card>
          )}
        </Space>
      </Col>
    </Row>
  )
}

function CheckItem({
  passed,
  text,
  details,
}: {
  passed: boolean
  text: string
  details?: string
}) {
  const { token } = theme.useToken()
  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        {passed ? (
          <CheckCircleFilled style={{ color: token.colorSuccess, fontSize: 16, flexShrink: 0 }} />
        ) : (
          <CloseCircleFilled style={{ color: token.colorError, fontSize: 16, flexShrink: 0 }} />
        )}
        <span style={{ color: passed ? token.colorText : token.colorTextSecondary }}>{text}</span>
      </div>
      {!passed && details && (
        <div
          style={{
            marginLeft: 24,
            marginTop: 4,
            fontSize: 12,
            color: token.colorErrorText,
            overflowWrap: 'anywhere',
          }}
        >
          {details}
        </div>
      )}
    </div>
  )
}
