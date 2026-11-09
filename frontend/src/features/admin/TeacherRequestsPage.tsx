import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Button, Card, Select, Table, type TableColumnsType } from 'antd'
import { useState } from 'react'
import type { Id } from '../../shared/api/types'
import { EmptyState, ErrorState, LoadingState } from '../../shared/components/PageState'
import { Icon } from '../../shared/components/Icon'
import '../../shared/styles/management.css'
import { formatDateTime } from '../../shared/utils/format'
import { getTeacherRequests } from '../teacher-requests/api'
import { TeacherRequestStatusTag } from '../teacher-requests/TeacherRequestStatusTag'
import { TEACHER_REQUEST_STATUS_LABEL, type TeacherRequest, type TeacherRequestParams, type TeacherRequestStatus } from '../teacher-requests/types'
import { TeacherDecisionForm } from './TeacherDecisionForm'

export function TeacherRequestsPage() {
  const [status, setStatus] = useState<TeacherRequestStatus | 'ALL'>('PENDING')
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [sort, setSort] = useState('created_at,desc')
  const [selectedId, setSelectedId] = useState<Id | null>(null)
  const [isDeciding, setIsDeciding] = useState(false)
  const params: TeacherRequestParams = { page, page_size: pageSize, sort, status: status === 'ALL' ? undefined : status }
  const query = useQuery({
    queryKey: ['teacher-requests', 'admin', params],
    queryFn: ({ signal }) => getTeacherRequests(params, signal),
    placeholderData: keepPreviousData,
  })
  const selected = query.data?.items.find((request) => request.id === selectedId) ?? query.data?.items[0] ?? null

  const columns: TableColumnsType<TeacherRequest> = [
    { title: 'Người gửi', dataIndex: 'user_id', width: 180, render: (id: Id) => <div><b>Người dùng</b><div className="account-id">Mã: {id}</div></div> },
    { title: 'Lý do', dataIndex: 'reason', width: 250, ellipsis: true },
    { title: 'Trạng thái', dataIndex: 'status', width: 130, render: (value: TeacherRequestStatus) => <TeacherRequestStatusTag status={value} /> },
    { title: 'Ngày gửi', dataIndex: 'created_at', width: 165, render: formatDateTime },
    { title: 'Thao tác', key: 'actions', width: 120, fixed: 'right', render: (_, request) => <Button type="link" disabled={isDeciding || query.isPlaceholderData} onClick={() => setSelectedId(request.id)}>Xem chi tiết</Button> },
  ]

  return (
    <div className="account-page">
      <p className="account-description">Xem lý do và lịch sử xét duyệt của sinh viên. Chọn một yêu cầu để duyệt hoặc từ chối.</p>
      <div className="account-toolbar">
        <div className="account-controls">
          <Select<TeacherRequestStatus | 'ALL'> aria-label="Trạng thái yêu cầu" value={status} disabled={isDeciding} options={[
            { value: 'ALL', label: 'Tất cả trạng thái' },
            ...(['PENDING', 'APPROVED', 'REJECTED'] as const).map((value) => ({ value, label: TEACHER_REQUEST_STATUS_LABEL[value] })),
          ]} onChange={(value) => { setStatus(value); setPage(1); setSelectedId(null) }} />
          <Select aria-label="Sắp xếp yêu cầu" value={sort} disabled={isDeciding} options={[
            { value: 'created_at,desc', label: 'Mới nhất' },
            { value: 'created_at,asc', label: 'Cũ nhất' },
          ]} onChange={(value) => { setSort(value); setPage(1); setSelectedId(null) }} />
        </div>
        <Button icon={<Icon name="refresh" />} loading={query.isFetching} disabled={isDeciding} aria-label="Làm mới" aria-busy={query.isFetching} onClick={() => void query.refetch()}>Làm mới</Button>
      </div>
      {query.isPending ? <LoadingState /> : query.isError ? <ErrorState error={query.error} onRetry={() => void query.refetch()} /> : (
        <div className="account-request-grid">
          <Card title="Yêu cầu trở thành giảng viên">
            <Table<TeacherRequest>
              rowKey="id"
              columns={columns}
              dataSource={query.data.items}
              loading={query.isFetching}
              scroll={{ x: 845 }}
              rowClassName={(request) => `account-request-row ${request.id === selected?.id ? 'account-selected-row' : ''}`}
              onRow={(request) => ({ onClick: () => { if (!isDeciding && !query.isPlaceholderData) setSelectedId(request.id) } })}
              locale={{ emptyText: <EmptyState description={status === 'PENDING' ? 'Không còn yêu cầu đang chờ.' : 'Chưa có yêu cầu trong trạng thái này.'} /> }}
              pagination={{
                current: page, pageSize, total: query.data.total, showSizeChanger: true, pageSizeOptions: [10, 20, 50, 100],
                showTotal: (total) => `${total} yêu cầu`, disabled: isDeciding || query.isFetching,
                onChange: (nextPage, nextSize) => { setPage(nextSize === pageSize ? nextPage : 1); setPageSize(nextSize); setSelectedId(null) },
              }}
            />
          </Card>
          <aside>
            {selected && !query.isPlaceholderData ? <TeacherDecisionForm key={selected.id} request={selected} onBusyChange={setIsDeciding} onResolved={() => {
              setSelectedId(null)
              if (status === 'PENDING' && query.data.items.length === 1 && page > 1) setPage(page - 1)
            }} /> : (
              <Card><EmptyState description="Chọn một yêu cầu để xem chi tiết." /></Card>
            )}
          </aside>
        </div>
      )}
    </div>
  )
}
