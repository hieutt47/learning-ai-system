import { BarChartOutlined } from '@ant-design/icons'
import { Button, Card, Segmented, Space, Table, Tag, Typography, type TableProps } from 'antd'
import { useState } from 'react'
import { Link } from 'react-router'
import { EmptyState } from '../../../../shared/components/PageState'
import { paths } from '../../../../shared/routing/paths'
import { formatDateTime } from '../../../../shared/utils/format'
import {
  filterCourseQuizzes,
  formatCount,
  formatScore,
  isActivePublishedQuiz,
  quizStatusMeta,
  scoreBadgeColor,
} from '../statistics.format'
import type { CourseQuizFilter, CourseQuizStatistics } from '../statistics.types'

interface CourseQuizzesTableProps {
  courseId: string
  quizzes: readonly CourseQuizStatistics[]
}

export function CourseQuizzesTable({ courseId, quizzes }: CourseQuizzesTableProps) {
  const [filter, setFilter] = useState<CourseQuizFilter>('active_published')
  const activeCount = quizzes.filter(isActivePublishedQuiz).length
  const visibleQuizzes = filterCourseQuizzes(quizzes, filter)

  const columns: TableProps<CourseQuizStatistics>['columns'] = [
    {
      title: 'Tên quiz',
      dataIndex: 'quizTitle',
      render: (title: string, quiz) => (
        <div>
          <strong>{title}</strong>
          <div style={{ fontSize: 12, color: 'var(--text-3)' }}>{formatCount(quiz.questionCount)} câu hỏi</div>
        </div>
      ),
    },
    {
      title: 'Bài học',
      dataIndex: 'lessonTitle',
      render: (lessonTitle: string, quiz) => (
        <Space size={6} wrap>
          <span>
            Bài {quiz.lessonOrderIndex}: {lessonTitle}
          </span>
          {quiz.lessonStatus === 'ARCHIVED' && <Tag>Bài đã lưu trữ</Tag>}
          {quiz.lessonStatus === 'DRAFT' && <Tag color="warning">Bài nháp</Tag>}
        </Space>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      width: 140,
      render: (status: CourseQuizStatistics['status']) => {
        const meta = quizStatusMeta(status)
        return <Tag color={meta.color}>{meta.label}</Tag>
      },
    },
    {
      title: 'Lượt nộp',
      dataIndex: 'submittedAttempts',
      width: 105,
      align: 'center',
      render: (value: number) => formatCount(value),
    },
    {
      title: 'Học viên',
      dataIndex: 'uniqueStudents',
      width: 105,
      align: 'center',
      render: (value: number) => formatCount(value),
    },
    {
      title: 'Điểm TB',
      dataIndex: 'averageScore',
      width: 120,
      align: 'center',
      render: (value: number | null) =>
        value === null ? (
          <Typography.Text type="secondary">—</Typography.Text>
        ) : (
          <Tag color={scoreBadgeColor(value)}>{formatScore(value)} / 100</Tag>
        ),
    },
    {
      title: 'Nộp gần nhất',
      dataIndex: 'latestSubmittedAt',
      width: 155,
      render: (value: string | null) => (value ? formatDateTime(value) : '—'),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 135,
      align: 'right',
      render: (_, quiz) => (
        <Link to={`${paths.quizStatistics(quiz.quizId)}?courseId=${encodeURIComponent(courseId)}`}>
          <Button size="small" type="link" icon={<BarChartOutlined />}>
            Xem chi tiết
          </Button>
        </Link>
      ),
    },
  ]

  return (
    <Card
      title="Quiz của khóa học"
      extra={
        quizzes.length > 0 ? (
          <Segmented
            value={filter}
            onChange={(value) => setFilter(value as CourseQuizFilter)}
            options={[
              { label: `Đang xuất bản (${activeCount})`, value: 'active_published' },
              { label: `Tất cả (${quizzes.length})`, value: 'all' },
            ]}
          />
        ) : null
      }
    >
      {visibleQuizzes.length === 0 ? (
        <EmptyState
          description={
            filter === 'active_published' && quizzes.length > 0
              ? 'Không có quiz nào đang xuất bản ở bài học hiển thị. Chuyển sang "Tất cả" để xem lịch sử quiz đã đóng.'
              : 'Khóa học chưa có quiz nào được xuất bản. Hãy xuất bản quiz trong bài học để xem thống kê tại đây.'
          }
          action={
            quizzes.length === 0 ? (
              <Link to={paths.teachingCourseLessons(courseId)}>
                <Button type="primary">Đi tới danh sách bài học</Button>
              </Link>
            ) : undefined
          }
        />
      ) : (
        <>
          <Table<CourseQuizStatistics>
            rowKey="quizId"
            columns={columns}
            dataSource={visibleQuizzes}
            pagination={visibleQuizzes.length > 10 ? { pageSize: 10, showSizeChanger: false } : false}
            scroll={{ x: 960 }}
          />
          <Typography.Paragraph type="secondary" style={{ margin: '12px 0 0', fontSize: 12 }}>
            Điểm trung bình tính trên tất cả lượt đã nộp (status = SUBMITTED), thang điểm 0–100.
          </Typography.Paragraph>
        </>
      )}
    </Card>
  )
}
