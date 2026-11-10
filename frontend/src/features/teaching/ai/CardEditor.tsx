import { Button, Card, Input, Space, Typography } from 'antd'
import { Icon } from '../../../shared/components/Icon'

export interface CandidateCard {
  front_text: string
  back_text: string
}

interface CardEditorProps {
  card: CandidateCard
  index: number
  saved: boolean
  fieldErrors: Record<string, string>
  onChange: (patch: Partial<CandidateCard>) => void
  onRemove: () => void
}

export function CardEditor({
  card, index, saved, fieldErrors, onChange, onRemove,
}: CardEditorProps) {
  const errors = [
    !card.front_text.trim() ? 'Nhập mặt trước' : '',
    !card.back_text.trim() ? 'Nhập mặt sau' : '',
  ].filter(Boolean)

  return (
    <Card
      title={`Thẻ ${index + 1}`}
      extra={(
        <Button danger icon={<Icon name="trash" />} disabled={saved} onClick={onRemove}>
          Xóa
        </Button>
      )}
      style={errors.length ? { borderColor: 'var(--danger)' } : undefined}
    >
      <Space direction="vertical" style={{ width: '100%' }}>
        <label>
          Mặt trước
          <Input.TextArea
            value={card.front_text}
            disabled={saved}
            maxLength={4000}
            onChange={event => onChange({ front_text: event.target.value })}
          />
          {fieldErrors.front_text && (
            <Typography.Text type="danger">
              {fieldErrors.front_text}
            </Typography.Text>
          )}
        </label>
        <label>
          Mặt sau
          <Input.TextArea
            value={card.back_text}
            disabled={saved}
            maxLength={4000}
            onChange={event => onChange({ back_text: event.target.value })}
          />
          {fieldErrors.back_text && (
            <Typography.Text type="danger">
              {fieldErrors.back_text}
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
