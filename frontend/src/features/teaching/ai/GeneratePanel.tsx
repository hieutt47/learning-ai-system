import { useEffect, useRef, useState } from 'react'
import {
  Alert, Button, Card, Form, InputNumber, Progress, Radio, Select, Space, Typography,
} from 'antd'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { isApiError } from '../../../shared/api/client'
import { Icon } from '../../../shared/components/Icon'
import { paths } from '../../../shared/routing/paths'
import {
  generateCards, generateQuiz, getDocuments,
  type GeneratedCards, type GeneratedQuiz, type GenerationRequest,
} from './api'
import { cardsDraft, quizDraft, writeDraft } from './draft'
import { aiErrorText } from './errors'
import { AiButton } from './AiButton'

interface GeneratePanelProps {
  lessonId: string
  onManualCard: () => void
}

export function GeneratePanel({ lessonId, onManualCard }: GeneratePanelProps) {
  const navigate = useNavigate()
  const controller = useRef<AbortController | null>(null)
  const [kind, setKind] = useState<'quiz' | 'flashcard'>('quiz')
  const [source, setSource] = useState<'TRANSCRIPT' | 'DOCUMENT'>('TRANSCRIPT')
  const [documentId, setDocumentId] = useState<string>()
  const [count, setCount] = useState(5)
  const [difficulty, setDifficulty] = useState<'EASY' | 'MEDIUM' | 'HARD'>('MEDIUM')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const documents = useQuery({
    queryKey: ['lesson-documents', lessonId],
    queryFn: () => getDocuments(lessonId),
    retry: false,
  })
  const generateMutation = useMutation<GeneratedQuiz | GeneratedCards, Error, GenerationRequest>({
    mutationFn: request => {
      const nextController = new AbortController()
      controller.current = nextController
      return kind === 'quiz'
        ? generateQuiz(request, nextController.signal)
        : generateCards(request, nextController.signal)
    },
    retry: false,
    onSuccess: (result, request) => {
      if (controller.current?.signal.aborted) return
      const nextDraft = 'questions' in result.candidate
        ? quizDraft(result as GeneratedQuiz)
        : cardsDraft(result as GeneratedCards)
      writeDraft(lessonId, { ...nextDraft, request })
      void navigate(paths.teachingLessonAiPreview(lessonId))
    },
    onError: error => {
      if (isApiError(error) && error.status === 400) {
        setFieldErrors(error.fieldErrors())
      }
    },
  })
  const runGeneration = () => {
    if (!Number.isInteger(count) || count < 1 || count > 10) {
      setFieldErrors({ count: 'Số lượng phải từ 1 đến 10' })
      return
    }
    if (source === 'DOCUMENT' && !documentId) {
      setFieldErrors({ document_id: 'Chọn tài liệu trước khi sinh bản nháp' })
      return
    }
    setFieldErrors({})
    generateMutation.mutate({
      lesson_id: lessonId,
      source_type: source,
      ...(source === 'DOCUMENT' ? { document_id: documentId } : {}),
      count,
      ...(kind === 'quiz' ? { difficulty } : {}),
    })
  }
  const cancel = () => {
    // AbortController ngăn phản hồi AI đến muộn mở trang xem trước sau khi Hủy.
    controller.current?.abort()
    generateMutation.reset()
  }
  useEffect(() => () => { controller.current?.abort() }, [])

  if (generateMutation.isPending) {
    return (
      <Card>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Typography.Title level={4} style={{ color: 'var(--ai)', margin: 0 }}>
            AI đang soạn {kind === 'quiz' ? 'quiz' : 'flashcard'}…
          </Typography.Title>
          <Progress percent={35} status="active" showInfo={false} strokeColor="var(--ai)" />
          <Typography.Text type="secondary">
            Có thể mất tới 30 giây.
          </Typography.Text>
          <Button onClick={cancel}>
            Hủy
          </Button>
        </Space>
      </Card>
    )
  }

  return (
    <Card
      title={(
        <span style={{ color: 'var(--ai)' }}>
          Sinh bằng AI
        </span>
      )}
      extra={(
        <span style={{ color: 'var(--ai)' }}>
          <Icon name="spark" />
        </span>
      )}
    >
      <Typography.Paragraph type="secondary">
        AI chỉ tạo bản nháp. Bạn xem lại, sửa rồi mới lưu và xuất bản.
      </Typography.Paragraph>
      {generateMutation.isError && (
        <Alert
          type="error"
          showIcon
          message="Không sinh được nội dung"
          description={aiErrorText(generateMutation.error)}
          action={(
            <Button onClick={runGeneration}>
              Thử lại
            </Button>
          )}
          style={{ marginBottom: 16 }}
        />
      )}
      <Form layout="vertical" onFinish={runGeneration}>
        <Form.Item label="Loại nội dung">
          <Radio.Group
            value={kind}
            onChange={event => setKind(event.target.value)}
            options={[
              { label: 'Quiz', value: 'quiz' },
              { label: 'Flashcard', value: 'flashcard' },
            ]}
            optionType="button"
            buttonStyle="solid"
          />
        </Form.Item>
        <Form.Item
          label="Nguồn"
          validateStatus={fieldErrors.source_type || fieldErrors.document_id ? 'error' : undefined}
          help={fieldErrors.source_type || fieldErrors.document_id}
        >
          <Space wrap>
            <Radio.Group
              value={source}
              onChange={event => setSource(event.target.value)}
              options={[
                { label: 'Transcript bài học', value: 'TRANSCRIPT' },
                {
                  label: 'Tài liệu',
                  value: 'DOCUMENT',
                  disabled: documents.isError || !documents.data?.items.length,
                },
              ]}
            />
            <Select
              style={{ minWidth: 230 }}
              disabled={source !== 'DOCUMENT'}
              placeholder="Chọn tài liệu"
              value={documentId}
              onChange={setDocumentId}
              options={documents.data?.items.map(document => ({
                label: document.original_name,
                value: document.id,
              }))}
            />
          </Space>
          {documents.isError && (
            <Typography.Text type="secondary" style={{ display: 'block' }}>
              Không tải được danh sách tài liệu. Bạn vẫn có thể dùng Transcript.
            </Typography.Text>
          )}
          {documents.isSuccess && !documents.data.items.length && (
            <Typography.Text type="secondary" style={{ display: 'block' }}>
              Bài học chưa có tài liệu. Bạn có thể dùng Transcript.
            </Typography.Text>
          )}
        </Form.Item>
        <Form.Item
          label="Số lượng câu"
          extra="Từ 1 đến 10"
          validateStatus={fieldErrors.count ? 'error' : undefined}
          help={fieldErrors.count}
        >
          <InputNumber min={1} max={10} value={count} onChange={value => setCount(value ?? 0)} />
        </Form.Item>
        {kind === 'quiz' && (
          <Form.Item
            label="Độ khó"
            validateStatus={fieldErrors.difficulty ? 'error' : undefined}
            help={fieldErrors.difficulty}
          >
            <Radio.Group
              value={difficulty}
              onChange={event => setDifficulty(event.target.value)}
              options={[
                { label: 'Dễ', value: 'EASY' },
                { label: 'Trung bình', value: 'MEDIUM' },
                { label: 'Khó', value: 'HARD' },
              ]}
              optionType="button"
            />
          </Form.Item>
        )}
        <Typography.Paragraph type="secondary">
          Kết quả hiện ở màn xem trước, chưa lưu cho đến khi bạn bấm “Lưu thành bản nháp”.
        </Typography.Paragraph>
        <Space>
          <AiButton
            htmlType="submit"
            disabled={source === 'DOCUMENT' && !documentId}
            icon={<Icon name="spark" />}
          >
            Sinh bản nháp
          </AiButton>
          <Typography.Link
            onClick={() => {
              if (kind === 'quiz') void navigate(paths.teachingLessonQuizzes(lessonId))
              else onManualCard()
            }}
          >
            Soạn thủ công
          </Typography.Link>
        </Space>
      </Form>
    </Card>
  )
}
