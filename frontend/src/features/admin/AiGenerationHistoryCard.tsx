import { useState } from 'react'
import { Alert, Button, Card, Descriptions, List, Modal, Select, Space, Table, Tag, Typography, type TableProps } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { Icon } from '../../shared/components/Icon'
import { EmptyState, ErrorState, LoadingState } from '../../shared/components/PageState'
import { formatDateTime } from '../marketplace/format'
import {
  adminAiKeys,
  listLessonAiGenerations,
  parseCandidatePreview,
  type AdminAiGeneration,
  type AiGenerationStatus,
  type AiGenerationType,
} from './ai.api'

const DIFFICULTY_LABEL: Record<string, string> = {
  EASY: 'Dễ',
  MEDIUM: 'Trung bình',
  HARD: 'Khó',
}

function GenerationDetailModal({
  generation,
  onClose,
}: {
  generation: AdminAiGeneration | null
  onClose: () => void
}) {
  const preview = generation ? parseCandidatePreview(generation.candidate_json) : null
  const optionLabel = (index: number) => String.fromCharCode(65 + index)

  return (
    <Modal
      open={Boolean(generation)}
      onCancel={onClose}
      footer={<Button onClick={onClose}>Đóng</Button>}
      width={760}
      title={
        generation ? (
          <Space>
            <Tag bordered={false} style={{ background: 'var(--ai-soft)', color: 'var(--ai-text)' }}>
              <Icon name="spark" /> {generation.generation_type}
            </Tag>
            <span>Chi tiết lượt sinh #{generation.id}</span>
          </Space>
        ) : null
      }
    >
      {generation && (
        <Space orientation="vertical" size={16} style={{ width: '100%' }}>
          <Descriptions size="small" bordered column={{ xs: 1, sm: 2 }}>
            <Descriptions.Item label="Khóa học">{generation.course_title}</Descriptions.Item>
            <Descriptions.Item label="Bài học">{generation.lesson_title}</Descriptions.Item>
            <Descriptions.Item label="Nguồn">
              {generation.source_type === 'DOCUMENT'
                ? `Tài liệu (${generation.source_document_name || `#${generation.source_document_id}`})`
                : 'Transcript'}
            </Descriptions.Item>
            <Descriptions.Item label="Số lượng / Độ khó">
              {generation.requested_count}
              {generation.difficulty ? ` · ${DIFFICULTY_LABEL[generation.difficulty] ?? generation.difficulty}` : ''}
            </Descriptions.Item>
            <Descriptions.Item label="Model">{`${generation.provider} / ${generation.model}`}</Descriptions.Item>
            <Descriptions.Item label="Prompt">{generation.prompt_version}</Descriptions.Item>
            <Descriptions.Item label="Người tạo">{generation.creator_name}</Descriptions.Item>
            <Descriptions.Item label="Thời gian">{formatDateTime(generation.created_at)}</Descriptions.Item>
          </Descriptions>

          {generation.status === 'FAILED' ? (
            <Alert
              type="error"
              showIcon
              message={`Lượt sinh thất bại${generation.error_code ? `: ${generation.error_code}` : ''}`}
            />
          ) : preview?.questions ? (
            <List
              size="small"
              dataSource={preview.questions}
              renderItem={(question, index) => (
                <List.Item style={{ display: 'block', paddingInline: 0 }}>
                  <Typography.Paragraph strong style={{ marginBottom: 6 }}>
                    Câu {index + 1}: {question.content}
                  </Typography.Paragraph>
                  <Space orientation="vertical" size={4} style={{ width: '100%', paddingLeft: 12 }}>
                    {question.options.map((opt, optIdx) => (
                      <Typography.Text
                        key={optIdx}
                        type={opt.is_correct ? 'success' : undefined}
                        strong={opt.is_correct}
                      >
                        {optionLabel(optIdx)}. {opt.content} {opt.is_correct ? '(Đúng)' : ''}
                      </Typography.Text>
                    ))}
                  </Space>
                  {question.explanation && (
                    <Typography.Paragraph type="secondary" style={{ marginTop: 6, marginBottom: 0, paddingLeft: 12 }}>
                      Giải thích: {question.explanation}
                    </Typography.Paragraph>
                  )}
                </List.Item>
              )}
            />
          ) : preview?.flashcards ? (
            <List
              size="small"
              dataSource={preview.flashcards}
              renderItem={(card, index) => (
                <List.Item style={{ display: 'block', paddingInline: 0 }}>
                  <Typography.Paragraph strong style={{ marginBottom: 4 }}>
                    Thẻ {index + 1}: {card.front_text}
                  </Typography.Paragraph>
                  <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
                    {card.back_text}
                  </Typography.Paragraph>
                </List.Item>
              )}
            />
          ) : (
            <EmptyState description="Không có dữ liệu xem trước cho lượt sinh này." />
          )}
        </Space>
      )}
    </Modal>
  )
}

interface AiGenerationHistoryCardProps {
  lessonId: string
}

