import {
  BookOutlined,
  CheckCircleOutlined,
  FileDoneOutlined,
  ReloadOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Button, Card, Typography } from 'antd'
import { ErrorState, LoadingState } from '../../../shared/components/PageState'
import { usePageHeader } from '../../../shared/components/pageHeader'
import { formatDateTime } from '../../../shared/utils/format'
import { KpiCard } from './components/KpiCard'
import { getSystemStatistics, statisticsKeys } from './statistics.api'
import { formatCount } from './statistics.format'
import './statistics.css'

export function SystemStatisticsPage() {
  usePageHeader({ title: 'Thống kê hệ thống' })
  const statsQuery = useQuery({
    queryKey: statisticsKeys.system,
    queryFn: getSystemStatistics,
  })

  if (statsQuery.isLoading) {
    return <LoadingState tip="Đang tải thống kê hệ thống…" />
  }
  if (statsQuery.isError) {
    return <ErrorState error={statsQuery.error} onRetry={() => void statsQuery.refetch()} />
  }
  if (!statsQuery.data) {
    return null
  }

  const data = statsQuery.data
  const totalUsers = data.activeUsers
  const teacherCount = Math.min(data.teachers, totalUsers)
  const otherUsers = Math.max(0, totalUsers - teacherCount)
  const teacherPct = totalUsers > 0 ? (teacherCount / totalUsers) * 100 : 0
  const otherPct = totalUsers > 0 ? (otherUsers / totalUsers) * 100 : 0

  return (
    <div className="stats-page">
      <div className="stats-toolbar">
        <span className="stats-updated">
          Cập nhật lúc {formatDateTime(data.generatedAt)} (giờ Việt Nam)
        </span>
        <Button
          icon={<ReloadOutlined />}
          loading={statsQuery.isFetching}
          onClick={() => void statsQuery.refetch()}
        >
          Làm mới
        </Button>
      </div>

      <div className="stats-kpi-grid">
        <KpiCard
          label="Tài khoản hoạt động"
          value={formatCount(data.activeUsers)}
          icon={<TeamOutlined />}
          tone="primary"
        />
        <KpiCard
          label="Giảng viên"
          value={formatCount(data.teachers)}
          icon={<UserOutlined />}
          tone="info"
        />
        <KpiCard
          label="Khóa học đang xuất bản"
          value={formatCount(data.publishedCourses)}
          icon={<BookOutlined />}
          tone="success"
        />
        <KpiCard
          label="Lượt ghi danh"
          value={formatCount(data.enrollments)}
          icon={<CheckCircleOutlined />}
          tone="warning"
        />
        <KpiCard
          label="Lượt nộp bài"
          value={formatCount(data.submittedAttempts)}
          icon={<FileDoneOutlined />}
          tone="primary"
        />
      </div>

      <Card
        title={
          <div>
            <span>Cơ cấu người dùng hoạt động</span>
            <Typography.Paragraph
              type="secondary"
              style={{ margin: 0, fontWeight: 'normal', fontSize: 13, whiteSpace: 'normal' }}
            >
              Tỷ lệ giảng viên trên tổng số tài khoản đang hoạt động.
            </Typography.Paragraph>
          </div>
        }
      >
        <div className="stats-donut-layout">
          <div
            className="stats-donut"
            role="img"
            aria-label={`Giảng viên chiếm ${teacherPct.toFixed(1)}%`}
            style={{
              background:
                totalUsers > 0
                  ? `conic-gradient(var(--primary) ${teacherPct * 3.6}deg, var(--info) 0)`
                  : 'var(--neutral-soft)',
            }}
          >
            <div className="stats-donut-inner">
              <strong>{formatCount(totalUsers)}</strong>
              <span>đang hoạt động</span>
            </div>
          </div>

          <div className="stats-legend">
            <div className="stats-legend-item">
              <i className="stats-dot stats-dot-info" />
              <span>Sinh viên &amp; quản trị khác:</span>
              <strong>
                {formatCount(otherUsers)} · {otherPct.toFixed(1).replace('.', ',')}%
              </strong>
            </div>
            <div className="stats-legend-item">
              <i className="stats-dot stats-dot-primary" />
              <span>Giảng viên:</span>
              <strong>
                {formatCount(data.teachers)} · {teacherPct.toFixed(1).replace('.', ',')}%
              </strong>
            </div>
          </div>
        </div>
      </Card>

      <Card
        title={
          <div>
            <span>Cách tính số liệu</span>
            <Typography.Paragraph
              type="secondary"
              style={{ margin: 0, fontWeight: 'normal', fontSize: 13, whiteSpace: 'normal' }}
            >
              Số liệu tổng hợp tức thời tại thời điểm cập nhật.
            </Typography.Paragraph>
          </div>
        }
      >
        <dl className="stats-dl">
          <dt>Tài khoản hoạt động</dt>
          <dd>Tổng số tài khoản đang ở trạng thái hoạt động (không bị khóa) trên hệ thống.</dd>
          <dt>Giảng viên</dt>
          <dd>Tổng số tài khoản có vai trò Giảng viên trong hệ thống.</dd>
          <dt>Khóa học đang xuất bản</dt>
          <dd>Khóa học đang xuất bản và chưa lưu trữ (không tính bản nháp hoặc đã lưu trữ).</dd>
          <dt>Lượt ghi danh</dt>
          <dd>Tổng số lượt sinh viên đã ghi danh vào các khóa học.</dd>
          <dt>Lượt nộp bài</dt>
          <dd>Tổng số lượt nộp quiz đã hoàn tất (trạng thái SUBMITTED) trên toàn hệ thống.</dd>
        </dl>
      </Card>
    </div>
  )
}
