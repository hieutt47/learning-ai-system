import { useMutation } from '@tanstack/react-query'
import { Alert, App, Button, Form, Input } from 'antd'
import { Link } from 'react-router'
import { isApiError } from '../../shared/api/client'
import { useCurrentUser } from '../../shared/auth/currentUser'
import { login, restoreSession } from '../../shared/auth/session'
import type { LoginRequest } from '../../shared/auth/types'
import { Icon } from '../../shared/components/Icon'
import { paths } from '../../shared/routing/paths'

function loginError(error: unknown): string {
  if (isApiError(error)) {
    if (error.code === 'ACCOUNT_INACTIVE') {
      return 'Tài khoản của bạn đã bị khóa. Liên hệ quản trị viên để được mở lại.'
    }
    if (error.status === 401) {
      return 'Email hoặc mật khẩu không đúng. Vui lòng kiểm tra lại.'
    }
    if (error.status >= 500) {
      return 'Máy chủ đang gặp sự cố. Vui lòng thử lại sau.'
    }
    return error.message
  }
  return 'Không thể đăng nhập. Vui lòng thử lại.'
}

export function LoginPage() {
  const [form] = Form.useForm<LoginRequest>()
  const { message } = App.useApp()
  const { error: sessionError } = useCurrentUser()
  const mutation = useMutation({
    mutationFn: login,
    onSuccess: () => void message.success('Đăng nhập thành công'),
    onError: (error) => {
      if (isApiError(error)) {
        const errors = error.fieldErrors()
        for (const name of ['email', 'password'] as const) {
          if (errors[name]) {
            form.setFields([{ name, errors: [errors[name]] }])
          }
        }
      }
    },
  })

  function submit(values: LoginRequest): void {
    if (!mutation.isPending) {
      mutation.mutate({ email: values.email.trim(), password: values.password })
    }
  }

  return (
    <section className="auth-form" aria-labelledby="login-title">
      <div>
        <h1 id="login-title">Đăng nhập</h1>
        <p className="auth-subtitle">Chào mừng bạn quay lại. Nhập email và mật khẩu để tiếp tục học.</p>
      </div>
      {sessionError && !mutation.error ? (
        <Alert
          type="warning"
          showIcon
          title="Chưa thể khôi phục phiên đăng nhập. Bạn có thể đăng nhập lại hoặc thử kết nối lại."
          action={<Button size="small" onClick={() => void restoreSession()}>Thử lại</Button>}
        />
      ) : null}
      {mutation.error ? <Alert type="error" showIcon title={loginError(mutation.error)} /> : null}
      <Form<LoginRequest>
        form={form}
        name="login"
        layout="vertical"
        requiredMark={false}
        onFinish={submit}
        onValuesChange={() => mutation.reset()}
        disabled={mutation.isPending}
      >
        <Form.Item
          name="email"
          label="Email"
          rules={[
            { required: true, whitespace: true, message: 'Nhập email.' },
            { type: 'email', message: 'Email chưa đúng định dạng, ví dụ ten@truong.edu.vn.' },
          ]}
        >
          <Input type="email" autoComplete="username" placeholder="ten@truong.edu.vn" prefix={<Icon name="mail" />} />
        </Form.Item>
        <Form.Item
          name="password"
          label="Mật khẩu"
          rules={[
            { required: true, whitespace: true, message: 'Nhập mật khẩu.' },
            { max: 72, message: 'Mật khẩu tối đa 72 ký tự.' },
          ]}
        >
          <Input.Password
            autoComplete="current-password"
            placeholder="Nhập mật khẩu"
            prefix={<Icon name="lock" />}
            iconRender={(visible) => <Icon name={visible ? 'eyeoff' : 'eye'} />}
          />
        </Form.Item>
        <Form.Item className="auth-actions">
          <Button type="primary" htmlType="submit" block loading={mutation.isPending} aria-label="Đăng nhập" aria-busy={mutation.isPending}>Đăng nhập</Button>
        </Form.Item>
      </Form>
      <p className="auth-footer">Chưa có tài khoản? <Link to={paths.register}>Đăng ký</Link></p>
    </section>
  )
}
