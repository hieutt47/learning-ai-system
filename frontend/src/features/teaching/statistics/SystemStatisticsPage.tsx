import { ReloadOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Button, Card, Col, Descriptions, Row, Space, Statistic, Typography } from 'antd'
import { ErrorState, LoadingState } from '../../../shared/components/PageState'
import { usePageHeader } from '../../../shared/components/pageHeader'
import { formatDateTime } from '../../../shared/utils/format'
import { getSystemStatistics, statisticsKeys } from './statistics.api'
import { formatCount } from './statistics.format'

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

  return (
    <Space orientation="vertical" size={20} style={{ width: '100%' }}>
      <Row justify="space-between" align="middle">
        <Col>
          <Typography.Text type="secondary">
            Cập nhật lúc {formatDateTime(data.generatedAt)} (giờ Việt Nam)
          </Typography.Text>
        </Col>
        <Col>
          <Button
            icon={<ReloadOutlined />}
            loading={statsQuery.isFetching}
            onClick={() => void statsQuery.refetch()}
          >
            Làm mới
          </Button>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={8} xl={4}>
          <Card>
            <Statistic title="Tài khoản hoạt động" value={formatCount(data.activeUsers)} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8} xl={5}>
          <Card>
            <Statistic title="Giảng viên" value={formatCount(data.teachers)} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8} xl={5}>
          <Card>
            <Statistic title="Khóa đang xuất bản" value={formatCount(data.publishedCourses)} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={12} xl={5}>
          <Card>
            <Statistic title="Lượt ghi danh" value={formatCount(data.enrollments)} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={12} xl={5}>
          <Card>
            <Statistic title="Lượt nộp bài" value={formatCount(data.submittedAttempts)} />
          </Card>
        </Col>
      </Row>

      <Card title="Cách tính số liệu">
        <Descriptions
          bordered
          column={1}
          items={[
            {
              key: 'active-users',
              label: 'Tài khoản hoạt động',
              children: 'Tất cả tài khoản có trạng thái đang hoạt động (is_active = TRUE).',
            },
            {
              key: 'teachers',
              label: 'Giảng viên',
              children: 'Tổng số tài khoản mang vai trò TEACHER trong hệ thống.',
            },
            {
              key: 'published-courses',
              label: 'Khóa học đang xuất bản',
              children: 'Khóa học đang hiển thị trên Khám phá (is_published = TRUE và chưa lưu trữ).',
            },
            {
              key: 'enrollments',
              label: 'Lượt ghi danh',
              children: 'Tổng số bản ghi ghi danh của sinh viên vào các khóa học.',
            },
            {
              key: 'submitted-attempts',
              label: 'Lượt nộp bài',
              children: 'Tổng số lượt làm quiz ở trạng thái SUBMITTED trên toàn hệ thống.',
            },
          ]}
        />
      </Card>
    </Space>
  )
}
