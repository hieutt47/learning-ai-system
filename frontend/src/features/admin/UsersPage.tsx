import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, App, Button, Card, Modal, Select, Table, Tag, Tooltip, type TableColumnsType } from 'antd'
import { useState } from 'react'
import { isApiError } from '../../shared/api/client'
import { useCurrentUser, ROLE_LABEL } from '../../shared/auth/currentUser'
import type { User } from '../../shared/auth/types'
import { Avatar } from '../../shared/components/AppLayout'
import { EmptyState, ErrorState, LoadingState } from '../../shared/components/PageState'
import { Icon } from '../../shared/components/Icon'
import '../../shared/styles/management.css'
import { formatDateTime } from '../../shared/utils/format'
import { getUsers, updateUserStatus } from '../auth/api'

export function UsersPage() {
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [sort, setSort] = useState('created_at,desc')
  const [target, setTarget] = useState<User | null>(null)
  const { user: currentUser } = useCurrentUser()
  const { message } = App.useApp()
  const queryClient = useQueryClient()
  const params = { page, page_size: pageSize, sort }
  const query = useQuery({
    queryKey: ['users', 'list', params],
    queryFn: ({ signal }) => getUsers(params, signal),
    placeholderData: keepPreviousData,
  })
  const mutation = useMutation({
    mutationFn: (user: User) => updateUserStatus(user.id, !user.is_active),
    onSuccess: async (updated) => {
      setTarget(null)
      void message.success(updated.is_active ? 'Đã mở khóa tài khoản' : 'Đã khóa tài khoản')
      await queryClient.invalidateQueries({ queryKey: ['users', 'list'] })
    },
    onError: (error) => {
      if (isApiError(error) && error.status === 404) {
        setTarget(null)
        setPage(1)
        void message.warning('Người dùng không còn tồn tại. Vui lòng kiểm tra danh sách mới.')
        void queryClient.invalidateQueries({ queryKey: ['users', 'list'] })
      }
    },
  })

  const columns: TableColumnsType<User> = [
    {
      title: 'Họ và tên', key: 'user', width: 290,
      render: (_, user) => (
        <div className="account-person">
          <Avatar name={user.full_name} />
          <div className="account-person-info">
            <b>{user.full_name}{user.id === currentUser?.id ? ' (bạn)' : ''}</b>
            <small>{user.email}</small>
            <small>Mã: {user.id}</small>
          </div>
        </div>
      ),
    },
    { title: 'Vai trò', dataIndex: 'role', width: 120, render: (role: User['role']) => <Tag color={role === 'ADMIN' ? 'warning' : role === 'TEACHER' ? 'processing' : 'default'}>{ROLE_LABEL[role]}</Tag> },
    { title: 'Trạng thái', dataIndex: 'is_active', width: 130, render: (active: boolean) => <Tag color={active ? 'success' : 'error'}>{active ? 'Hoạt động' : 'Bị khóa'}</Tag> },
    { title: 'Ngày tạo', dataIndex: 'created_at', width: 165, render: formatDateTime },
    {
      title: 'Hành động', key: 'actions', width: 125, fixed: 'right',
      render: (_, user) => (
        <Tooltip title={user.id === currentUser?.id ? 'Bạn không thể tự khóa tài khoản của mình.' : undefined}>
          <Button
            size="small"
            icon={<Icon name={user.is_active ? 'lock' : 'unlock'} />}
            disabled={user.id === currentUser?.id || mutation.isPending || query.isPlaceholderData}
            onClick={() => { mutation.reset(); setTarget(user) }}
          >{user.is_active ? 'Khóa' : 'Mở khóa'}</Button>
        </Tooltip>
      ),
    },
  ]

  return (
    <div className="account-page">
      <p className="account-description">Quản lý tài khoản và trạng thái truy cập. Vai trò Giảng viên được cấp qua yêu cầu ở mục Duyệt giảng viên.</p>
      <Card>
        <div className="account-toolbar">
          <h2>Tất cả người dùng</h2>
          <div className="account-controls">
            <Select aria-label="Sắp xếp người dùng" value={sort} disabled={mutation.isPending} options={[
              { value: 'created_at,desc', label: 'Mới nhất' },
              { value: 'created_at,asc', label: 'Cũ nhất' },
              { value: 'full_name,asc', label: 'Họ tên A–Z' },
              { value: 'full_name,desc', label: 'Họ tên Z–A' },
            ]} onChange={(value) => { setSort(value); setPage(1) }} />
            <Button icon={<Icon name="refresh" />} loading={query.isFetching} disabled={mutation.isPending} aria-label="Làm mới" aria-busy={query.isFetching} onClick={() => void query.refetch()}>Làm mới</Button>
          </div>
        </div>
        {query.isPending ? <LoadingState /> : query.isError ? <ErrorState error={query.error} onRetry={() => void query.refetch()} /> : (
          <Table<User>
            rowKey="id"
            columns={columns}
            dataSource={query.data.items}
            loading={query.isFetching}
            scroll={{ x: 830 }}
            locale={{ emptyText: <EmptyState description="Chưa có người dùng." /> }}
            pagination={{
              current: page, pageSize, total: query.data.total, showSizeChanger: true, pageSizeOptions: [10, 20, 50, 100],
              showTotal: (total) => `${total} người dùng`,
              disabled: mutation.isPending || query.isFetching,
              onChange: (nextPage, nextSize) => { setPage(nextSize === pageSize ? nextPage : 1); setPageSize(nextSize) },
            }}
          />
        )}
      </Card>
      <Modal
        open={target !== null}
        title={target?.is_active ? 'Khóa tài khoản?' : 'Mở khóa tài khoản?'}
        okText={target?.is_active ? 'Khóa tài khoản' : 'Mở khóa tài khoản'}
        cancelText="Hủy"
        confirmLoading={mutation.isPending}
        okButtonProps={{ danger: target?.is_active, disabled: mutation.isPending, 'aria-label': target?.is_active ? 'Khóa tài khoản' : 'Mở khóa tài khoản', 'aria-busy': mutation.isPending }}
        cancelButtonProps={{ disabled: mutation.isPending }}
        closable={!mutation.isPending}
        mask={{ closable: !mutation.isPending }}
        keyboard={!mutation.isPending}
        onCancel={() => { if (!mutation.isPending) setTarget(null) }}
        onOk={() => { if (target && !mutation.isPending) mutation.mutate(target) }}
      >
        <div className="account-stack">
          <p>{target?.is_active ? `Tài khoản ${target.full_name} sẽ bị thu hồi phiên đăng nhập và không thể truy cập cho đến khi được mở khóa.` : `Cho phép ${target?.full_name} đăng nhập trở lại.`}</p>
          {mutation.error && <Alert type="error" showIcon title={mutation.error.message} />}
        </div>
      </Modal>
    </div>
  )
}
