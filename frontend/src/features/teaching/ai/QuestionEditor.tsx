import { Button, Card, Checkbox, Input, Space, Typography } from 'antd'
import { Icon } from '../../../shared/components/Icon'
import { formatDuplicateGroupMessage, optionLabel } from '../quizzes/quizValidation'
import type { QuizQuestion } from './api'
import { validateAiQuestion } from './validation'

interface QuestionEditorProps {
  question: QuizQuestion
  index: number
  saved: boolean
  fieldErrors: Record<string, string>
  onChange: (patch: Partial<QuizQuestion>) => void
  onRemove: () => void
}

export function QuestionEditor({
  question, index, saved, fieldErrors, onChange, onRemove,
}: QuestionEditorProps) {
  const result = validateAiQuestion(question, index)
  const errors = result.messages
  const contentInvalid = result.contentBlank || result.contentTooLong
  const contentErrorId = `ai-q-${index}-content-error`
  const explanationErrorId = `ai-q-${index}-explanation-error`

  const setCorrect = (correctIndex: number) => {
    onChange({
      options: question.options.map((option, offset) => ({
        ...option,
        is_correct: offset === correctIndex,
      })),
    })
  }
  const setOption = (optionIndex: number, content: string) => {
    onChange({
      options: question.options.map((option, offset) =>
        offset === optionIndex ? { ...option, content } : option,
      ),
    })
  }
  const removeOption = (optionIndex: number) => {
    onChange({ options: question.options.filter((_, offset) => offset !== optionIndex) })
  }

  return (
    <Card
      title={`Câu ${index + 1}`}
      extra={(
        <Button danger icon={<Icon name="trash" />} disabled={saved} onClick={onRemove}>
          Xóa
        </Button>
      )}
      style={errors.length ? { borderColor: 'var(--danger)' } : undefined}
    >
      <Space direction="vertical" style={{ width: '100%' }}>
        <label htmlFor={`ai-q-${index}-content`}>
          Nội dung câu hỏi
          <Input.TextArea
            id={`ai-q-${index}-content`}
            value={question.content}
            disabled={saved}
            status={contentInvalid ? 'error' : undefined}
            aria-invalid={contentInvalid || undefined}
            aria-describedby={contentInvalid ? contentErrorId : undefined}
            onChange={event => onChange({ content: event.target.value })}
          />
          {contentInvalid && (
            <Typography.Text id={contentErrorId} type="danger">
              {result.contentBlank ? 'Nhập nội dung câu hỏi' : 'Nội dung câu hỏi tối đa 4000 ký tự'}
            </Typography.Text>
          )}
          {fieldErrors.content && (
            <Typography.Text type="danger">
              {fieldErrors.content}
            </Typography.Text>
          )}
        </label>
        <Typography.Text strong>
          Đáp án
        </Typography.Text>
        {question.options.map((option, optionIndex) => {
          const isBlank = result.blankOptionIndices.includes(optionIndex)
          const isTooLong = result.tooLongOptionIndices.includes(optionIndex)
          const dupGroup = result.duplicateGroups.find((g) => g.optionIndices.includes(optionIndex))
          const optInvalid = isBlank || isTooLong || Boolean(dupGroup)
          const optErrorId = `ai-q-${index}-opt-${optionIndex}-error`
          const optErrorMsg = isBlank
            ? `Nhập nội dung cho đáp án ${optionLabel(optionIndex)}`
            : isTooLong
              ? `Đáp án ${optionLabel(optionIndex)} vượt quá 1000 ký tự`
              : dupGroup
                ? formatDuplicateGroupMessage(dupGroup, index)
                : null

          return (
            <div key={optionIndex} style={{ width: '100%' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, width: '100%' }}>
                <Checkbox
                  checked={option.is_correct}
                  disabled={saved}
                  onChange={() => setCorrect(optionIndex)}
                  aria-label={`Câu ${index + 1}, đáp án ${optionLabel(optionIndex)} đúng`}
                />
                <span style={{ fontWeight: 600, minWidth: 20 }}>
                  {optionLabel(optionIndex)}.
                </span>
                <Input
                  id={`ai-q-${index}-opt-${optionIndex}`}
                  value={option.content}
                  disabled={saved}
                  maxLength={1000}
                  status={optInvalid ? 'error' : undefined}
                  aria-invalid={optInvalid || undefined}
                  aria-describedby={optErrorMsg ? optErrorId : undefined}
                  aria-label={`Câu ${index + 1}, nội dung đáp án ${optionLabel(optionIndex)}`}
                  style={{ flex: 1, minWidth: 0 }}
                  onChange={event => setOption(optionIndex, event.target.value)}
                />
                <Button
                  disabled={saved || question.options.length <= 2}
                  onClick={() => removeOption(optionIndex)}
                >
                  Xóa
                </Button>
              </div>
              {optErrorMsg && (
                <Typography.Text
                  id={optErrorId}
                  type="danger"
                  style={{ display: 'block', marginLeft: 48, marginTop: 4, fontSize: 12 }}
                >
                  {optErrorMsg}
                </Typography.Text>
              )}
            </div>
          )
        })}
        {fieldErrors.options && (
          <Typography.Text type="danger">
            {fieldErrors.options}
          </Typography.Text>
        )}
        <Button
          disabled={saved || question.options.length >= 6}
          onClick={() => onChange({
            options: [...question.options, { content: '', is_correct: false }],
          })}
        >
          Thêm đáp án
        </Button>
        <label htmlFor={`ai-q-${index}-explanation`}>
          Giải thích
          <Input.TextArea
            id={`ai-q-${index}-explanation`}
            value={question.explanation ?? ''}
            disabled={saved}
            status={result.explanationTooLong ? 'error' : undefined}
            aria-invalid={result.explanationTooLong || undefined}
            aria-describedby={result.explanationTooLong ? explanationErrorId : undefined}
            onChange={event => onChange({ explanation: event.target.value })}
          />
          {result.explanationTooLong && (
            <Typography.Text id={explanationErrorId} type="danger">
              Giải thích tối đa 4000 ký tự
            </Typography.Text>
          )}
          {fieldErrors.explanation && (
            <Typography.Text type="danger">
              {fieldErrors.explanation}
            </Typography.Text>
          )}
        </label>
        {result.invalidOptionCount && (
          <Typography.Text type="danger">
            Cần từ 2 đến 6 đáp án
          </Typography.Text>
        )}
        {result.invalidCorrectCount && (
          <Typography.Text type="danger">
            Chọn đúng 1 đáp án đúng
          </Typography.Text>
        )}
      </Space>
    </Card>
  )
}
