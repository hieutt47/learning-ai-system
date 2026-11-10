import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Alert, App, Button, Card, Descriptions, Form, Input } from 'antd'
import { isApiError } from '../../shared/api/client'
import { useCurrentUser } from '../../shared/auth/currentUser'
import { formatDateTime } from '../../shared/utils/format'
import { decideTeacherRequest } from '../teacher-requests/api'
import { TeacherRequestStatusTag } from '../teacher-requests/TeacherRequestStatusTag'
import type { TeacherDecision, TeacherRequest } from '../teacher-requests/types'

export function TeacherDecisionForm({ request, onBusyChange, onResolved }: { request: TeacherRequest; onBusyChange: (busy: boolean) => void; onResolved: () => void }) {
  const [form] = Form.useForm<{ decision_note: string }>()
  const { user } = useCurrentUser()
  const { message } = App.useApp()
  const queryClient = useQueryClient()
  const isSelf = request.user_id === user?.id
  const mutation = useMutation({
    mutationFn: (decision: TeacherDecision) => decideTeacherRequest(request.id, decision),
    onMutate: () => onBusyChange(true),
    onSuccess: async (updated) => {
      onResolved()
      void message.success(updated.status === 'APPROVED' ? 'Đã duyệt yêu cầu trở thành giảng viên' : 'Đã từ chối yêu cầu')
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['teacher-requests'] }),
        queryClient.invalidateQueries({ queryKey: ['users', 'list'] }),
      ])
    },
    onError: (error) => {
      if (isApiError(error)) {
        const noteError = error.fieldErrors().decision_note
        if (noteError) form.setFields([{ name: 'decision_note', errors: [noteError] }])
        if (error.status === 404 || error.code === 'INVALID_STATE') {
          onResolved()
          void message.warning('Yêu cầu đã thay đổi hoặc người dùng không còn đủ điều kiện. Vui lòng kiểm tra trạng thái mới.')
          void queryClient.invalidateQueries({ queryKey: ['teacher-requests'] })
        }
      }
    },
    onSettled: () => onBusyChange(false),
  })

  async function decide(status: TeacherDecision['status']): Promise<void> {
    if (mutation.isPending || isSelf) return
    try {
      const values = await form.validateFields()
      const note = values.decision_note?.trim() || null
      if (status === 'REJECTED' && !note) {
        form.setFields([{ name: 'decision_note', errors: ['Nhập ghi chú khi từ chối để người gửi biết cần bổ sung gì.'] }])
        return
      }
      mutation.mutate({ status, decision_note: note })
    } catch {
      return
    }
  }

  return (
    <Card title="Chi tiết yêu cầu" extra={<TeacherRequestStatusTag status={request.status} />}>
      <div className="account-stack">
        <Descriptions column={1} size="small" items={[
          { key: 'id', label: 'Mã yêu cầu', children: <span className="account-id">{request.id}</span> },
          { key: 'user', label: 'Mã người gửi', children: <span className="account-id">{request.user_id}</span> },
          { key: 'created', label: 'Ngày gửi', children: formatDateTime(request.created_at) },
        ]} />
        <div><h3>Lý do</h3><p className="account-note">{request.reason}</p></div>
        {request.status === 'PENDING' ? (
          <>
            {isSelf && <Alert type="warning" showIcon title="Bạn không thể tự xét duyệt yêu cầu của mình." />}
            {mutation.error && !(isApiError(mutation.error) && mutation.error.fieldErrors().decision_note) ? (
              <Alert type="error" showIcon title={isApiError(mutation.error) && mutation.error.code === 'INVALID_STATE' ? 'Không thể xử lý yêu cầu ở trạng thái hiện tại. Vui lòng làm mới danh sách.' : mutation.error.message} />
            ) : null}
            <Form<{ decision_note: string }>
              form={form}
              name="teacher-decision"
              layout="vertical"
              disabled={mutation.isPending || isSelf}
              onValuesChange={() => mutation.reset()}
            >
              <Form.Item name="decision_note" label="Ghi chú quyết định" extra="Không bắt buộc khi duyệt. Cần nhập khi từ chối. Tối đa 4000 ký tự." rules={[
                { max: 4000, message: 'Ghi chú tối đa 4000 ký tự.' },
              ]}>
                <Input.TextArea rows={5} showCount placeholder="Ghi chú để người gửi biết kết quả xem xét…" />
              </Form.Item>
              <div className="account-actions">
                <Button danger disabled={mutation.isPending || isSelf} onClick={() => void decide('REJECTED')}>Từ chối</Button>
                <Button type="primary" loading={mutation.isPending} disabled={mutation.isPending || isSelf} aria-label="Duyệt yêu cầu" aria-busy={mutation.isPending} onClick={() => void decide('APPROVED')}>Duyệt</Button>
              </div>
            </Form>
          </>
        ) : (
          <>
            <Descriptions column={1} size="small" items={[
              { key: 'decided', label: 'Ngày quyết định', children: formatDateTime(request.decided_at) },
              { key: 'admin', label: 'Mã người duyệt', children: request.decided_by || '—' },
            ]} />
            <Alert type={request.status === 'APPROVED' ? 'success' : 'info'} showIcon title="Ghi chú quyết định" description={<span className="account-note">{request.decision_note || 'Không có ghi chú.'}</span>} />
            <p className="account-description">Yêu cầu đã được xử lý nên chỉ xem được.</p>
          </>
        )}
      </div>
    </Card>
  )
}
