import { Button, Card, Checkbox, Input, Space, Typography } from 'antd'
import { Icon } from '../../../shared/components/Icon'
import type { QuizQuestion } from './api'
import { questionErrors } from './validation'

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
  const errors = questionErrors(question)
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
        <label>
          Nội dung câu hỏi
          <Input.TextArea
            value={question.content}
            disabled={saved}
            onChange={event => onChange({ content: event.target.value })}
          />
          {fieldErrors.content && (
            <Typography.Text type="danger">
              {fieldErrors.content}
            </Typography.Text>
          )}
        </label>
        <Typography.Text strong>
          Đáp án
        </Typography.Text>
        {question.options.map((option, optionIndex) => (
          <Space key={optionIndex} style={{ width: '100%' }}>
            <Checkbox
              checked={option.is_correct}
              disabled={saved}
              onChange={() => setCorrect(optionIndex)}
              aria-label={`Đáp án ${optionIndex + 1} đúng`}
            />
            <Input
              value={option.content}
              disabled={saved}
              maxLength={1000}
              onChange={event => setOption(optionIndex, event.target.value)}
            />
            <Button
              disabled={saved || question.options.length <= 2}
              onClick={() => removeOption(optionIndex)}
            >
              Xóa
            </Button>
          </Space>
        ))}
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
        <label>
          Giải thích
          <Input.TextArea
            value={question.explanation ?? ''}
            disabled={saved}
            onChange={event => onChange({ explanation: event.target.value })}
          />
          {fieldErrors.explanation && (
            <Typography.Text type="danger">
              {fieldErrors.explanation}
            </Typography.Text>
          )}
        </label>
        {errors.map(error => (
          <Typography.Text key={error} type="danger">
            {error}
          </Typography.Text>
        ))}
      </Space>
    </Card>
  )
}
