import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  App,
  Button,
  Card,
  Col,
  Form,
  Input,
  Modal,
  Row,
  Segmented,
  Select,
  Space,
  Statistic,
  Table,
  Typography,
  type TableProps,
} from 'antd'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { EmptyState, ErrorState, LoadingState } from '../../shared/components/PageState'
import { paths } from '../../shared/routing/paths'
import {
  archiveCourse,
  courseKeys,
  createCourse,
  getCategories,
  listOwnedCourses,
  setPublished,
} from './course.api'
import { CourseStatusTag } from './CourseStatusTag'
import { formatDateTime } from './format'
import type { CourseFormValues, CourseStatus, OwnedCourse } from './course.types'

type Filter = 'all' | CourseStatus

export function TeachingCoursesPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { message, modal } = App.useApp()
  const [filter, setFilter] = useState<Filter>('all')
  const [createOpen, setCreateOpen] = useState(false)
  const [form] = Form.useForm<CourseFormValues>()
  const coursesQuery = useQuery({
    queryKey: courseKeys.teaching,
    queryFn: () => listOwnedCourses({ page: 1, page_size: 100 }),
  })
  const categoriesQuery = useQuery({ queryKey: courseKeys.categories, queryFn: getCategories })

  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: courseKeys.teaching })
  }
  const createMutation = useMutation({
    mutationFn: (values: CourseFormValues) => createCourse(values),
    onSuccess: async (course) => {
      await invalidate()
      setCreateOpen(false)
      form.resetFields()
      void message.success('Đã tạo khóa học.')
      navigate(paths.teachingCourse(course.id))
    },
    onError: (error: Error) => void message.error(error.message),
  })
  const publishMutation = useMutation({
    mutationFn: ({ id, published }: { id: string; published: boolean }) => setPublished(id, published),
    onSuccess: async (course) => {
      await invalidate()
      void message.success(course.status === 'Published' ? 'Đã xuất bản khóa học.' : 'Đã gỡ xuất bản.')
    },
    onError: (error: Error) => void message.error(error.message),
  })
  const archiveMutation = useMutation({
    mutationFn: (id: string) => archiveCourse(id),
    onSuccess: async () => {
      await invalidate()
      void message.success('Đã lưu trữ khóa học.')
    },
    onError: (error: Error) => void message.error(error.message),
  })
  if (coursesQuery.isLoading) return <LoadingState tip="Đang tải khóa học…" />
  if (coursesQuery.isError) return <ErrorState error={coursesQuery.error} onRetry={() => void coursesQuery.refetch()} />
  const all = coursesQuery.data?.items ?? []
  const counts = {
    Draft: all.filter((course) => course.status === 'Draft').length,
    Published: all.filter((course) => course.status === 'Published').length,
    Archived: all.filter((course) => course.status === 'Archived').length,
  }
  const visible = filter === 'all' ? all : all.filter((course) => course.status === filter)
  const totalStudents = all.reduce((sum, course) => sum + course.totalStudents, 0)
  const confirmArchive = (course: OwnedCourse) => {
    modal.confirm({
      title: 'Lưu trữ khóa học?',
      content:
        'Sinh viên đã ghi danh vẫn xem lại được, nhưng khóa sẽ không còn hiển thị ở trang Khám phá.',
      okText: 'Lưu trữ',
      cancelText: 'Hủy',
      onOk: () => archiveMutation.mutateAsync(course.id),
    })
  }
  const columns: TableProps<OwnedCourse>['columns'] = [
    {
      title: 'Khóa học',
      dataIndex: 'title',
      render: (title: string, course) => (
        <div>
          <Link to={paths.teachingCourse(course.id)}>
            <strong>{title}</strong>
          </Link>
          <div style={{ color: 'var(--text-2, rgba(0,0,0,0.55))', fontSize: 12 }}>{course.categoryName}</div>
        </div>
      ),
    },
    { title: 'Trạng thái', dataIndex: 'status', width: 120, render: (status: CourseStatus) => <CourseStatusTag status={status} /> },
    {
      title: 'Bài học',
      dataIndex: 'lessonCount',
      width: 100,
      align: 'center',
      render: (count: number, course) => <Link to={paths.teachingCourseLessons(course.id)}>{count}</Link>,
    },
    {
      title: 'Học viên',
      dataIndex: 'totalStudents',
      width: 110,
      align: 'center',
      render: (count: number, course) => (
        <Link to={paths.teachingCourseStudents(course.id)}>
          <strong>{count}</strong>
        </Link>
      ),
    },
    { title: 'Cập nhật', dataIndex: 'updatedAt', width: 150, render: (value: string) => formatDateTime(value) },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 240,
      render: (_, course) => (
        <Space wrap>
          <Link to={paths.teachingCourse(course.id)}>
            <Button size="small">Sửa</Button>
          </Link>
          {course.status === 'Draft' && (
            <Button
              size="small"
              type="primary"
              loading={publishMutation.isPending}
              onClick={() => publishMutation.mutate({ id: course.id, published: true })}
            >
              Xuất bản
            </Button>
          )}
          {course.status === 'Published' && (
            <Button
              size="small"
              loading={publishMutation.isPending}
              onClick={() => publishMutation.mutate({ id: course.id, published: false })}
            >
              Gỡ xuất bản
            </Button>
          )}
          {(course.status === 'Draft' || course.status === 'Published') && (
            <Button size="small" danger onClick={() => confirmArchive(course)}>
              Lưu trữ
            </Button>
          )}
          {course.status === 'Archived' && (
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              Không thể xuất bản lại
            </Typography.Text>
          )}
        </Space>
      ),
    },
  ]
  const createButton = (
    <Button type="primary" onClick={() => setCreateOpen(true)}>
      + Tạo khóa học
    </Button>
  )

  return (
    <Space orientation="vertical" size={20} style={{ width: '100%' }}>
      <Row justify="space-between" align="middle">
        <Col>
          <Typography.Title level={3} style={{ margin: 0 }}>
            Tổng quan
          </Typography.Title>
          <Typography.Text type="secondary">Khóa học, học viên và bài nộp của bạn.</Typography.Text>
        </Col>
        <Col>{createButton}</Col>
      </Row>
      <Row gutter={[16, 16]}>
        <Col xs={24} md={8}>
          <Card>
            <Statistic title="Khóa đã xuất bản" value={counts.Published} />
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              trên {all.length} khóa · {counts.Draft} Draft, {counts.Archived} Archived
            </Typography.Text>
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card>
            <Statistic title="Tổng học viên" value={totalStudents} />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card>
            <Statistic title="Khóa đang lưu trữ" value={counts.Archived} />
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              Học viên cũ vẫn vào học được
            </Typography.Text>
          </Card>
        </Col>
      </Row>
      <Card
        title="Khóa học của tôi"
        extra={
          <Segmented
            value={filter}
            onChange={(value) => setFilter(value as Filter)}
            options={[
              { label: `Tất cả (${all.length})`, value: 'all' },
              { label: `Draft (${counts.Draft})`, value: 'Draft' },
              { label: `Published (${counts.Published})`, value: 'Published' },
              { label: `Archived (${counts.Archived})`, value: 'Archived' },
            ]}
          />
        }
      >
        {all.length === 0 ? (
          <EmptyState
            description="Bạn chưa có khóa học nào. Tạo khóa học đầu tiên, thêm bài học rồi xuất bản để sinh viên ghi danh."
            action={createButton}
          />
        ) : (
          <Table<OwnedCourse>
            rowKey="id"
            columns={columns}
            dataSource={visible}
            pagination={false}
            scroll={{ x: 960 }}
            locale={{ emptyText: <EmptyState description="Không có khóa học nào ở trạng thái này." /> }}
          />
        )}
      </Card>
      <Modal
        title="Tạo khóa học"
        open={createOpen}
        okText="Tạo khóa học"
        cancelText="Hủy"
        confirmLoading={createMutation.isPending}
        onOk={() => form.submit()}
        onCancel={() => setCreateOpen(false)}
        destroyOnHidden
      >
        <Form<CourseFormValues>
          form={form}
          layout="vertical"
          initialValues={{ title: '', description: '', price: '0' }}
          onFinish={(values) => createMutation.mutate(values)}
        >
          <Form.Item name="title" label="Tên khóa học" rules={[{ required: true, max: 200, message: 'Nhập tên khóa học.' }]}>
            <Input maxLength={200} showCount placeholder="VD: Lập trình Java cơ bản" />
          </Form.Item>
          <Form.Item name="description" label="Mô tả" rules={[{ required: true, message: 'Nhập mô tả khóa học.' }]}>
            <Input.TextArea rows={4} maxLength={2000} showCount />
          </Form.Item>
          <Form.Item name="categoryId" label="Danh mục" rules={[{ required: true, message: 'Chọn danh mục.' }]}>
            <Select
              loading={categoriesQuery.isLoading}
              placeholder="Chọn danh mục"
              options={(categoriesQuery.data ?? []).map((category) => ({ label: category.name, value: category.id }))}
            />
          </Form.Item>
          <Form.Item
            name="price"
            label="Giá (VNĐ)"
            extra="Mặc định 0 — bản MVP chỉ xuất bản được khóa miễn phí."
            rules={[{ pattern: /^\d+(\.\d{1,2})?$/, message: 'Giá phải là số từ 0 trở lên.' }]}
          >
            <Input inputMode="decimal" />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  )
}
