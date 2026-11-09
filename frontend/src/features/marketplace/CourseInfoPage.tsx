import { CheckCircleFilled, CloseCircleFilled, UploadOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  App,
  Button,
  Card,
  Col,
  Form,
  Input,
  Progress,
  Row,
  Select,
  Space,
  Typography,
  Upload,
} from 'antd'
import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { isApiError } from '../../shared/api/client'
import { ErrorState, LoadingState } from '../../shared/components/PageState'
import { usePageHeader } from '../../shared/components/pageHeader'
import { paths } from '../../shared/routing/paths'
import {
  archiveCourse,
  courseKeys,
  getCategories,
  getOwnedCourse,
  setPublished,
  updateCourse,
  uploadThumbnail,
} from './course.api'
import { formatDateTime } from './format'
import type { CourseFormValues, OwnedCourse } from './course.types'

/**
 * Khóa học · Thông tin (prototype #tcourse).
 *
 * Hai cột như prototype: bên trái form thông tin + ảnh bìa, bên phải checklist
 * điều kiện xuất bản và trạng thái. Checklist tính từ dữ liệu thật thay vì đếm
 * form đang sửa, nên nó chỉ đúng sau khi lưu — đây là lý do nút Xuất bản nằm
 * dưới checklist và bị khoá khi thiếu điều kiện.
 */
