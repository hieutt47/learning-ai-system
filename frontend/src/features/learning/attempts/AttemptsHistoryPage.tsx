import { EyeOutlined, PlayCircleOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Button, Card, Space, Table, Tag, Typography, type TableProps } from 'antd'
import { useState } from 'react'
import { useNavigate } from 'react-router'
import { EmptyState, ErrorState, LoadingState } from '../../../shared/components/PageState'
import { usePageHeader } from '../../../shared/components/pageHeader'
import { paths } from '../../../shared/routing/paths'
import { formatDateTime } from '../../../shared/utils/format'
import { attemptKeys, getMyAttempts } from './attempts.api'
import type { AttemptStatus, AttemptSummaryItem } from './attempts.types'

const PAGE_SIZE = 10

export function AttemptsHistoryPage() {
  const navigate = useNavigate()
  const [page, setPage] = useState(1)

  usePageHeader({
    title: 'Lịch sử làm bài',
    crumbs: [{ label: 'Trang chủ', to: paths.home }, { label: 'Lịch sử làm bài' }],
  })

  const attemptsQuery = useQuery({
    queryKey: attemptKeys.mine({ page, page_size: PAGE_SIZE }),
    queryFn: () => getMyAttempts({ page, page_size: PAGE_SIZE }),
  })

  if (attemptsQuery.isLoading) return <LoadingState tip="Đang tải lịch sử làm bài…" />
  if (attemptsQuery.isError) {
    return (
      <ErrorState
        error={attemptsQuery.error}
        onRetry={() => void attemptsQuery.refetch()}
      />
    )
  }

  const items = attemptsQuery.data?.items ?? []
  const total = attemptsQuery.data?.total ?? 0

  const statusTag = (status: AttemptStatus) => {
    switch (status) {
      case 'SUBMITTED':
        return <Tag color="success">Đã nộp</Tag>
      case 'IN_PROGRESS':
        return <Tag color="processing">Đang làm</Tag>
    }
  }

  const scoreTag = (score?: number | null) => {
    if (score == null) return <span style={{ color: 'rgba(0,0,0,0.25)' }}>—</span>
    let color = 'error'
    if (score >= 80) color = 'success'
    else if (score >= 50) color = 'warning'
    return <Tag color={color} style={{ fontWeight: 600 }}>{score} / 100</Tag>
  }

  const columns: TableProps<AttemptSummaryItem>['columns'] = [
    {
      title: '#',
      key: 'index',
      width: 60,
      align: 'center',
      render: (_, __, index) => (page - 1) * PAGE_SIZE + index + 1,
    },
    {
      title: 'Bài kiểm tra',
      dataIndex: 'quizTitle',
      render: (title: string, record) => (
        <Button
          type="link"
          style={{ padding: 0 }}
          onClick={() => {
            if (record.status === 'SUBMITTED') {
              navigate(paths.attemptResult(record.id))
            } else {
              navigate(paths.takeQuiz(record.quizId))
            }
          }}
        >
          {title}
        </Button>
      ),
    },
    {
      title: 'Khóa học',
      dataIndex: 'courseTitle',
      render: (title?: string) => title || <span style={{ color: 'rgba(0,0,0,0.25)' }}>—</span>,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      width: 130,
      align: 'center',
      render: (status: AttemptStatus) => statusTag(status),
    },
    {
      title: 'Điểm',
      dataIndex: 'score',
      width: 120,
      align: 'center',
      render: (score?: number | null) => scoreTag(score),
    },
    {
      title: 'Bắt đầu lúc',
      dataIndex: 'startedAt',
      width: 160,
      render: (date: string) => formatDateTime(date),
    },
    {
      title: 'Nộp lúc',
      dataIndex: 'submittedAt',
      width: 160,
      render: (date?: string | null) => formatDateTime(date ?? null),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 130,
      render: (_, record) => (
        <Space>
          {record.status === 'SUBMITTED' ? (
            <Button
              type="text"
              icon={<EyeOutlined />}
              onClick={() => navigate(paths.attemptResult(record.id))}
            >
              Xem kết quả
            </Button>
          ) : (
            <Button
              type="primary"
              size="small"
              icon={<PlayCircleOutlined />}
              onClick={() => navigate(paths.takeQuiz(record.quizId))}
            >
              Làm tiếp
            </Button>
          )}
        </Space>
      ),
    },
  ]

  return (
    <Card
      title={
        <div>
          <span>Lịch sử làm bài ({total})</span>
          <Typography.Paragraph
            type="secondary"
            style={{ marginBottom: 0, fontWeight: 'normal', fontSize: 13 }}
          >
            Theo dõi tất cả các bài quiz bạn đã tham gia và điểm số đạt được.
          </Typography.Paragraph>
        </div>
      }
    >
      {items.length === 0 ? (
        <EmptyState
          description="Chưa có lần làm bài nào. Khi bạn làm quiz, mỗi lần làm sẽ xuất hiện ở đây."
          action={
            <Button type="primary" onClick={() => navigate(paths.myCourses)}>
              Đến khóa học của tôi
            </Button>
          }
        />
      ) : (
        <Table<AttemptSummaryItem>
          rowKey="id"
          columns={columns}
          dataSource={items}
          pagination={{
            current: page,
            pageSize: PAGE_SIZE,
            total,
            onChange: (p) => setPage(p),
            showTotal: (t) => `Tổng cộng ${t} lần làm bài`,
          }}
        />
      )}
    </Card>
  )
}
