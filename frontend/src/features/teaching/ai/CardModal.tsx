import { Form, Input, Modal } from 'antd'
import type { FormInstance } from 'antd'
import type { Flashcard } from './api'

export interface CardValues {
  front_text: string
  back_text: string
}

interface CardModalProps {
  editing: Flashcard | null | undefined
  form: FormInstance<CardValues>
  pending: boolean
  onCancel: () => void
  onSave: (values: CardValues) => void
}

export function CardModal({ editing, form, pending, onCancel, onSave }: CardModalProps) {
  return (
    <Modal
      title={editing?.id ? 'Sửa thẻ' : 'Tạo thẻ thủ công'}
      open={editing !== undefined}
      onCancel={onCancel}
      onOk={() => void form.validateFields().then(onSave).catch(() => undefined)}
      okText={editing?.id ? 'Lưu thẻ' : 'Tạo thẻ nháp'}
      confirmLoading={pending}
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="front_text"
          label="Mặt trước"
          rules={[{ required: true, whitespace: true, message: 'Mặt trước không được để trống' }]}
        >
          <Input.TextArea maxLength={4000} rows={3} />
        </Form.Item>
        <Form.Item
          name="back_text"
          label="Mặt sau"
          rules={[{ required: true, whitespace: true, message: 'Mặt sau không được để trống' }]}
        >
          <Input.TextArea maxLength={4000} rows={3} />
        </Form.Item>
      </Form>
    </Modal>
  )
}
