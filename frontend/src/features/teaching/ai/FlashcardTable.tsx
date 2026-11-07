import { forwardRef, useImperativeHandle, useState } from 'react'
import { Alert, App, Button, Card, Form, Pagination, Space, Table, Tag, Typography } from 'antd'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { isApiError } from '../../../shared/api/client'
import { EmptyState, ErrorState, LoadingState } from '../../../shared/components/PageState'
import { Icon } from '../../../shared/components/Icon'
import {
  createCard, deleteCard, getFlashcards, publishCard, updateCard, type Flashcard,
} from './api'
import { aiErrorText } from './errors'
import { CardModal, type CardValues } from './CardModal'

const pageSize = 5
const statusLabels: Record<Flashcard['status'], string> = {
  DRAFT: 'Draft',
  PUBLISHED: 'Published',
  CLOSED: 'Closed',
}
const statusColors: Record<Flashcard['status'], { background: string; color: string }> = {
  DRAFT: { background: 'var(--warning-soft)', color: 'var(--warning-text)' },
  PUBLISHED: { background: 'var(--success-soft)', color: 'var(--success-text)' },
  CLOSED: { background: 'var(--neutral-soft)', color: 'var(--neutral-text)' },
}

function StatusBadges({ card }: { card: Flashcard }) {
  return (
    <Space size={4}>
      <Tag bordered={false} style={statusColors[card.status]}>
        {card.status === 'PUBLISHED' && <Icon name="checkc" />}
        {' '}{statusLabels[card.status]}
      </Tag>
      {card.generated_by_ai && (
        <Tag
          bordered={false}
          style={{ background: 'var(--ai-soft)', color: 'var(--ai-text)' }}
        >
          <Icon name="spark" /> AI tạo
        </Tag>
      )}
    </Space>
  )
}

interface FlashcardTableProps {
  lessonId: string
}

export interface FlashcardTableHandle {
  openCreate: () => void
}

