import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, App, Button, Card, Form, Input, Pagination } from 'antd'
import { useState } from 'react'
import { isApiError } from '../../shared/api/client'
import type { User } from '../../shared/auth/types'
import { EmptyState, ErrorState, LoadingState } from '../../shared/components/PageState'
import { formatDateTime } from '../../shared/utils/format'
import { createTeacherRequest, getMyTeacherRequests } from './api'
import { TeacherRequestStatusTag } from './TeacherRequestStatusTag'

export function TeacherRequestSection({ user, onRefreshUser }: { user: User; onRefreshUser: () => void }) {
  const [form] = Form.useForm<{ reason: string }>()
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const { message } = App.useApp()
  const queryClient = useQueryClient()
  const params = { page, page_size: pageSize, sort: 'created_at,desc' }
  const latestParams = { page: 1, page_size: 10, sort: 'created_at,desc' }
  const history = useQuery({
    queryKey: ['teacher-requests', 'me', user.id, params],
    queryFn: ({ signal }) => getMyTeacherRequests(params, signal),
    staleTime: 0,
    placeholderData: keepPreviousData,
  })
  const latest = useQuery({
    queryKey: ['teacher-requests', 'me', user.id, latestParams],
    queryFn: ({ signal }) => getMyTeacherRequests(latestParams, signal),
    staleTime: 0,
    enabled: user.role === 'STUDENT',
  })
  const pendingRequest = latest.data?.items.find((request) => request.status === 'PENDING')
  const mutation = useMutation({
    mutationFn: createTeacherRequest,
    onSuccess: async () => {
      form.resetFields()
      setPage(1)
      void message.success('Đã gửi yêu cầu trở thành giảng viên')
      await queryClient.invalidateQueries({ queryKey: ['teacher-requests'] })
    },
    onError: (error) => {
      if (isApiError(error)) {
        const reasonError = error.fieldErrors().reason
        if (reasonError) form.setFields([{ name: 'reason', errors: [reasonError] }])
        if (error.code === 'INVALID_STATE') {
          void message.warning('Bạn đã có yêu cầu đang chờ duyệt. Vui lòng kiểm tra lịch sử yêu cầu.')
          void queryClient.invalidateQueries({ queryKey: ['teacher-requests', 'me'] })
        }
      }
    },
  })

  function refresh(): void {
    void queryClient.invalidateQueries({ queryKey: ['teacher-requests', 'me'] })
    onRefreshUser()
  }

  return (
    <>
      {user.role === 'STUDENT' && (
        <Card title="Trở thành giảng viên">
          <div className="account-stack">
            <p className="account-description">Gửi yêu cầu để được tạo và quản lý khóa học. Quản trị viên sẽ xem xét và phản hồi.</p>
            {latest.isPending ? <LoadingState /> : latest.isError ? (
              <ErrorState error={latest.error} onRetry={() => void latest.refetch()} />
            ) : pendingRequest ? (
              <Alert type="warning" showIcon title="Yêu cầu đang chờ duyệt" description={`Bạn đã gửi ngày ${formatDateTime(pendingRequest.created_at)}. Vui lòng chờ quản trị viên xem xét.`} />
            ) : (
              <>
                {mutation.error && !(isApiError(mutation.error) && mutation.error.fieldErrors().reason) ? (
                  <Alert type="error" showIcon title={isApiError(mutation.error) && mutation.error.code === 'INVALID_STATE' ? 'Bạn đã có yêu cầu đang chờ duyệt.' : mutation.error.message} />
                ) : null}
                <Form<{ reason: string }>
                  form={form}
                  name="teacher-request"
                  layout="vertical"
                  requiredMark={false}
                  disabled={mutation.isPending}
                  onValuesChange={() => mutation.reset()}
                  onFinish={(values) => {
                    if (!mutation.isPending) mutation.mutate(values.reason.trim())
                  }}
                >
                  <Form.Item name="reason" label="Lý do muốn trở thành giảng viên" extra="Tối đa 4000 ký tự." rules={[
                    { required: true, whitespace: true, message: 'Vui lòng nhập lý do muốn trở thành giảng viên.' },
                    { max: 4000, message: 'Lý do tối đa 4000 ký tự.' },
                  ]}>
                    <Input.TextArea rows={4} showCount placeholder="Chuyên môn, kinh nghiệm giảng dạy hoặc khóa học bạn muốn mở…" />
                  </Form.Item>
                  <div className="account-actions">
                    <Button type="primary" htmlType="submit" loading={mutation.isPending} aria-label="Gửi yêu cầu" aria-busy={mutation.isPending}>Gửi yêu cầu</Button>
                  </div>
                </Form>
              </>
            )}
          </div>
        </Card>
      )}
      <Card title="Lịch sử yêu cầu" extra={<Button onClick={refresh} loading={history.isFetching} aria-label="Làm mới" aria-busy={history.isFetching}>Làm mới</Button>}>
        {history.isPending ? <LoadingState /> : history.isError ? (
          <ErrorState error={history.error} onRetry={refresh} />
        ) : (
          <div className="account-stack" aria-busy={history.isFetching}>
            {history.data.items.length === 0 ? <EmptyState description="Bạn chưa gửi yêu cầu trở thành giảng viên." /> : history.data.items.map((request) => (
              <article className="account-history" key={request.id}>
                <div className="account-history-heading">
                  <div><b>Gửi ngày {formatDateTime(request.created_at)}</b><div className="account-id">Mã yêu cầu: {request.id}</div></div>
                  <TeacherRequestStatusTag status={request.status} />
                </div>
                <p className="account-note">{request.reason}</p>
                {request.status !== 'PENDING' && (
                  <div className="account-stack">
                    <span className="account-muted">Quyết định ngày {formatDateTime(request.decided_at)}</span>
                    <Alert type={request.status === 'APPROVED' ? 'success' : 'info'} showIcon title="Ghi chú của quản trị viên" description={<span className="account-note">{request.decision_note || 'Không có ghi chú.'}</span>} />
                  </div>
                )}
              </article>
            ))}
            {history.data.total > 0 && (
              <Pagination
                current={page}
                pageSize={pageSize}
                total={history.data.total}
                showSizeChanger
                pageSizeOptions={[10, 20, 50, 100]}
                showTotal={(total) => `${total} yêu cầu`}
                disabled={history.isFetching}
                onChange={(nextPage, nextSize) => {
                  setPage(nextSize === pageSize ? nextPage : 1)
                  setPageSize(nextSize)
                }}
              />
            )}
          </div>
        )}
      </Card>
    </>
  )
}
