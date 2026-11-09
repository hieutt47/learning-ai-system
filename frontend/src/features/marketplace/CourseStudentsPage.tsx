import { useQuery } from '@tanstack/react-query'
import { Avatar, Card, Input, Space, Table, Typography, type TableProps } from 'antd'
import { useState } from 'react'
import { useParams } from 'react-router'
import { EmptyState, ErrorState, LoadingState } from '../../shared/components/PageState'
import { courseKeys, getMembers } from './course.api'
import { formatDateTime } from './format'
import type { CourseMember } from './course.types'

const PAGE_SIZE = 20

export function CourseStudentsPage() {
  const { courseId = '' } = useParams()
  const [page, setPage] = useState(1)
  const [query, setQuery] = useState('')
  const membersQuery = useQuery({
    queryKey: courseKeys.members(courseId, page),
    queryFn: () => getMembers(courseId, { page, page_size: PAGE_SIZE }),
    enabled: Boolean(courseId),
  })
  if (membersQuery.isLoading) return <LoadingState tip="Đang tải học viên…" />
  if (membersQuery.isError) return <ErrorState error={membersQuery.error} onRetry={() => void membersQuery.refetch()} />
  const members = membersQuery.data?.items ?? []
  const total = membersQuery.data?.total ?? 0
  const normalized = query.trim().toLowerCase()
  const visible = normalized ? members.filter((member) => member.fullName.toLowerCase().includes(normalized)) : members
  const columns: TableProps<CourseMember>['columns'] = [
    {
      title: 'Học viên',
      dataIndex: 'fullName',
      render: (name: string) => (
        <Space>
          <Avatar>{name.trim().charAt(0).toUpperCase()}</Avatar>
          <strong>{name}</strong>
        </Space>
      ),
    },
    {
      title: 'Ngày ghi danh',
      dataIndex: 'enrolledAt',
      width: 200,
      sorter: (a, b) => a.enrolledAt.localeCompare(b.enrolledAt),
      defaultSortOrder: 'ascend',
      render: (value: string) => formatDateTime(value),
    },
  ]
  return (
    <Card
      title={`Học viên (${total})`}
      extra={
        <Input.Search
          allowClear
          placeholder="Tìm theo tên trong trang này"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          style={{ width: 240 }}
        />
      }
    >
      {total === 0 ? (
        <EmptyState description="Khóa học chưa có học viên nào. Học viên sẽ xuất hiện sau khi ghi danh." />
      ) : (
        <>
          <Table<CourseMember>
            rowKey="studentId"
            columns={columns}
            dataSource={visible}
            pagination={{
              current: page,
              pageSize: PAGE_SIZE,
              total,
              showSizeChanger: false,
              onChange: setPage,
            }}
            locale={{ emptyText: <EmptyState description="Không có học viên khớp tên đã nhập trong trang này." /> }}
          />
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            Danh sách không có email: chủ khóa chỉ cần biết ai đang học.
          </Typography.Text>
        </>
      )}
    </Card>
  )
}
