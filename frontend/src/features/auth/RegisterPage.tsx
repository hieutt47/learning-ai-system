import { useMutation } from '@tanstack/react-query'
import { Alert, App, Button, Form, Input } from 'antd'
import { Link, useNavigate } from 'react-router'
import { apiPost, isApiError } from '../../shared/api/client'
import type { RegisterRequest, User } from '../../shared/auth/types'
import { Icon } from '../../shared/components/Icon'
import { paths } from '../../shared/routing/paths'

interface RegisterFormValues extends RegisterRequest {
  confirm_password: string
}

function registerError(error: unknown): string | null {
  if (!error) {
    return null
  }
  if (isApiError(error)) {
    if (error.code === 'EMAIL_ALREADY_EXISTS') {
      return null
    }
    if (error.status === 400 && error.details.some((detail) => ['full_name', 'email', 'password'].includes(detail.field))) {
      return null
    }
    if (error.status >= 500) {
      return 'Máy chủ đang gặp sự cố. Vui lòng thử lại sau.'
    }
    return error.message
  }
  return 'Không thể tạo tài khoản. Vui lòng thử lại.'
}

export function RegisterPage() {
  const [form] = Form.useForm<RegisterFormValues>()
  const password = Form.useWatch('password', form) as string | undefined
  const navigate = useNavigate()
  const { message } = App.useApp()
  const mutation = useMutation({
    mutationFn: (request: RegisterRequest) => apiPost<User>('/auth/register', request, { auth: false }),
    onSuccess: () => {
      void message.success('Tạo tài khoản thành công. Vui lòng đăng nhập.')
      void navigate(paths.login, { replace: true })
    },
    onError: (error) => {
      if (isApiError(error)) {
        if (error.code === 'EMAIL_ALREADY_EXISTS') {
          form.setFields([{ name: 'email', errors: ['Email này đã được đăng ký. Hãy đăng nhập hoặc dùng email khác.'] }])
          return
        }
        const errors = error.fieldErrors()
        for (const name of ['full_name', 'email', 'password'] as const) {
          if (errors[name]) {
            form.setFields([{ name, errors: [errors[name]] }])
          }
        }
      }
    },
  })
  const errorMessage = registerError(mutation.error)

  function submit(values: RegisterFormValues): void {
    if (!mutation.isPending) {
      mutation.mutate({
        full_name: values.full_name.trim(),
        email: values.email.trim(),
        password: values.password,
      })
    }
  }

  return (
    <section className="auth-form" aria-labelledby="register-title">
      <div>
        <h1 id="register-title">Tạo tài khoản</h1>
        <p className="auth-subtitle">Đăng ký miễn phí để ghi danh khóa học và lưu tiến độ.</p>
      </div>
      {errorMessage ? <Alert type="error" showIcon title={errorMessage} /> : null}
      <Form<RegisterFormValues>
        form={form}
        name="register"
        layout="vertical"
        requiredMark={false}
        onFinish={submit}
        onValuesChange={() => mutation.reset()}
        disabled={mutation.isPending}
        noValidate
      >
        <Form.Item
          name="full_name"
          label="Họ và tên"
          rules={[
            { required: true, whitespace: true, message: 'Nhập họ và tên.' },
            { max: 120, message: 'Họ và tên tối đa 120 ký tự.' },
          ]}
        >
          <Input autoComplete="name" placeholder="VD: Lê Minh Châu" prefix={<Icon name="user" />} />
        </Form.Item>
        <Form.Item
          name="email"
          label="Email"
          rules={[
            { required: true, whitespace: true, message: 'Nhập email.' },
            { type: 'email', message: 'Email chưa đúng định dạng, ví dụ ten@truong.edu.vn.' },
            { max: 254, message: 'Email tối đa 254 ký tự.' },
          ]}
        >
          <Input type="email" autoComplete="username" placeholder="ten@truong.edu.vn" prefix={<Icon name="mail" />} />
        </Form.Item>
        <Form.Item
          name="password"
          label="Mật khẩu"
          extra={`Từ 10 đến 72 ký tự · ${password?.length ?? 0} ký tự`}
          rules={[
            { required: true, whitespace: true, message: 'Nhập mật khẩu.' },
            { min: 10, max: 72, message: 'Mật khẩu cần từ 10 đến 72 ký tự.' },
          ]}
        >
          <Input.Password
            autoComplete="new-password"
            placeholder="Tạo mật khẩu"
            prefix={<Icon name="lock" />}
            iconRender={(visible) => <Icon name={visible ? 'eyeoff' : 'eye'} />}
          />
        </Form.Item>
        <Form.Item
          name="confirm_password"
          label="Xác nhận mật khẩu"
          dependencies={['password']}
          rules={[
            { required: true, message: 'Nhập lại mật khẩu.' },
            ({ getFieldValue }) => ({
              validator(_, value: string) {
                if (!value || getFieldValue('password') === value) {
                  return Promise.resolve()
                }
                return Promise.reject(new Error('Mật khẩu xác nhận chưa khớp.'))
              },
            }),
          ]}
        >
          <Input.Password
            autoComplete="new-password"
            placeholder="Nhập lại mật khẩu"
            prefix={<Icon name="lock" />}
            iconRender={(visible) => <Icon name={visible ? 'eyeoff' : 'eye'} />}
          />
        </Form.Item>
        <p className="auth-hint">Tài khoản mới là Sinh viên. Muốn dạy học, gửi yêu cầu trong trang Hồ sơ.</p>
        <Form.Item className="auth-actions">
          <Button type="primary" htmlType="submit" block loading={mutation.isPending} aria-label="Tạo tài khoản" aria-busy={mutation.isPending}>Tạo tài khoản</Button>
        </Form.Item>
      </Form>
      <p className="auth-footer">Đã có tài khoản? <Link to={paths.login}>Đăng nhập</Link></p>
    </section>
  )
}
