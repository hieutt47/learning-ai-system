import { useMutation } from '@tanstack/react-query'
import { Alert, Button, Form, Input } from 'antd'
import { useEffect, useRef } from 'react'
import { Link, useSearchParams } from 'react-router'
import { apiPost, isApiError } from '../../shared/api/client'
import { paths } from '../../shared/routing/paths'

function verificationError(error: Error): string {
  if (isApiError(error)) {
    if (error.code === 'INVALID_EMAIL_VERIFICATION_TOKEN') return 'Link xác minh không hợp lệ, đã hết hạn hoặc đã được sử dụng.'
    if (error.code === 'EMAIL_SEND_FAILED') return 'Chưa thể gửi email. Vui lòng thử lại sau.'
    if (error.code === 'ACCOUNT_INACTIVE') return 'Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.'
    if (error.status >= 500) return 'Máy chủ chưa thể xử lý. Vui lòng thử lại.'
  }
  return error.message
}

export function VerifyEmailPage() {
  const [params, setParams] = useSearchParams()
  const token = params.get('token') || ''
  const [form] = Form.useForm<{ email: string }>()
  const verification = useMutation({
    mutationFn: (value: string) => apiPost<void>('/auth/verify-email', { token: value }, { auth: false }),
    onSuccess: () => setParams({}, { replace: true }),
  })
  const submittedToken = useRef<string | null>(null)
  const verify = verification.mutate
  useEffect(() => {
    if (token && submittedToken.current !== token) {
      submittedToken.current = token
      verify(token)
    }
  }, [token, verify])
  const resend = useMutation({
    mutationFn: (email: string) => apiPost<void>('/auth/resend-verification', { email }, { auth: false }),
    onSuccess: () => {
      verification.reset()
      setParams({ email: form.getFieldValue('email').trim() }, { replace: true })
    },
    onError: (error) => {
      if (isApiError(error) && error.fieldErrors().email) {
        form.setFields([{ name: 'email', errors: [error.fieldErrors().email] }])
      }
    },
  })
  const invalidLink = isApiError(verification.error) && verification.error.status === 400
  const blocked = isApiError(verification.error) && verification.error.status === 403
  const pending = verification.isPending || resend.isPending
  const confirming = Boolean(token) && !invalidLink

  return (
    <section className="auth-form" aria-labelledby="verify-email-title">
      <div>
        <h1 id="verify-email-title">{verification.isSuccess ? 'Xác minh thành công' : verification.isError ? 'Không thể xác minh email' : confirming ? 'Đang xác minh email' : 'Kiểm tra email'}</h1>
        <p className="auth-subtitle">{verification.isSuccess
          ? 'Bạn có thể đăng nhập để sử dụng tài khoản Learning AI.'
          : verification.isError
            ? 'Vui lòng xem thông báo bên dưới để tiếp tục.'
          : confirming
            ? 'Hệ thống đang xác minh địa chỉ email của bạn từ đường dẫn đã mở.'
            : 'Mở email từ Learning AI và bấm vào link xác minh. Bạn có thể kiểm tra thêm trong thư rác.'}</p>
      </div>
      {verification.isSuccess ? (
        <>
          <Alert type="success" showIcon title="Email đã được xác minh. Bạn có thể đăng nhập." />
          <Link className="btn btn-primary btn-block" to={paths.login}>Đăng nhập</Link>
        </>
      ) : confirming ? (
        <>
          {verification.error ? <Alert type="error" showIcon title={verificationError(verification.error)} /> : null}
          {!verification.error ? <p role="status" className="auth-hint">Đang xác minh email…</p> : null}
          {verification.isError && !blocked ? (
            <Button
              type="primary"
              block
              loading={verification.isPending}
              disabled={pending}
              onClick={() => { if (!pending) verification.mutate(token) }}
            >Thử lại xác minh</Button>
          ) : null}
        </>
      ) : (
        <>
          {verification.error ? <Alert type="error" showIcon title={verificationError(verification.error)} /> : null}
          <p className="auth-hint">Link có hiệu lực trong 24 giờ. Nếu chưa nhận được email hoặc link đã hết hạn, hãy yêu cầu gửi lại.</p>
          {resend.isSuccess ? <Alert type="success" showIcon title="Nếu tài khoản cần xác minh, email hướng dẫn đã được gửi. Vui lòng kiểm tra hộp thư." /> : null}
          {resend.error ? <Alert type="error" showIcon title={verificationError(resend.error)} /> : null}
          <Form<{ email: string }>
            form={form}
            layout="vertical"
            requiredMark={false}
            initialValues={{ email: params.get('email') || '' }}
            disabled={pending}
            onValuesChange={() => resend.reset()}
            onFinish={(values) => { if (!pending) resend.mutate(values.email.trim()) }}
          >
            <Form.Item name="email" label="Email" rules={[
              { required: true, whitespace: true, message: 'Nhập email.' },
              { type: 'email', message: 'Email không đúng định dạng.' },
              { max: 254, message: 'Email tối đa 254 ký tự.' },
            ]}>
              <Input type="email" autoComplete="email" />
            </Form.Item>
            <Button htmlType="submit" block loading={resend.isPending}>Gửi lại email xác minh</Button>
          </Form>
        </>
      )}
      {!verification.isSuccess ? <p className="auth-footer"><Link to={paths.login}>Về trang đăng nhập</Link></p> : null}
    </section>
  )
}
