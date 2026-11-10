import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Card, Input, Space, Table, Typography, type TableProps } from 'antd'
import { useState } from 'react'
import { Link } from 'react-router'
import { EmptyState, ErrorState, LoadingState } from '../../shared/components/PageState'
import { paths } from '../../shared/routing/paths'
import { courseKeys, listOwnedCourses, setPublished } from './course.api'
import { CourseStatusTag } from './CourseStatusTag'
import { formatDateTime } from './format'
import type { CourseStatus, OwnedCourse } from './course.types'

export function AdminCoursesPage() {
  const queryClient = useQueryClient()
  const { message, modal } = App.useApp()
  const [query, setQuery] = useState('')
  const coursesQuery = useQuery({
    queryKey: courseKeys.teaching,
    queryFn: () => listOwnedCourses({ page: 1, page_size: 100 }),
  })
  const unpublishMutation = useMutation({
    mutationFn: (courseId: string) => setPublished(courseId, false),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: courseKeys.teaching })
      void message.success('Đã gỡ xuất bản.')
    },
    onError: (error: Error) => void message.error(error.message),
  })
  if (coursesQuery.isLoading) return <LoadingState tip="Đang tải khóa học…" />
  if (coursesQuery.isError) return <ErrorState error={coursesQuery.error} onRetry={() => void coursesQuery.refetch()} />
  const all = coursesQuery.data?.items ?? []
  const normalized = query.trim().toLowerCase()
  const visible = normalized
    ? all.filter(
        (course) =>
          course.title.toLowerCase().includes(normalized) || course.categoryName.toLowerCase().includes(normalized),
      )
    : all
  const confirmUnpublish = (course: OwnedCourse) =>
    modal.confirm({
      title: 'Gỡ xuất bản khóa học?',
      content: 'Khóa học sẽ không còn hiển thị ở trang Khám phá. Giảng viên có thể xuất bản lại.',
      okText: 'Gỡ xuất bản',
      cancelText: 'Hủy',
      okButtonProps: { danger: true },
      onOk: () => unpublishMutation.mutateAsync(course.id),
    })
  const columns: TableProps<OwnedCourse>['columns'] = [
    {
      title: 'Khóa học',
      dataIndex: 'title',
      render: (title: string, course) => (
        <div>
          <strong>{title}</strong>
          <div style={{ color: 'var(--text-2, rgba(0,0,0,0.55))', fontSize: 12 }}>{course.categoryName}</div>
        </div>
      ),
    },
    {
      title: 'Giảng viên',
      dataIndex: 'teacherName',
      width: 160,
      render: (teacherName: string, course) => teacherName || `#${course.teacherId}`,
    },
    { title: 'Trạng thái', dataIndex: 'status', width: 120, render: (status: CourseStatus) => <CourseStatusTag status={status} /> },
    { title: 'Học viên', dataIndex: 'totalStudents', width: 100, align: 'center' },
    { title: 'Cập nhật', dataIndex: 'updatedAt', width: 150, render: (value: string) => formatDateTime(value) },
    {
      title: 'Hành động',
      key: 'actions',
      width: 220,
      render: (_, course) => (
        <Space wrap>
          <Link to={paths.courseDetail(course.id)}>
            <Button size="small">Xem</Button>
          </Link>
          {course.status === 'Published' && (
            <Button size="small" danger loading={unpublishMutation.isPending} onClick={() => confirmUnpublish(course)}>
              Gỡ xuất bản
            </Button>
          )}
        </Space>
      ),
    },
  ]
  return (
    <Card
      title={
        <Space>
          <span>Tất cả khóa học</span>
          <Typography.Text type="secondary" style={{ fontWeight: 400 }}>
            {all.length}
          </Typography.Text>
        </Space>
      }
      extra={
        <Input.Search
          allowClear
          placeholder="Tìm theo tên khóa hoặc danh mục"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          style={{ width: 280 }}
        />
      }
    >
      {all.length === 0 ? (
        <EmptyState description="Chưa có khóa học nào trong hệ thống." />
      ) : (
        <Table<OwnedCourse>
          rowKey="id"
          columns={columns}
          dataSource={visible}
          pagination={{ pageSize: 20, showSizeChanger: false }}
          scroll={{ x: 900 }}
          locale={{ emptyText: <EmptyState description="Không có khóa học khớp. Thử từ khóa khác." /> }}
        />
      )}
    </Card>
  )
}