export const FlashcardTable = forwardRef<FlashcardTableHandle, FlashcardTableProps>(
  function FlashcardTable({ lessonId }, ref) {
    const { message, modal } = App.useApp()
    const queryClient = useQueryClient()
    const [cardForm] = Form.useForm<CardValues>()
    const [editing, setEditing] = useState<Flashcard | null | undefined>(undefined)
    const [filter, setFilter] = useState<'ALL' | Flashcard['status']>('ALL')
    const [page, setPage] = useState(1)
    const [selected, setSelected] = useState<string[]>([])
    const cards = useQuery({
      queryKey: ['lesson-flashcards', lessonId],
      queryFn: async () => {
        const first = await getFlashcards(lessonId, 1, 100)
        const count = Math.ceil(first.total / 100)
        const rest = await Promise.all(
          Array.from({ length: Math.max(0, count - 1) }, (_, index) =>
            getFlashcards(lessonId, index + 2, 100),
          ),
        )
        return [first, ...rest].flatMap(result => result.items)
      },
    })
    const refresh = () => {
      void queryClient.invalidateQueries({ queryKey: ['lesson-flashcards', lessonId] })
    }
    const saveCardMutation = useMutation({
      mutationFn: (values: CardValues) => editing
        ? updateCard(editing.id, values)
        : createCard(lessonId, values),
      onSuccess: () => {
        setEditing(undefined)
        cardForm.resetFields()
        refresh()
        void message.success('Đã lưu thẻ nháp')
      },
      onError: error => {
        if (isApiError(error) && error.status === 400) {
          const fields = Object.entries(error.fieldErrors())
            .filter(([name]) => name === 'front_text' || name === 'back_text')
            .map(([name, value]) => ({
              name: name as 'front_text' | 'back_text',
              errors: [value],
            }))
          cardForm.setFields(fields)
        } else {
          void message.error(aiErrorText(error))
        }
      },
    })
    const publishMutation = useMutation({
      mutationFn: ({ id, status }: { id: string; status: 'PUBLISHED' | 'CLOSED' }) =>
        publishCard(id, status),
      onSuccess: () => { refresh(); void message.success('Đã cập nhật trạng thái thẻ') },
      onError: error => { refresh(); void message.error(aiErrorText(error)) },
    })
    const bulkPublishMutation = useMutation({
      mutationFn: async (ids: string[]) => {
        for (const id of ids) await publishCard(id, 'PUBLISHED')
      },
      onSuccess: () => {
        setSelected([])
        refresh()
        void message.success('Đã xuất bản các thẻ đã chọn')
      },
      onError: error => { refresh(); void message.error(aiErrorText(error)) },
    })
    const deleteCardMutation = useMutation({
      mutationFn: deleteCard,
      onSuccess: () => { refresh(); void message.success('Đã xóa thẻ') },
      onError: error => { refresh(); void message.error(aiErrorText(error)) },
    })
    const openCreate = () => {
      cardForm.resetFields()
      setEditing(null)
    }
    useImperativeHandle(ref, () => ({ openCreate }))
    const closeCardModal = () => {
      cardForm.resetFields()
      setEditing(undefined)
    }
    const openEdit = (card: Flashcard) => {
      cardForm.setFieldsValue({ front_text: card.front_text, back_text: card.back_text })
      setEditing(card)
    }
    const confirmPublication = (id: string, status: 'PUBLISHED' | 'CLOSED') => {
      modal.confirm({
        title: status === 'PUBLISHED' ? 'Xuất bản thẻ?' : 'Đóng thẻ?',
        onOk: () => publishMutation.mutateAsync({ id, status }),
      })
    }
    const confirmDelete = (id: string) => {
      modal.confirm({
        title: 'Xóa thẻ nháp?',
        content: 'Thẻ sẽ bị xóa vĩnh viễn.',
        okText: 'Xóa thẻ',
        okButtonProps: { danger: true },
        onOk: () => deleteCardMutation.mutateAsync(id),
      })
    }
    const allCards = cards.data ?? []
    const filtered = filter === 'ALL'
      ? allCards
      : allCards.filter(card => card.status === filter)
    const visible = filtered.slice((page - 1) * pageSize, page * pageSize)
    const chips: { value: typeof filter; label: string }[] = [
      { value: 'ALL', label: 'Tất cả' },
      { value: 'DRAFT', label: 'Draft' },
      { value: 'PUBLISHED', label: 'Published' },
      { value: 'CLOSED', label: 'Closed' },
    ]

    return (
      <>
        <Card
          title={`Flashcard của bài (${allCards.length})`}
          extra={(
            <Button icon={<Icon name="plus" />} onClick={openCreate}>
              Tạo thẻ thủ công
            </Button>
          )}
        >
          <Typography.Paragraph type="secondary">
            Thẻ đã xuất bản không sửa được, chỉ có thể đóng.
          </Typography.Paragraph>
          <Space wrap style={{ marginBottom: 16 }}>
            {chips.map(chip => (
              <Button
                key={chip.value}
                type={filter === chip.value ? 'primary' : 'default'}
                onClick={() => { setFilter(chip.value); setPage(1) }}
              >
                {chip.label} ({chip.value === 'ALL'
                  ? allCards.length
                  : allCards.filter(card => card.status === chip.value).length})
              </Button>
            ))}
          </Space>
          {selected.length > 0 && (
            <Alert
              type="info"
              message={`Đã chọn ${selected.length} thẻ`}
              action={(
                <Space>
                  <Button
                    onClick={() => modal.confirm({
                      title: 'Xuất bản đã chọn?',
                      onOk: () => bulkPublishMutation.mutateAsync(selected),
                    })}
                  >
                    Xuất bản đã chọn
                  </Button>
                  <Button onClick={() => setSelected([])}>
                    Bỏ chọn
                  </Button>
                </Space>
              )}
              style={{ marginBottom: 16 }}
            />
          )}
          {cards.isPending ? <LoadingState /> : cards.isError ? (
            <ErrorState error={cards.error} onRetry={() => void cards.refetch()} />
          ) : !allCards.length ? (
            <EmptyState
              description="Chưa có flashcard"
              action={(
                <Button onClick={openCreate}>
                  Tạo thủ công
                </Button>
              )}
            />
          ) : (
            <>
              <Table
                rowKey="id"
                dataSource={visible}
                pagination={false}
                rowSelection={{
                  selectedRowKeys: selected,
                  onChange: keys => setSelected(keys.map(String)),
                  getCheckboxProps: card => ({ disabled: card.status !== 'DRAFT' }),
                }}
                columns={[
                  { title: 'Mặt trước', dataIndex: 'front_text', ellipsis: true },
                  { title: 'Mặt sau', dataIndex: 'back_text', ellipsis: true },
                  { title: 'Trạng thái', render: (_, card) => <StatusBadges card={card} /> },
                  {
                    title: 'Ngày tạo',
                    dataIndex: 'created_at',
                    render: (value: string) => new Date(value).toLocaleDateString('vi-VN'),
                  },
                  {
                    title: 'Thao tác',
                    render: (_, card) => card.status === 'DRAFT' ? (
                      <Space>
                        <Button size="small" onClick={() => openEdit(card)}>
                          Sửa
                        </Button>
                        <Button size="small" danger onClick={() => confirmDelete(card.id)}>
                          Xóa
                        </Button>
                        <Button
                          size="small"
                          onClick={() => confirmPublication(card.id, 'PUBLISHED')}
                        >
                          Xuất bản
                        </Button>
                      </Space>
                    ) : card.status === 'PUBLISHED' ? (
                      <Button size="small" onClick={() => confirmPublication(card.id, 'CLOSED')}>
                        Đóng
                      </Button>
                    ) : null,
                  },
                ]}
              />
              <Pagination
                current={page}
                pageSize={pageSize}
                total={filtered.length}
                onChange={setPage}
                style={{ marginTop: 16, textAlign: 'right' }}
              />
            </>
          )}
        </Card>
        <CardModal
          editing={editing}
          form={cardForm}
          pending={saveCardMutation.isPending}
          onCancel={closeCardModal}
          onSave={values => saveCardMutation.mutate(values)}
        />
      </>
    )
  },
)
