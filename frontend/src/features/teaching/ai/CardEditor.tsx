import { Button, Card, Input, Space, Typography } from 'antd'
import { Icon } from '../../../shared/components/Icon'
import { cardErrors } from './validation'

export interface CandidateCard {
  front_text: string
  back_text: string
}

interface CardEditorProps {
  card: CandidateCard
  index: number
  saved: boolean
  isDuplicate?: boolean
  fieldErrors: Record<string, string>
  onChange: (patch: Partial<CandidateCard>) => void
  onRemove: () => void
}

export function CardEditor({
  card, index, saved, isDuplicate = false, fieldErrors, onChange, onRemove,
}: CardEditorProps) {
  const errors = cardErrors(card, isDuplicate)

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
            showCount
            rows={2}
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
            showCount
            rows={2}
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
