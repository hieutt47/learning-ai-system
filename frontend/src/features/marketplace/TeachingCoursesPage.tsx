import { BookOutlined, InboxOutlined, PlusOutlined, TeamOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  App,
  Button,
  Card,
  Form,
  Input,
  Modal,
  Segmented,
  Select,
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
  unarchiveCourse,
} from './course.api'
import { applyCourseFormErrors, extractCourseErrorMessage } from './courseFormErrors'
import { CourseStatusTag } from './CourseStatusTag'
import { formatDateTime } from './format'
import type { CourseFormValues, CourseStatus, OwnedCourse } from './course.types'
import './teaching-courses.css'

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
    onError: (error: unknown) => {
      const detailMessage = applyCourseFormErrors(error, form)
      void message.error(detailMessage ?? extractCourseErrorMessage(error, 'Không tạo được khóa học.'))
    },
  })
  const publishMutation = useMutation({
    mutationFn: ({ id, published }: { id: string; published: boolean }) => setPublished(id, published),
    onSuccess: async (course) => {
      await invalidate()
      void message.success(course.status === 'Published' ? 'Đã xuất bản khóa học.' : 'Đã gỡ xuất bản.')
    },
    onError: (error: unknown) =>
      void message.error(extractCourseErrorMessage(error, 'Không cập nhật được trạng thái xuất bản.')),
  })
  const archiveMutation = useMutation({
    mutationFn: (id: string) => archiveCourse(id),
    onSuccess: async () => {
      await invalidate()
      void message.success('Đã lưu trữ khóa học.')
    },
    onError: (error: unknown) =>
      void message.error(extractCourseErrorMessage(error, 'Không lưu trữ được khóa học.')),
  })
  const unarchiveMutation = useMutation({
    mutationFn: (id: string) => unarchiveCourse(id),
    onSuccess: async () => {
      await invalidate()
      void message.success('Đã mở lại khóa học (chuyển về bản nháp).')
    },
    onError: (error: unknown) =>
      void message.error(extractCourseErrorMessage(error, 'Không mở lại được khóa học.')),
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
        'Khóa sẽ biến mất khỏi trang Khám phá. Học viên không thể mở nội dung bài học hoặc bắt đầu quiz mới; lịch sử làm quiz vẫn được giữ.',
      okText: 'Lưu trữ',
      cancelText: 'Hủy',
      onOk: () => archiveMutation.mutateAsync(course.id),
    })
  }
  const confirmUnarchive = (course: OwnedCourse) => {
    modal.confirm({
      title: 'Mở lại khóa học?',
      content: 'Khóa học sẽ được chuyển về trạng thái Draft (Nháp) để bạn chỉnh sửa và xuất bản lại.',
      okText: 'Mở lại',
      cancelText: 'Hủy',
      onOk: () => unarchiveMutation.mutateAsync(course.id),
    })
  }
  const columns: TableProps<OwnedCourse>['columns'] = [
    {
      title: 'Khóa học',
      dataIndex: 'title',
      render: (title: string, course) => (
        <div className="tc-course-cell">
          <Link to={paths.teachingCourse(course.id)} className="tc-course-title">
            {title}
          </Link>
          <span className="tc-course-cat">{course.categoryName}</span>
        </div>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      width: 118,
      align: 'center',
      render: (status: CourseStatus) => <CourseStatusTag status={status} />,
    },
    {
      title: 'Bài học',
      dataIndex: 'lessonCount',
      width: 96,
      align: 'center',
      render: (count: number, course) => (
        <Link to={paths.teachingCourseLessons(course.id)} className={`tc-metric-link${count === 0 ? ' is-zero' : ''}`}>
          {count}
        </Link>
      ),
    },
    {
      title: 'Học viên',
      dataIndex: 'totalStudents',
      width: 100,
      align: 'center',
      render: (count: number, course) => (
        <Link to={paths.teachingCourseStudents(course.id)} className={`tc-metric-link${count === 0 ? ' is-zero' : ''}`}>
          {count}
        </Link>
      ),
    },
    {
      title: 'Cập nhật',
      dataIndex: 'updatedAt',
      width: 154,
      render: (value: string) => <span className="tc-date">{formatDateTime(value)}</span>,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 280,
      align: 'right',
      render: (_, course) => {
        const isRowPublishing = publishMutation.isPending && publishMutation.variables?.id === course.id
        const isRowArchiving = archiveMutation.isPending && archiveMutation.variables === course.id
        const isRowUnarchiving = unarchiveMutation.isPending && unarchiveMutation.variables === course.id
        return (
          <div className="tc-actions">
            <Link to={paths.teachingCourse(course.id)}>
              <Button size="small">Sửa</Button>
            </Link>
            {course.status === 'Draft' && (
              <Button
                size="small"
                type="primary"
                loading={isRowPublishing}
                disabled={publishMutation.isPending && !isRowPublishing}
                onClick={() => publishMutation.mutate({ id: course.id, published: true })}
              >
                Xuất bản
              </Button>
            )}
            {course.status === 'Published' && (
              <Button
                size="small"
                loading={isRowPublishing}
                disabled={publishMutation.isPending && !isRowPublishing}
                onClick={() => publishMutation.mutate({ id: course.id, published: false })}
              >
                Gỡ xuất bản
              </Button>
            )}
            {(course.status === 'Draft' || course.status === 'Published') && (
              <Button size="small" danger loading={isRowArchiving} onClick={() => confirmArchive(course)}>
                Lưu trữ
              </Button>
            )}
            {course.status === 'Archived' && (
              <Button
                size="small"
                type="primary"
                loading={isRowUnarchiving}
                onClick={() => confirmUnarchive(course)}
              >
                Mở lại
              </Button>
            )}
          </div>
        )
      },
    },
  ]
  const createButton = (
    <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
      Tạo khóa học
    </Button>
  )

  return (
    <div className="tc-page">
      <div className="tc-header">
        <div className="tc-header-text">
          <Typography.Title level={3}>Tổng quan</Typography.Title>
          <p className="tc-header-sub">Quản lý trạng thái xuất bản, bài học và học viên theo từng khóa giảng dạy.</p>
        </div>
        {createButton}
      </div>

      <div className="tc-kpi-grid">
        <div className="tc-kpi-card">
          <div className="tc-kpi-top">
            <span className="tc-kpi-label">Khóa đã xuất bản</span>
            <span className="tc-kpi-icon tone-primary" aria-hidden="true">
              <BookOutlined />
            </span>
          </div>
          <div className="tc-kpi-value">
            {counts.Published}
            <small>/ {all.length}</small>
          </div>
          <div className="tc-kpi-sub">
            {counts.Draft} bản nháp · {counts.Archived} đã lưu trữ
          </div>
        </div>

        <div className="tc-kpi-card">
          <div className="tc-kpi-top">
            <span className="tc-kpi-label">Tổng học viên</span>
            <span className="tc-kpi-icon tone-success" aria-hidden="true">
              <TeamOutlined />
            </span>
          </div>
          <div className="tc-kpi-value">{totalStudents.toLocaleString('vi-VN')}</div>
          <div className="tc-kpi-sub">Tổng lượt ghi danh trên tất cả khóa học của bạn</div>
        </div>

        <div className="tc-kpi-card">
          <div className="tc-kpi-top">
            <span className="tc-kpi-label">Khóa đang lưu trữ</span>
            <span className="tc-kpi-icon tone-neutral" aria-hidden="true">
              <InboxOutlined />
            </span>
          </div>
          <div className="tc-kpi-value">{counts.Archived}</div>
          <div className="tc-kpi-sub">Đã ẩn khỏi luồng học; lịch sử quiz vẫn được giữ</div>
        </div>
      </div>

      <Card
        className="tc-table-card"
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
            scroll={{ x: 980 }}
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
          <Form.Item name="title" label="Tên khóa học" rules={[{ required: true, whitespace: true, max: 200, message: 'Nhập tên khóa học.' }]}>
            <Input maxLength={200} showCount placeholder="VD: Lập trình Java cơ bản" />
          </Form.Item>
          <Form.Item name="description" label="Mô tả" rules={[{ required: true, whitespace: true, message: 'Nhập mô tả khóa học.' }]}>
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
    </div>
  )
}