export function CourseInfoPage() {
  const { courseId = '' } = useParams()
  const queryClient = useQueryClient()
  const { message, modal } = App.useApp()
  const [form] = Form.useForm<CourseFormValues>()
  const [thumbnailError, setThumbnailError] = useState<string | null>(null)

  const courseQuery = useQuery({
    queryKey: courseKeys.owned(courseId),
    queryFn: () => getOwnedCourse(courseId),
    enabled: Boolean(courseId),
  })
  const categoriesQuery = useQuery({ queryKey: courseKeys.categories, queryFn: getCategories })

  usePageHeader(
    courseQuery.data
      ? {
          title: courseQuery.data.title,
          crumbs: [
            { label: 'Khóa học giảng dạy', to: paths.teachingCourses },
            { label: courseQuery.data.title },
          ],
        }
      : null,
  )

  // Nạp form một lần cho mỗi bản ghi; không nạp lại sau mỗi lần lưu để không
  // xoá những gì người dùng đang gõ dở.
  useEffect(() => {
    if (courseQuery.data) {
      form.setFieldsValue({
        title: courseQuery.data.title,
        description: courseQuery.data.description,
        categoryId: courseQuery.data.categoryId,
        price: courseQuery.data.price,
      })
    }
  }, [courseQuery.data, form])

  const invalidate = () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: courseKeys.owned(courseId) }),
      queryClient.invalidateQueries({ queryKey: courseKeys.teaching }),
    ])

  const saveMutation = useMutation({
    mutationFn: (values: CourseFormValues) => updateCourse(courseId, values),
    onSuccess: async () => {
      await invalidate()
      void message.success('Đã lưu thay đổi.')
    },
    onError: (error: unknown) => {
      if (isApiError(error) && error.details.length > 0) {
        const fieldErrors = error.fieldErrors()
        form.setFields(
          Object.entries(fieldErrors).map(([name, msg]) => ({
            name: name as keyof CourseFormValues,
            errors: [msg],
          })),
        )
        return
      }
      void message.error(error instanceof Error ? error.message : 'Không lưu được thay đổi.')
    },
  })

  const thumbnailMutation = useMutation({
    mutationFn: (file: File) => uploadThumbnail(courseId, file),
    onSuccess: async () => {
      setThumbnailError(null)
      await invalidate()
      void message.success('Đã cập nhật ảnh bìa.')
    },
    onError: (error: unknown) => {
      // FILE_TOO_LARGE và UNSUPPORTED_FILE_TYPE là hai lỗi upload người dùng sửa
      // được, nên hiện ngay cạnh ô chọn file thay vì chỉ hiện toast.
      setThumbnailError(error instanceof Error ? error.message : 'Không tải được ảnh.')
    },
  })

  const publishMutation = useMutation({
    mutationFn: (published: boolean) => setPublished(courseId, published),
    onSuccess: async (course) => {
      await invalidate()
      void message.success(course.status === 'Published' ? 'Đã xuất bản khóa học.' : 'Đã gỡ xuất bản.')
    },
    onError: (error: Error) => void message.error(error.message),
  })

  const archiveMutation = useMutation({
    mutationFn: () => archiveCourse(courseId),
    onSuccess: async () => {
      await invalidate()
      void message.success('Đã lưu trữ khóa học.')
    },
    onError: (error: Error) => void message.error(error.message),
  })

  if (courseQuery.isLoading) return <LoadingState tip="Đang tải khóa học…" />
  if (courseQuery.isError) return <ErrorState error={courseQuery.error} onRetry={() => void courseQuery.refetch()} />

  const course = courseQuery.data!
  const checks = publishChecks(course)
  const passed = checks.filter((check) => check.ok).length
  const readOnly = course.status === 'Archived'

  const save = () => {
    if (readOnly) return
    form.submit()
  }

  const confirmArchive = () =>
    modal.confirm({
      title: 'Lưu trữ khóa học?',
      content: 'Sinh viên đã ghi danh vẫn xem lại được, nhưng khóa sẽ không còn hiển thị ở trang Khám phá.',
      okText: 'Lưu trữ',
      cancelText: 'Hủy',
      onOk: () => archiveMutation.mutateAsync(),
    })

  const actions = (
    <Space wrap>
      {course.status === 'Draft' && (
        <>
          <Button onClick={save} loading={saveMutation.isPending}>
            Lưu thay đổi
          </Button>
          <Button
            type="primary"
            disabled={passed < checks.length}
            loading={publishMutation.isPending}
            onClick={() => publishMutation.mutate(true)}
          >
            Xuất bản
          </Button>
        </>
      )}
      {course.status === 'Published' && (
        <>
          <Button loading={publishMutation.isPending} onClick={() => publishMutation.mutate(false)}>
            Gỡ xuất bản
          </Button>
          <Button danger onClick={confirmArchive}>
            Lưu trữ
          </Button>
        </>
      )}
      {course.status === 'Archived' && (
        <Typography.Text type="secondary">Khóa đã lưu trữ, không xuất bản lại được.</Typography.Text>
      )}
    </Space>
  )

  return (
    <Row gutter={[24, 24]}>
      <Col xs={24} lg={15}>
        <Space orientation="vertical" size={20} style={{ width: '100%' }}>
          <Card title="Thông tin cơ bản" extra={actions}>
            {readOnly && <Alert type="warning" showIcon message="Khóa đã lưu trữ: chỉ xem, không sửa được." style={{ marginBottom: 16 }} />}
            <Form<CourseFormValues> form={form} layout="vertical" onFinish={(values) => saveMutation.mutate(values)}>
              <Form.Item
                name="title"
                label="Tên khóa học"
                rules={[{ required: true, max: 200, message: 'Nhập tên khóa học.' }]}
              >
                <Input maxLength={200} showCount disabled={readOnly} />
              </Form.Item>
              <Form.Item
                name="description"
                label="Mô tả"
                rules={[{ required: true, message: 'Nhập mô tả khóa học.' }]}
              >
                <Input.TextArea rows={5} maxLength={2000} showCount disabled={readOnly} />
              </Form.Item>
              <Row gutter={16}>
                <Col xs={24} md={12}>
                  <Form.Item name="categoryId" label="Danh mục" rules={[{ required: true, message: 'Chọn danh mục.' }]}>
                    <Select
                      disabled={readOnly}
                      loading={categoriesQuery.isLoading}
                      options={(categoriesQuery.data ?? []).map((category) => ({
                        label: category.name,
                        value: category.id,
                      }))}
                    />
                  </Form.Item>
                </Col>
                <Col xs={24} md={12}>
                  <Form.Item
                    name="price"
                    label="Giá (VNĐ)"
                    extra="Khóa có giá > 0 lưu được ở Draft nhưng không xuất bản được."
                    rules={[{ pattern: /^\d+(\.\d{1,2})?$/, message: 'Giá phải là số từ 0 trở lên.' }]}
                  >
                    <Input inputMode="decimal" disabled={readOnly} />
                  </Form.Item>
                </Col>
              </Row>
            </Form>
          </Card>

          <Card title="Ảnh bìa">
            <Space orientation="vertical" size={12} style={{ width: '100%' }}>
              {course.thumbnailUrl ? (
                <img
                  src={course.thumbnailUrl}
                  alt="Ảnh bìa khóa học"
                  style={{ width: 260, borderRadius: 12, display: 'block' }}
                />
              ) : (
                <Typography.Text type="secondary">Chưa có ảnh bìa.</Typography.Text>
              )}
              <Upload
                accept="image/jpeg,image/png,image/webp"
                showUploadList={false}
                beforeUpload={(file) => {
                  thumbnailMutation.mutate(file)
                  // Trả false để antd không tự upload; ta đã gọi API riêng.
                  return Upload.LIST_IGNORE
                }}
                disabled={readOnly || thumbnailMutation.isPending}
              >
                <Button icon={<UploadOutlined />} loading={thumbnailMutation.isPending} disabled={readOnly}>
                  {course.thumbnailUrl ? 'Đổi ảnh bìa' : 'Chọn ảnh bìa'}
                </Button>
              </Upload>
              {thumbnailError && <Alert type="error" showIcon message={thumbnailError} />}
              {!thumbnailError && (
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  JPEG, PNG hoặc WebP · tối đa 5 MB · tỉ lệ 16:9
                </Typography.Text>
              )}
            </Space>
          </Card>
        </Space>
      </Col>

      <Col xs={24} lg={9}>
        <Space orientation="vertical" size={20} style={{ width: '100%' }}>
          <Card title="Điều kiện xuất bản" extra={<strong>{passed} / {checks.length}</strong>}>
            <Progress percent={Math.round((passed / checks.length) * 100)} showInfo={false} />
            <ul style={{ listStyle: 'none', padding: 0, margin: '12px 0 16px' }}>
              {checks.map((check) => (
                <li key={check.label} style={{ display: 'flex', gap: 8, padding: '4px 0' }}>
                  {check.ok ? (
                    <CheckCircleFilled style={{ color: 'var(--success-text, #15803d)' }} />
                  ) : (
                    <CloseCircleFilled style={{ color: 'var(--text-2, rgba(0,0,0,0.35))' }} />
                  )}
                  <span>{check.label}</span>
                </li>
              ))}
            </ul>
            {course.status === 'Draft' && (
              <>
                <Button
                  type="primary"
                  block
                  disabled={passed < checks.length}
                  loading={publishMutation.isPending}
                  onClick={() => publishMutation.mutate(true)}
                >
                  Xuất bản
                </Button>
                <Typography.Text type="secondary" style={{ display: 'block', marginTop: 8 }}>
                  {passed === checks.length
                    ? 'Đủ điều kiện để xuất bản.'
                    : 'Hoàn thành đủ điều kiện để xuất bản.'}
                </Typography.Text>
              </>
            )}
            {course.status !== 'Draft' && (
              <Typography.Text type="secondary">Chỉ khóa ở trạng thái Draft mới xuất bản được.</Typography.Text>
            )}
          </Card>

          <Card title="Trạng thái">
            <Space orientation="vertical" size={4}>
              <StatusExplanation course={course} />
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                Tạo {formatDateTime(course.createdAt)} · Cập nhật {formatDateTime(course.updatedAt)}
              </Typography.Text>
              <Link to={paths.teachingCourseLessons(course.id)}>Thêm hoặc sửa bài học →</Link>
            </Space>
          </Card>
        </Space>
      </Col>
    </Row>
  )
}

/**
 * Năm điều kiện publish, tính từ dữ liệu đã lưu.
 *
 * Lesson count và student count đến cùng response nên không cần gọi thêm API;
 * điều kiện "có bài học" vì thế đúng ngay sau khi thêm bài mà không phải tải lại.
 */
function publishChecks(course: OwnedCourse) {
  return [
    { label: 'Có tên khóa học', ok: course.title.trim().length > 0 },
    { label: 'Có mô tả', ok: course.description.trim().length > 0 },
    { label: 'Đã chọn danh mục', ok: Boolean(course.categoryId) },
    { label: 'Có ảnh bìa', ok: Boolean(course.thumbnailUrl) },
    { label: 'Có ít nhất 1 bài học', ok: course.lessonCount > 0 },
    { label: 'Giá bằng 0', ok: Number(course.price) === 0 },
  ]
}

function StatusExplanation({ course }: { course: OwnedCourse }) {
  const text =
    course.status === 'Draft'
      ? 'Sinh viên chưa nhìn thấy khóa này.'
      : course.status === 'Published'
        ? 'Sinh viên thấy và ghi danh được.'
        : 'Chỉ sinh viên đã ghi danh xem lại được.'
  return <Typography.Text>{text}</Typography.Text>
}