/** Bảng lịch sử các lần sinh AI (Quiz / Flashcard) của một bài học cho Admin xem chỉ đọc. */
export function AiGenerationHistoryCard({ lessonId }: AiGenerationHistoryCardProps) {
  const [generationType, setGenerationType] = useState<AiGenerationType | undefined>(undefined)
  const [status, setStatus] = useState<AiGenerationStatus | undefined>(undefined)
  const [page, setPage] = useState(1)
  const [selected, setSelected] = useState<AdminAiGeneration | null>(null)
  const pageSize = 10

  const params = { generation_type: generationType, status, page, page_size: pageSize }
  const query = useQuery({
    queryKey: adminAiKeys.lessonGenerations(lessonId, params),
    queryFn: () => listLessonAiGenerations(lessonId, params),
    enabled: Boolean(lessonId),
  })

  const items = query.data?.items ?? []
  const total = query.data?.total ?? 0

  const columns: TableProps<AdminAiGeneration>['columns'] = [
    {
      title: 'Loại',
      dataIndex: 'generation_type',
      width: 120,
      render: (value: AiGenerationType, row) => (
        <Space orientation="vertical" size={2}>
          <Tag bordered={false} style={{ background: 'var(--ai-soft)', color: 'var(--ai-text)' }}>
            <Icon name="spark" /> {value}
          </Tag>
          {row.difficulty && (
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              {DIFFICULTY_LABEL[row.difficulty] ?? row.difficulty}
            </Typography.Text>
          )}
        </Space>
      ),
    },
    {
      title: 'Nguồn',
      dataIndex: 'source_type',
      width: 180,
      render: (value: AdminAiGeneration['source_type'], row) =>
        value === 'DOCUMENT'
          ? `Tài liệu: ${row.source_document_name || `#${row.source_document_id}`}`
          : 'Transcript',
    },
    {
      title: 'SL',
      dataIndex: 'requested_count',
      width: 64,
      align: 'center',
    },
    {
      title: 'Model / Prompt',
      key: 'model',
      width: 190,
      render: (_, row) => (
        <div>
          <div style={{ fontSize: 13 }}>{row.model}</div>
          <div style={{ fontSize: 12, color: 'var(--text-2, rgba(0,0,0,0.55))' }}>
            {row.provider} · {row.prompt_version}
          </div>
        </div>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      width: 130,
      render: (value: AiGenerationStatus, row) => (
        <Space orientation="vertical" size={2}>
          <Tag color={value === 'SUCCEEDED' ? 'green' : 'red'}>{value}</Tag>
          {row.error_code && (
            <Typography.Text type="danger" style={{ fontSize: 12 }}>
              {row.error_code}
            </Typography.Text>
          )}
        </Space>
      ),
    },
    {
      title: 'Người tạo',
      dataIndex: 'creator_name',
      width: 140,
    },
    {
      title: 'Thời gian',
      dataIndex: 'created_at',
      width: 145,
      render: (value: string) => formatDateTime(value),
    },
    {
      title: 'Hành động',
      key: 'action',
      width: 120,
      render: (_, row) => (
        <Button size="small" icon={<Icon name="eye" />} onClick={() => setSelected(row)}>
          Xem kết quả
        </Button>
      ),
    },
  ]

  return (
    <>
      <Card
        title={
          <Space>
            <span>Lịch sử sinh AI</span>
            <Typography.Text type="secondary" style={{ fontWeight: 400 }}>
              {total}
            </Typography.Text>
          </Space>
        }
        extra={
          <Space wrap>
            <Select
              allowClear
              placeholder="Loại nội dung"
              style={{ width: 145 }}
              value={generationType}
              onChange={(value) => {
                setGenerationType(value)
                setPage(1)
              }}
              options={[
                { value: 'QUIZ', label: 'Quiz' },
                { value: 'FLASHCARD', label: 'Flashcard' },
              ]}
            />
            <Select
              allowClear
              placeholder="Trạng thái"
              style={{ width: 145 }}
              value={status}
              onChange={(value) => {
                setStatus(value)
                setPage(1)
              }}
              options={[
                { value: 'SUCCEEDED', label: 'Thành công' },
                { value: 'FAILED', label: 'Thất bại' },
              ]}
            />
          </Space>
        }
      >
        {query.isLoading ? (
          <LoadingState tip="Đang tải lịch sử sinh AI..." />
        ) : query.isError ? (
          <ErrorState error={query.error} onRetry={() => void query.refetch()} />
        ) : items.length === 0 ? (
          <EmptyState description="Bài học chưa có lượt sinh AI nào." />
        ) : (
          <Table<AdminAiGeneration>
            rowKey="id"
            columns={columns}
            dataSource={items}
            scroll={{ x: 900 }}
            pagination={{
              current: page,
              pageSize,
              total,
              showSizeChanger: false,
              onChange: setPage,
            }}
          />
        )}
      </Card>
      <GenerationDetailModal generation={selected} onClose={() => setSelected(null)} />
    </>
  )
}
