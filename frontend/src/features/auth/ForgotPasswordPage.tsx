import { useMutation } from '@tanstack/react-query'
import { Alert, Button, Form, Input } from 'antd'
import { Link, useSearchParams } from 'react-router'
import { isApiError } from '../../shared/api/client'
import type { ForgotPasswordRequest } from '../../shared/auth/types'
import { Icon } from '../../shared/components/Icon'
import { paths } from '../../shared/routing/paths'
import { forgotPassword } from './api'

function forgotPasswordError(error: unknown): string {
  if (isApiError(error)) {
    if (error.code === 'EMAIL_SEND_FAILED') return 'Chưa thể gửi email đặt lại mật khẩu. Vui lòng thử lại sau.'
    if (error.status === 400) return 'Email chưa hợp lệ. Vui lòng kiểm tra lại.'
    if (error.status === 429) return 'Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau.'
    if (error.status >= 500) return 'Máy chủ đang gặp sự cố. Vui lòng thử lại sau.'
  }
  return 'Không thể gửi yêu cầu. Vui lòng kiểm tra kết nối và thử lại.'
}

export function ForgotPasswordPage() {
  const [form] = Form.useForm<ForgotPasswordRequest>()
  const [params, setParams] = useSearchParams()
  const mutation = useMutation({
    mutationFn: forgotPassword,
    onSuccess: (_, request) => setParams({ email: request.email }, { replace: true }),
    onError: (error) => {
      if (isApiError(error) && error.fieldErrors().email) {
        form.setFields([{ name: 'email', errors: ['Email chưa hợp lệ. Vui lòng kiểm tra lại.'] }])
      }
    },
  })
  const actionLabel = mutation.isSuccess ? 'Gửi lại email' : 'Gửi liên kết đặt lại mật khẩu'

  return (
    <section className="auth-form" aria-labelledby="forgot-password-title">
      <div>
        <h1 id="forgot-password-title">Quên mật khẩu</h1>
        <p className="auth-subtitle">Nhập email tài khoản để nhận liên kết đặt lại mật khẩu.</p>
      </div>
      {mutation.isSuccess ? (
        <Alert type="success" showIcon title="Nếu email hợp lệ, bạn sẽ nhận được liên kết đặt lại mật khẩu. Vui lòng kiểm tra hộp thư và thư rác." />
      ) : null}
      {mutation.error ? <Alert type="error" showIcon title={forgotPasswordError(mutation.error)} /> : null}
      <Form<ForgotPasswordRequest>
        form={form}
        name="forgot-password"
        layout="vertical"
        requiredMark={false}
        initialValues={{ email: params.get('email') || '' }}
        disabled={mutation.isPending}
        noValidate
        onValuesChange={() => mutation.reset()}
        onFinish={(values) => {
          if (!mutation.isPending) mutation.mutate({ email: values.email.trim() })
        }}
      >
        <Form.Item name="email" label="Email" rules={[
          { required: true, whitespace: true, message: 'Nhập email.' },
          { type: 'email', message: 'Email chưa đúng định dạng, ví dụ ten@truong.edu.vn.' },
          { max: 254, message: 'Email tối đa 254 ký tự.' },
        ]}>
          <Input type="email" autoComplete="email" placeholder="ten@truong.edu.vn" prefix={<Icon name="mail" />} />
        </Form.Item>
        <p className="auth-hint">Liên kết có hiệu lực trong 30 phút và chỉ dùng một lần. Gửi lại thành công sẽ vô hiệu hóa liên kết cũ.</p>
        <Form.Item className="auth-actions">
          <Button type="primary" htmlType="submit" block loading={mutation.isPending} disabled={mutation.isPending} aria-label={actionLabel} aria-busy={mutation.isPending}>
            {actionLabel}
          </Button>
        </Form.Item>
      </Form>
      <p className="auth-footer"><Link to={paths.login}>Về trang đăng nhập</Link></p>
    </section>
  )
}
