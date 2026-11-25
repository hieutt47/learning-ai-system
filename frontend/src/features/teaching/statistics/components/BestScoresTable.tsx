import { UserOutlined } from '@ant-design/icons'
import { Card, Space, Table, Tag, Typography, type TableProps } from 'antd'
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
        <Space size={8}>
          <UserOutlined style={{ color: 'var(--text-2)' }} />
          <div>
            <strong>{studentName}</strong>
            <div style={{ fontSize: 12, color: 'var(--text-2)' }}>Mã SV: #{row.studentId}</div>
          </div>
        </Space>
      ),
    },
    {
      title: 'Số lượt nộp',
      dataIndex: 'attemptCount',
      width: 125,
      align: 'center',
      render: (value: number) => formatCount(value),
    },
    {
      title: 'Điểm cao nhất',
      dataIndex: 'score',
      width: 240,
      render: (score: number) => {
        const pct = Math.max(0, Math.min(100, Number(score) || 0))
        const barColor = pct >= 80 ? 'var(--success)' : pct >= 50 ? 'var(--warning)' : 'var(--danger)'
        return (
          <div className="stats-score-cell">
            <Tag color={scoreBadgeColor(score)} style={{ fontWeight: 600, margin: 0 }}>
              {formatScore(score)} / 100
            </Tag>
            <span className="stats-score-mini" aria-hidden="true">
              <i style={{ width: `${pct}%`, background: barColor }} />
            </span>
          </div>
        )
      },
    },
    {
      title: 'Nộp gần nhất',
      dataIndex: 'latestSubmittedAt',
      width: 165,
      render: (value: string | null) => (value ? formatDateTime(value) : '—'),
    },
  ]

  return (
    <Card
      className="page-table-card"
      title={
        <div>
          <span>Điểm cao nhất theo học viên</span>
          <Typography.Paragraph
            type="secondary"
            style={{ margin: 0, fontWeight: 'normal', fontSize: 13, whiteSpace: 'normal' }}
          >
            Lượt nộp có điểm cao nhất của mỗi học viên (tính trên các lượt đã nộp).
          </Typography.Paragraph>
        </div>
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
          scroll={{ x: 720 }}
        />
      )}
    </Card>
  )
}
