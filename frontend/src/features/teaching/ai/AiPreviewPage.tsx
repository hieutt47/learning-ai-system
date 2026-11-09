import { useCallback, useEffect, useRef, useState } from 'react'
import { Alert, App, Button, Card, Input, Space, Typography } from 'antd'
import { useMutation } from '@tanstack/react-query'
import { useBlocker, useNavigate, useParams } from 'react-router'
import { isApiError } from '../../../shared/api/client'
import { EmptyState } from '../../../shared/components/PageState'
import { Icon } from '../../../shared/components/Icon'
import { paths } from '../../../shared/routing/paths'
import {
  createCard, createQuestion, createQuiz, generateCards, generateQuiz,
  type GeneratedCards, type GeneratedQuiz, type QuizQuestion,
} from './api'
import { CardEditor, type CandidateCard } from './CardEditor'
import { cardsDraft, clearDraft, quizDraft, readDraft, writeDraft, type AiDraft } from './draft'
import { QuestionEditor } from './QuestionEditor'
import { questionErrors } from './validation'
import { AiButton } from './AiButton'

export function AiPreviewPage() {
  const { lessonId = '' } = useParams()
  const navigate = useNavigate()
  const { message, modal } = App.useApp()
  const [draft, setDraft] = useState<AiDraft | null>(() => readDraft(lessonId))
  const [saveError, setSaveError] = useState<string>()
  const [saveFields, setSaveFields] = useState<Record<string, string>>({})
  // Cho phép điều hướng sau khi đã lưu hoặc xác nhận bỏ bản nháp.
  const allowExit = useRef(false)
  const blocker = useBlocker(({ currentLocation, nextLocation }) =>
    !!draft && !allowExit.current && currentLocation.pathname !== nextLocation.pathname,
  )
  const confirmLeave = useCallback((onConfirmed: () => void, onCancelled?: () => void) => {
    modal.confirm({
      title: 'Rời trang xem trước?',
      content: 'Bản nháp AI chưa được lưu và sẽ mất nếu bạn rời trang.',
      okText: 'Rời trang',
      onOk: () => {
        clearDraft(lessonId)
        setDraft(null)
        allowExit.current = true
        onConfirmed()
      },
      onCancel: onCancelled,
    })
  }, [lessonId, modal])

  useEffect(() => {
    if (blocker.state !== 'blocked') return
    confirmLeave(() => blocker.proceed(), () => blocker.reset())
  }, [blocker, confirmLeave])

  useEffect(() => {
    if (!draft) return
    const onBeforeUnload = (event: BeforeUnloadEvent) => { event.preventDefault() }
    window.addEventListener('beforeunload', onBeforeUnload)
    return () => window.removeEventListener('beforeunload', onBeforeUnload)
  }, [draft])

  const changeDraft = (value: AiDraft) => {
    setDraft(value)
    writeDraft(lessonId, value)
    setSaveError(undefined)
    setSaveFields({})
  }
  const leave = () => confirmLeave(() => void navigate(paths.teachingLessonAi(lessonId)))
  const finish = (destination: string, notice: string) => {
    clearDraft(lessonId)
    setDraft(null)
    allowExit.current = true
    void message.success(notice)
    void navigate(destination)
  }

  const regenerateMutation = useMutation<GeneratedQuiz | GeneratedCards, Error, void>({
    mutationFn: async () => {
      if (!draft?.request) throw new Error('Không có thông tin nguồn để sinh lại.')
      return draft.kind === 'quiz'
        ? generateQuiz(draft.request)
        : generateCards(draft.request)
    },
    retry: false,
    onSuccess: result => {
      const nextDraft = 'questions' in result.candidate
        ? quizDraft(result as GeneratedQuiz)
        : cardsDraft(result as GeneratedCards)
      changeDraft({ ...nextDraft, request: draft?.request })
      void message.success('Đã sinh bản nháp mới')
    },
    onError: error => void message.error(error.message),
  })

  const saveDraftMutation = useMutation({
    mutationFn: async () => {
      if (!draft) return
      let current = draft
      if (current.kind === 'quiz') {
        let quizId = current.quiz_id
        if (!quizId) {
          const quiz = await createQuiz(lessonId, {
            title: current.title?.trim() || 'Quiz do AI tạo',
            generation_id: current.generation_id,
          })
          quizId = quiz.id
          current = { ...current, quiz_id: quizId }
          changeDraft(current)
        }
        for (let index = current.saved_count; index < current.items.length; index++) {
          const question = current.items[index]
          await createQuestion(quizId, {
            content: question.content.trim(),
            explanation: question.explanation?.trim() ?? '',
            order_index: index,
            options: question.options.map((option, order_index) => ({
              content: option.content.trim(),
              is_correct: option.is_correct,
              order_index,
            })),
            generation_id: current.generation_id,
          })
          current = { ...current, saved_count: index + 1 }
          changeDraft(current)
        }
        finish(paths.quizEditor(quizId), 'Đã lưu quiz nháp')
      } else {
        for (let index = current.saved_count; index < current.items.length; index++) {
          const card = current.items[index]
          await createCard(lessonId, {
            front_text: card.front_text.trim(),
            back_text: card.back_text.trim(),
            generation_id: current.generation_id,
          })
          current = { ...current, saved_count: index + 1 }
          changeDraft(current)
        }
        finish(paths.teachingLessonAi(lessonId), 'Đã lưu flashcard nháp')
      }
    },
    retry: false,
    onError: error => {
      const quizUnavailable = draft?.kind === 'quiz' && isApiError(error) && error.status === 404
      setSaveError(quizUnavailable
        ? 'Chưa lưu được quiz — chức năng quiz chưa sẵn sàng, thử lại sau.'
        : isApiError(error) ? error.message : 'Không lưu được. Hãy thử lại.')
      if (isApiError(error) && error.status === 400) setSaveFields(error.fieldErrors())
    },
  })

  if (!draft) {
    return (
      <EmptyState
        description="Chưa có kết quả AI để xem trước"
        action={(
          <Button onClick={() => void navigate(paths.teachingLessonAi(lessonId))}>
            Về tab AI & Flashcard
          </Button>
        )}
      />
    )
  }

  const invalid = draft.kind === 'quiz'
    ? draft.items.some(question => questionErrors(question).length > 0)
    : draft.items.some(card => !card.front_text.trim() || !card.back_text.trim())
  const updateQuestion = (index: number, patch: Partial<QuizQuestion>) => {
    if (draft.kind !== 'quiz') return
    changeDraft({
      ...draft,
      items: draft.items.map((item, offset) => offset === index ? { ...item, ...patch } : item),
    })
  }
  const updateCard = (index: number, patch: Partial<CandidateCard>) => {
    if (draft.kind !== 'flashcard') return
    changeDraft({
      ...draft,
      items: draft.items.map((item, offset) => offset === index ? { ...item, ...patch } : item),
    })
  }
  const removeItem = (index: number) => {
    if (index < draft.saved_count) return
    changeDraft({
      ...draft,
      items: draft.items.filter((_, offset) => offset !== index),
    } as AiDraft)
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Alert
        style={{ borderColor: 'var(--ai)', background: 'var(--ai-soft)' }}
        message={(
          <Typography.Text style={{ color: 'var(--ai)' }} strong>
            Bản nháp do AI tạo — chưa được lưu
          </Typography.Text>
        )}
        description="Hãy kiểm tra trước khi lưu."
        showIcon
        icon={<Icon name="spark" />}
      />
      {draft.source_truncated && (
        <Alert type="warning" showIcon message="AI chỉ đọc 20.000 ký tự đầu của nguồn" />
      )}
      {draft.saved_count > 0 && (
        <Alert
          type="info"
          message={`Đã lưu ${draft.saved_count} / ${draft.items.length} mục. Các mục đã lưu không thể sửa tại đây.`}
        />
      )}
      {draft.kind === 'quiz' && (
        <Card>
          <label htmlFor="quiz-title">
            Tên quiz
          </label>
          <Input
            id="quiz-title"
            maxLength={200}
            value={draft.title ?? ''}
            placeholder="Quiz do AI tạo"
            disabled={!!draft.quiz_id}
            onChange={event => changeDraft({ ...draft, title: event.target.value })}
          />
          {saveFields.title && (
            <Typography.Text type="danger">
              {saveFields.title}
            </Typography.Text>
          )}
        </Card>
      )}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: draft.kind === 'flashcard'
            ? 'repeat(auto-fit, minmax(280px, 1fr))'
            : '1fr',
          gap: 16,
        }}
      >
        {draft.kind === 'quiz'
          ? draft.items.map((question, index) => (
            <QuestionEditor
              key={index}
              question={question}
              index={index}
              saved={index < draft.saved_count}
              fieldErrors={saveFields}
              onChange={patch => updateQuestion(index, patch)}
              onRemove={() => removeItem(index)}
            />
          ))
          : draft.items.map((card, index) => (
            <CardEditor
              key={index}
              card={card}
              index={index}
              saved={index < draft.saved_count}
              fieldErrors={saveFields}
              onChange={patch => updateCard(index, patch)}
              onRemove={() => removeItem(index)}
            />
          ))}
      </div>
      {saveError && <Alert type="error" showIcon message={saveError} />}
      <Card>
        <Space wrap style={{ width: '100%', justifyContent: 'space-between' }}>
          <Typography.Text type={invalid ? 'danger' : 'secondary'}>
            {invalid
              ? 'Sửa hoặc xóa mục không hợp lệ để lưu'
              : draft.items.length ? 'Sẵn sàng lưu thành bản nháp' : 'Không còn mục nào để lưu'}
          </Typography.Text>
          <Space>
            <Button onClick={leave}>
              Hủy
            </Button>
            <Button
              icon={<Icon name="refresh" />}
              disabled={draft.saved_count > 0 || !draft.request}
              loading={regenerateMutation.isPending}
              onClick={() => regenerateMutation.mutate()}
            >
              Sinh lại
            </Button>
            <AiButton
              disabled={invalid || !draft.items.length}
              loading={saveDraftMutation.isPending}
              onClick={() => saveDraftMutation.mutate()}
            >
              {draft.saved_count ? 'Lưu tiếp' : 'Lưu thành bản nháp'}
            </AiButton>
          </Space>
        </Space>
      </Card>
    </Space>
  )
}
