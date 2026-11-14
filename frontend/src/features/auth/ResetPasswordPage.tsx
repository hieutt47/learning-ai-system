import { useMutation } from '@tanstack/react-query'
import { Alert, Button, Form, Input } from 'antd'
import { Link, useSearchParams } from 'react-router'
import { isApiError } from '../../shared/api/client'
import { resetPassword } from '../../shared/auth/session'
import { Icon } from '../../shared/components/Icon'
import { paths } from '../../shared/routing/paths'

interface ResetPasswordFormValues {
  new_password: string
  confirm_password: string
}

const PASSWORD_MESSAGES: Record<string, string> = {
  'New password must not be blank': 'Nhập mật khẩu mới.',
  'New password must contain between 10 and 72 characters': 'Mật khẩu mới cần từ 10 đến 72 ký tự.',
  'New password must differ from current password': 'Mật khẩu mới phải khác mật khẩu hiện tại.',
}

function resetPasswordError(error: unknown): string {
  if (isApiError(error)) {
    if (error.code === 'INVALID_PASSWORD_RESET_TOKEN' || error.fieldErrors().token) {
      return 'Liên kết đặt lại mật khẩu không hợp lệ, đã hết hạn hoặc đã được sử dụng. Vui lòng yêu cầu liên kết mới.'
    }
    if (error.code === 'ACCOUNT_INACTIVE') return 'Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.'
    if (error.code === 'EMAIL_NOT_VERIFIED') return 'Bạn cần xác minh email trước khi đặt lại mật khẩu.'
    if (error.status === 400) return 'Mật khẩu mới chưa hợp lệ. Vui lòng kiểm tra lại.'
    if (error.status === 403) return 'Không thể đặt lại mật khẩu cho tài khoản này.'
    if (error.status === 429) return 'Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau.'
    if (error.status >= 500) return 'Máy chủ đang gặp sự cố. Vui lòng thử lại sau.'
  }
  return 'Không thể đặt lại mật khẩu. Vui lòng kiểm tra kết nối và thử lại.'
}

export function ResetPasswordPage() {
  const [params, setParams] = useSearchParams()
  const token = params.get('token') || ''
  const [form] = Form.useForm<ResetPasswordFormValues>()
  const mutation = useMutation({
    mutationFn: resetPassword,
    onSuccess: () => {
      form.resetFields()
      setParams({}, { replace: true })
    },
    onError: (error) => {
      if (isApiError(error)) {
        const passwordError = error.fieldErrors().new_password
        if (passwordError) {
          form.setFields([{ name: 'new_password', errors: [PASSWORD_MESSAGES[passwordError] || 'Mật khẩu mới chưa hợp lệ. Vui lòng kiểm tra lại.'] }])
        }
      }
    },
  })
  const invalidToken = !/^[A-Za-z0-9_-]{43}$/.test(token)
    || (isApiError(mutation.error) && (mutation.error.code === 'INVALID_PASSWORD_RESET_TOKEN' || Boolean(mutation.error.fieldErrors().token)))
  const blocked = isApiError(mutation.error) && mutation.error.status === 403

  return (
    <section className="auth-form" aria-labelledby="reset-password-title">
      <div>
        <h1 id="reset-password-title">{mutation.isSuccess ? 'Đặt lại mật khẩu thành công' : 'Đặt lại mật khẩu'}</h1>
        <p className="auth-subtitle">{mutation.isSuccess
          ? 'Bạn có thể đăng nhập bằng mật khẩu mới.'
          : 'Tạo mật khẩu mới cho tài khoản Learning AI của bạn.'}</p>
      </div>
      {mutation.isSuccess ? (
        <>
          <Alert type="success" showIcon title="Mật khẩu đã được cập nhật. Vui lòng đăng nhập lại." />
          <Link className="btn btn-primary btn-block" to={paths.login}>Đăng nhập</Link>
        </>
      ) : invalidToken ? (
        <>
          <Alert type="error" showIcon title="Liên kết đặt lại mật khẩu không hợp lệ, đã hết hạn hoặc đã được sử dụng. Vui lòng yêu cầu liên kết mới." />
          <Link className="btn btn-primary btn-block" to={paths.forgotPassword}>Yêu cầu liên kết mới</Link>
        </>
      ) : blocked ? (
        <>
          <Alert type="error" showIcon title={resetPasswordError(mutation.error)} />
          {isApiError(mutation.error) && mutation.error.code === 'EMAIL_NOT_VERIFIED' ? (
            <Link className="btn btn-primary btn-block" to={paths.verifyEmail}>Xác minh email</Link>
          ) : null}
        </>
      ) : (
        <>
          {mutation.error ? <Alert type="error" showIcon title={resetPasswordError(mutation.error)} /> : null}
          <Form<ResetPasswordFormValues>
            form={form}
            name="reset-password"
            layout="vertical"
            requiredMark={false}
            disabled={mutation.isPending}
            noValidate
            onValuesChange={() => mutation.reset()}
            onFinish={(values) => {
              if (!mutation.isPending) mutation.mutate({ token, new_password: values.new_password })
            }}
          >
            <Form.Item name="new_password" label="Mật khẩu mới" rules={[
              { required: true, whitespace: true, message: 'Nhập mật khẩu mới.' },
              { min: 10, max: 72, message: 'Mật khẩu mới cần từ 10 đến 72 ký tự.' },
            ]}>
              <Input.Password
                autoComplete="new-password"
                placeholder="Nhập mật khẩu mới"
                prefix={<Icon name="lock" />}
                iconRender={(visible) => <Icon name={visible ? 'eyeoff' : 'eye'} />}
              />
            </Form.Item>
            <Form.Item name="confirm_password" label="Xác nhận mật khẩu mới" dependencies={['new_password']} rules={[
              { required: true, message: 'Nhập lại mật khẩu mới.' },
              ({ getFieldValue }) => ({
                validator(_, value: string) {
                  if (!value || getFieldValue('new_password') === value) return Promise.resolve()
                  return Promise.reject(new Error('Mật khẩu xác nhận chưa khớp.'))
                },
              }),
            ]}>
              <Input.Password
                autoComplete="new-password"
                placeholder="Nhập lại mật khẩu mới"
                prefix={<Icon name="lock" />}
                iconRender={(visible) => <Icon name={visible ? 'eyeoff' : 'eye'} />}
              />
            </Form.Item>
            <p className="auth-hint">Mật khẩu mới cần từ 10 đến 72 ký tự và khác mật khẩu hiện tại.</p>
            <Form.Item className="auth-actions">
              <Button type="primary" htmlType="submit" block loading={mutation.isPending} disabled={mutation.isPending} aria-label="Đặt lại mật khẩu" aria-busy={mutation.isPending}>Đặt lại mật khẩu</Button>
            </Form.Item>
          </Form>
        </>
      )}
      {!mutation.isSuccess ? <p className="auth-footer"><Link to={paths.login}>Về trang đăng nhập</Link></p> : null}
    </section>
  )
}
