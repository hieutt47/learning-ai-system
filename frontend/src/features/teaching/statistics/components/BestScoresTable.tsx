import { Card, Table, Tag, Typography, type TableProps } from 'antd'
import { EmptyState } from '../../../../shared/components/PageState'
import { formatDateTime } from '../../../../shared/utils/format'
import { formatCount, formatScore, scoreBadgeColor } from '../statistics.format'
import type { StudentBestScore } from '../statistics.types'

interface BestScoresTableProps {
  bestScores: readonly StudentBestScore[]
}

export function BestScoresTable({ bestScores }: BestScoresTableProps) {
  const columns: TableProps<StudentBestScore>['columns'] = [
    {
      title: '#',
      key: 'rank',
      width: 64,
      align: 'center',
      render: (_, __, index) => index + 1,
    },
    {
      title: 'Học viên',
      dataIndex: 'studentName',
      render: (studentName: string, row) => (
        <div>
          <strong>{studentName}</strong>
          <div style={{ fontSize: 12, color: 'var(--text-3)' }}>Mã SV: #{row.studentId}</div>
        </div>
      ),
    },
    {
      title: 'Số lượt nộp',
      dataIndex: 'attemptCount',
      width: 130,
      align: 'center',
      render: (value: number) => formatCount(value),
    },
    {
      title: 'Điểm cao nhất',
      dataIndex: 'score',
      width: 150,
      align: 'center',
      render: (value: number) => <Tag color={scoreBadgeColor(value)}>{formatScore(value)} / 100</Tag>,
    },
    {
      title: 'Nộp gần nhất',
      dataIndex: 'latestSubmittedAt',
      width: 170,
      render: (value: string | null) => (value ? formatDateTime(value) : '—'),
    },
  ]

  return (
    <Card
      title="Điểm cao nhất theo học viên"
      extra={
        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
          Lượt nộp có điểm cao nhất của mỗi học viên
        </Typography.Text>
      }
    >
      {bestScores.length === 0 ? (
        <EmptyState description="Chưa có học viên nào nộp bài cho quiz này." />
      ) : (
        <Table<StudentBestScore>
          rowKey="studentId"
          columns={columns}
          dataSource={[...bestScores]}
          pagination={bestScores.length > 10 ? { pageSize: 10, showSizeChanger: false } : false}
          scroll={{ x: 680 }}
        />
      )}
    </Card>
  )
}
