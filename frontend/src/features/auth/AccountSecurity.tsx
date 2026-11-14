import { useMutation } from '@tanstack/react-query'
import { Alert, App, Button, Card, Form, Input, Modal } from 'antd'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { isApiError } from '../../shared/api/client'
import { changePassword, logoutAll } from '../../shared/auth/session'
import type { ChangePasswordRequest } from '../../shared/auth/types'
import { paths } from '../../shared/routing/paths'

interface PasswordFormValues extends ChangePasswordRequest {
  confirm_password: string
}

const PASSWORD_MESSAGES: Record<string, string> = {
  'Current password is incorrect': 'Mật khẩu hiện tại không đúng.',
  'Current password must not be blank': 'Nhập mật khẩu hiện tại.',
  'Current password must not exceed 72 characters': 'Mật khẩu hiện tại tối đa 72 ký tự.',
  'New password must not be blank': 'Nhập mật khẩu mới.',
  'New password must contain between 10 and 72 characters': 'Mật khẩu mới cần từ 10 đến 72 ký tự.',
  'New password must differ from current password': 'Mật khẩu mới phải khác mật khẩu hiện tại.',
}

export function AccountSecurity() {
  const [form] = Form.useForm<PasswordFormValues>()
  const [confirmLogout, setConfirmLogout] = useState(false)
  const { message } = App.useApp()
  const navigate = useNavigate()
  const passwordMutation = useMutation({
    mutationFn: changePassword,
    onSuccess: () => {
      form.resetFields()
      void message.success('Đã đổi mật khẩu')
    },
    onError: (error) => {
      if (isApiError(error)) {
        const errors = error.fieldErrors()
        for (const name of ['current_password', 'new_password'] as const) {
          if (errors[name]) {
            const text = PASSWORD_MESSAGES[errors[name]]
              ?? (name === 'current_password' ? 'Mật khẩu hiện tại không hợp lệ.' : 'Mật khẩu mới không hợp lệ.')
            form.setFields([{ name, errors: [text] }])
          }
        }
      }
    },
  })
  const logoutMutation = useMutation({
    mutationFn: logoutAll,
    onSuccess: () => {
      void message.success('Đã đăng xuất tất cả phiên')
      void navigate(paths.login, { replace: true })
    },
  })
  const pending = passwordMutation.isPending || logoutMutation.isPending
  const passwordFieldError = isApiError(passwordMutation.error)
    && Object.keys(passwordMutation.error.fieldErrors()).length > 0

  return (
    <>
      <Card title="Đổi mật khẩu">
        <div className="account-stack">
          <p className="account-description">Đổi mật khẩu bằng cách nhập mật khẩu hiện tại và mật khẩu mới. Các phiên đăng nhập được giữ nguyên.</p>
          {passwordMutation.error && !passwordFieldError ? (
            <Alert type="error" showIcon title="Không thể đổi mật khẩu. Vui lòng thử lại." />
          ) : null}
          <Form<PasswordFormValues>
            form={form}
            name="change-password"
            layout="vertical"
            requiredMark={false}
            disabled={pending}
            onValuesChange={() => passwordMutation.reset()}
            onFinish={(values) => {
              if (!pending) {
                passwordMutation.mutate({ current_password: values.current_password, new_password: values.new_password })
              }
            }}
          >
            <Form.Item name="current_password" label="Mật khẩu hiện tại" extra={<Link to={paths.forgotPassword}>Quên mật khẩu hiện tại?</Link>} rules={[
              { required: true, whitespace: true, message: 'Nhập mật khẩu hiện tại.' },
              { max: 72, message: 'Mật khẩu tối đa 72 ký tự.' },
            ]}>
              <Input.Password autoComplete="current-password" />
            </Form.Item>
            <Form.Item name="new_password" label="Mật khẩu mới" rules={[
              { required: true, whitespace: true, message: 'Nhập mật khẩu mới.' },
              { min: 10, max: 72, message: 'Mật khẩu cần từ 10 đến 72 ký tự.' },
            ]}>
              <Input.Password autoComplete="new-password" />
            </Form.Item>
            <Form.Item name="confirm_password" label="Xác nhận mật khẩu mới" dependencies={['new_password']} rules={[
              { required: true, message: 'Nhập lại mật khẩu mới.' },
              ({ getFieldValue }) => ({
                validator(_, value: string) {
                  if (!value || getFieldValue('new_password') === value) {
                    return Promise.resolve()
                  }
                  return Promise.reject(new Error('Mật khẩu xác nhận chưa khớp.'))
                },
              }),
            ]}>
              <Input.Password autoComplete="new-password" />
            </Form.Item>
            <div className="account-actions">
              <Button type="primary" htmlType="submit" loading={passwordMutation.isPending} aria-label="Đổi mật khẩu" aria-busy={passwordMutation.isPending}>Đổi mật khẩu</Button>
            </div>
          </Form>
        </div>
      </Card>
      <Card title="Phiên đăng nhập">
        <div className="account-stack">
          <p className="account-description">Thu hồi các phiên đăng nhập của tài khoản. Phiên ở trình duyệt khác có thể tiếp tục hoạt động tối đa 15 phút.</p>
          <div className="account-actions">
            <Button danger disabled={pending} onClick={() => { logoutMutation.reset(); setConfirmLogout(true) }}>Đăng xuất tất cả phiên</Button>
          </div>
        </div>
      </Card>
      <Modal
        open={confirmLogout}
        title="Đăng xuất tất cả phiên?"
        okText="Đăng xuất tất cả phiên"
        cancelText="Hủy"
        confirmLoading={logoutMutation.isPending}
        okButtonProps={{ danger: true, disabled: pending, 'aria-label': 'Đăng xuất tất cả phiên', 'aria-busy': logoutMutation.isPending }}
        cancelButtonProps={{ disabled: pending }}
        closable={!pending}
        mask={{ closable: !pending }}
        keyboard={!pending}
        onCancel={() => { if (!pending) setConfirmLogout(false) }}
        onOk={() => { if (!pending) logoutMutation.mutate() }}
      >
        <div className="account-stack">
          <p>Bạn sẽ đăng xuất khỏi trình duyệt này và cần đăng nhập lại để tiếp tục sử dụng tài khoản.</p>
          {logoutMutation.error && <Alert type="error" showIcon title={logoutMutation.error.message} />}
        </div>
      </Modal>
    </>
  )
}
