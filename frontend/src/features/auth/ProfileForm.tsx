import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Alert, App, Button, Card, Form, Input } from 'antd'
import { useEffect } from 'react'
import { isApiError } from '../../shared/api/client'
import { getSession, updateCurrentUser } from '../../shared/auth/session'
import type { ProfileRequest, User } from '../../shared/auth/types'
import { updateProfile } from './api'

export function ProfileForm({ user }: { user: User }) {
  const [form] = Form.useForm<ProfileRequest>()
  const { message } = App.useApp()
  const queryClient = useQueryClient()
  const mutation = useMutation({
    mutationFn: updateProfile,
    onSuccess: (updated) => {
      if (getSession().user?.id !== updated.id) return
      updateCurrentUser(updated)
      queryClient.setQueryData(['users', 'me'], updated)
      void queryClient.invalidateQueries({ queryKey: ['users', 'list'] })
      void message.success('Đã lưu hồ sơ')
    },
    onError: (error) => {
      if (isApiError(error) && error.fieldErrors().full_name) {
        form.setFields([{ name: 'full_name', errors: [error.fieldErrors().full_name] }])
      }
    },
  })

  useEffect(() => {
    form.setFieldsValue({ full_name: user.full_name })
  }, [user.full_name, form])

  function cancel(): void {
    form.resetFields()
    form.setFieldsValue({ full_name: user.full_name })
    mutation.reset()
  }

  return (
    <Card title="Thông tin cá nhân">
      <div className="account-stack">
        <p className="account-description">Họ tên hiển thị trên bài làm và danh sách học viên.</p>
        {mutation.error && !(isApiError(mutation.error) && mutation.error.fieldErrors().full_name) ? (
          <Alert type="error" showIcon title={mutation.error.message} />
        ) : null}
        <Form<ProfileRequest>
          form={form}
          name="profile"
          layout="vertical"
          requiredMark={false}
          initialValues={{ full_name: user.full_name }}
          disabled={mutation.isPending}
          onValuesChange={() => mutation.reset()}
          onFinish={(values) => {
            if (!mutation.isPending) mutation.mutate({ full_name: values.full_name.trim() })
          }}
        >
          <Form.Item name="full_name" label="Họ và tên" rules={[
            { required: true, whitespace: true, message: 'Họ tên không được để trống.' },
            { max: 120, message: 'Họ và tên tối đa 120 ký tự.' },
          ]}>
            <Input autoComplete="name" />
          </Form.Item>
          <Form.Item label="Email" extra="Email dùng để đăng nhập, không thể thay đổi.">
            <Input value={user.email} disabled aria-label="Email" />
          </Form.Item>
          <div className="account-actions">
            <Button onClick={cancel}>Hủy</Button>
            <Button type="primary" htmlType="submit" loading={mutation.isPending} aria-label="Lưu hồ sơ" aria-busy={mutation.isPending}>Lưu</Button>
          </div>
        </Form>
      </div>
    </Card>
  )
}
