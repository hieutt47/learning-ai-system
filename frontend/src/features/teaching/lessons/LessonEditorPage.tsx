import { DeleteOutlined, DownloadOutlined, UploadOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, App, Button, Card, Form, Input, List, Popconfirm, Space, Upload, type UploadFile } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router'
import { ErrorState, LoadingState } from '../../../shared/components/PageState'
import { paths } from '../../../shared/routing/paths'
import { courseKeys, getOwnedCourse } from '../../marketplace/course.api'
import { LessonLifecycleActions, LessonStatusTag } from './LessonLifecycleControls'
import {
  createLesson,
  deleteLessonDocument,
  downloadLessonDocument,
  getCourseLessons,
  getLesson,
  getLessonDocuments,
  lessonKeys,
  updateLesson,
  uploadLessonDocument,
} from './lesson.api'
import type { LessonFormValues } from './lesson.types'
import { useLessonStatusMutation } from './useLessonStatusMutation'
import { isValidYouTubeUrl } from './youtube-url'

export function LessonEditorPage() {
  const { lessonId = '' } = useParams()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { message } = App.useApp()
  const [form] = Form.useForm<LessonFormValues>()
  const [files, setFiles] = useState<UploadFile[]>([])
  const [dirty, setDirty] = useState(false)
  const isCreating = lessonId === 'new'
  const courseId = searchParams.get('courseId') ?? ''
  const validCourseContext = /^\d+$/.test(courseId) && Number(courseId) > 0

  const lessonQuery = useQuery({
    queryKey: lessonKeys.detail(lessonId),
    queryFn: () => getLesson(lessonId),
    enabled: !isCreating && Boolean(lessonId),
  })
  const activeCourseId = isCreating ? courseId : lessonQuery.data?.courseId ?? ''
  const courseQuery = useQuery({
    queryKey: courseKeys.owned(activeCourseId),
    queryFn: () => getOwnedCourse(activeCourseId),
    enabled: Boolean(activeCourseId),
  })
  const courseLessonsQuery = useQuery({
    queryKey: lessonKeys.course(activeCourseId),
    queryFn: () => getCourseLessons(activeCourseId),
    enabled: !isCreating && Boolean(activeCourseId),
  })
  const statusMutation = useLessonStatusMutation()
  const readOnly = courseQuery.data?.status === 'Archived' || lessonQuery.data?.status === 'ARCHIVED'
  const hasUnsavedChanges = dirty || files.length > 0
  const publishedCount = courseLessonsQuery.data?.filter((lesson) => lesson.status === 'PUBLISHED').length ?? 0
  const documentsQuery = useQuery({
    queryKey: lessonKeys.documents(lessonId),
    queryFn: () => getLessonDocuments(lessonId),
    enabled: !isCreating && Boolean(lessonId),
  })

  useEffect(() => {
    if (lessonQuery.data && !dirty) {
      form.setFieldsValue({
        title: lessonQuery.data.title,
        videoUrl: lessonQuery.data.videoUrl,
        transcriptText: lessonQuery.data.transcriptText,
      })
    }
  }, [form, lessonQuery.data, dirty])

  const saveMutation = useMutation({
    mutationFn: async (values: LessonFormValues) => {
      const lesson = isCreating
        ? await createLesson(courseId, values)
        : await updateLesson(lessonId, values)
      let failedUpload: Error | null = null
      for (const item of files) {
        if (!item.originFileObj) continue
        try {
          await uploadLessonDocument(lesson.id, item.originFileObj)
        } catch (error) {
          failedUpload = error instanceof Error ? error : new Error('Không tải được tài liệu.')
          break
        }
      }
      return { lesson, failedUpload }
    },
    onSuccess: async ({ lesson, failedUpload }) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: lessonKeys.course(lesson.courseId) }),
        queryClient.invalidateQueries({ queryKey: lessonKeys.detail(lesson.id) }),
        queryClient.invalidateQueries({ queryKey: lessonKeys.documents(lesson.id) }),
        queryClient.invalidateQueries({ queryKey: ['courses'] }),
      ])
      setFiles([])
      setDirty(false)
      if (failedUpload) {
        void message.warning(`Bài học đã lưu nhưng tải tài liệu thất bại: ${failedUpload.message}. Hãy chọn lại file để thử tiếp.`)
      } else {
        void message.success(isCreating ? 'Đã tạo bài học.' : 'Đã lưu thay đổi.')
      }
      if (isCreating) navigate(paths.teachingLesson(lesson.id), { replace: true })
    },
    onError: (error: Error) => void message.error(error.message),
  })
  const deleteDocumentMutation = useMutation({
    mutationFn: deleteLessonDocument,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: lessonKeys.documents(lessonId) })
      void message.success('Đã xóa tài liệu.')
    },
    onError: (error: Error) => void message.error(error.message),
  })

  const previewFile = (file: UploadFile) => {
    if (!file.originFileObj) return
    const url = URL.createObjectURL(file.originFileObj)
    window.open(url, '_blank', 'noopener,noreferrer')
    window.setTimeout(() => URL.revokeObjectURL(url), 60_000)
  }

  const cancelEdit = () => {
    if (isCreating) {
      navigate(paths.teachingCourseLessons(courseId))
      return
    }
    if (lessonQuery.data) {
      form.setFieldsValue({
        title: lessonQuery.data.title,
        videoUrl: lessonQuery.data.videoUrl,
        transcriptText: lessonQuery.data.transcriptText,
      })
    }
    setFiles([])
    setDirty(false)
  }

  if (isCreating && !validCourseContext) {
    return <Alert type="error" showIcon message="Không xác định được khóa học" description="Hãy mở danh sách bài học của khóa học rồi chọn Thêm bài học." />
  }
  if (!isCreating && lessonQuery.isLoading) return <LoadingState tip="Đang tải bài học…" />
  if (!isCreating && lessonQuery.isError) return <ErrorState error={lessonQuery.error} onRetry={() => void lessonQuery.refetch()} />
  if (courseQuery.isLoading || (!isCreating && courseLessonsQuery.isLoading)) return <LoadingState tip="Đang tải trạng thái bài học…" />
  if (courseQuery.isError) return <ErrorState error={courseQuery.error} onRetry={() => void courseQuery.refetch()} />
  if (!isCreating && courseLessonsQuery.isError) return <ErrorState error={courseLessonsQuery.error} onRetry={() => void courseLessonsQuery.refetch()} />

  return (
    <Card title={isCreating ? 'Thêm bài học mới' : 'Chỉnh sửa bài học'}
      extra={!isCreating && lessonQuery.data ? <Space wrap>
        <LessonStatusTag status={lessonQuery.data.status} />
        <LessonLifecycleActions lesson={lessonQuery.data} courseStatus={courseQuery.data!.status}
          publishedCount={publishedCount} hasUnsavedChanges={hasUnsavedChanges}
          busy={saveMutation.isPending || statusMutation.isPending}
          onChange={statusMutation.mutateAsync} />
      </Space> : null}>
      {readOnly && <Alert type="warning" showIcon message={courseQuery.data?.status === 'Archived' ? 'Khóa đã lưu trữ: chỉ xem nội dung bài học.' : 'Bài học đã lưu trữ: chỉ xem nội dung và lịch sử.'} style={{ marginBottom: 16 }} />}
      {!isCreating && lessonQuery.data?.status === 'DRAFT' && !readOnly && <Alert type="info" showIcon message="Bản nháp chỉ giảng viên nhìn thấy." description="Lưu nội dung và tài liệu trước khi xuất bản cho học viên." style={{ marginBottom: 16 }} />}
      {!isCreating && lessonQuery.data?.status === 'PUBLISHED' && !readOnly && <Alert type="info" showIcon message="Bài học đang hiển thị cho học viên." description="Nếu cần thay toàn bộ nội dung, hãy tạo bài mới rồi lưu trữ bài cũ để giữ lịch sử quiz." style={{ marginBottom: 16 }} />}
      {isCreating && !readOnly && <Alert type="info" showIcon title="Lưu bài học trước khi soạn Quiz hoặc dùng AI & Flashcard" description={`Bài học mới sẽ được thêm vào khóa học #${courseId}. Sau khi bấm Tạo bài học thành công, các tab sẽ được mở.`} style={{ marginBottom: 16 }} />}
      <Form<LessonFormValues> form={form} layout="vertical"
        onValuesChange={() => setDirty(true)} onFinish={(values) => saveMutation.mutate(values)} requiredMark="optional">
        <Form.Item name="title" label="Tên bài học" rules={[{ required: true, whitespace: true, message: 'Vui lòng nhập tên bài học.' }, { max: 200, message: 'Tên bài học không được vượt quá 200 ký tự.' }]}>
          <Input placeholder="Ví dụ: JVM và bytecode" disabled={readOnly || saveMutation.isPending || statusMutation.isPending} />
        </Form.Item>
        <Form.Item name="videoUrl" label="Đường dẫn video YouTube" rules={[{ validator: async (_, value?: string) => { if (!value || isValidYouTubeUrl(value)) return; throw new Error('Vui lòng nhập đúng đường dẫn YouTube.') } }]}>
          <Input placeholder="https://www.youtube.com/watch?v=..." disabled={readOnly || saveMutation.isPending || statusMutation.isPending} />
        </Form.Item>
        <Form.Item name="transcriptText" label="Transcript" extra="Transcript được dùng làm nguồn tạo quiz và flashcard.">
          <Input.TextArea rows={8} placeholder="Nhập hoặc dán nội dung transcript..." showCount disabled={readOnly || saveMutation.isPending || statusMutation.isPending} />
        </Form.Item>
        <Form.Item label="Tài liệu mới" extra="PDF, DOCX hoặc TXT; mỗi file tối đa 10 MiB.">
          <Upload multiple accept=".pdf,.docx,.txt" fileList={files} disabled={readOnly || saveMutation.isPending || statusMutation.isPending}
            beforeUpload={() => false} onChange={({ fileList }) => setFiles(fileList)} onPreview={previewFile}>
            <Button icon={<UploadOutlined />}>Chọn tài liệu</Button>
          </Upload>
        </Form.Item>
        {!isCreating && (
          <Form.Item label="Tài liệu đã lưu">
            {documentsQuery.isError ? <ErrorState error={documentsQuery.error} onRetry={() => void documentsQuery.refetch()} /> : (
              <List loading={documentsQuery.isLoading} dataSource={documentsQuery.data ?? []} locale={{ emptyText: 'Chưa có tài liệu.' }} renderItem={(document) => (
                <List.Item actions={[
                  <Button key="download" type="link" icon={<DownloadOutlined />} onClick={() => void downloadLessonDocument(document.id, document.originalName).catch((error: Error) => message.error(error.message))}>Tải xuống</Button>,
                  <Popconfirm key="delete" title="Xóa tài liệu?" okText="Xóa" cancelText="Hủy" onConfirm={() => deleteDocumentMutation.mutateAsync(document.id)}><Button type="text" danger icon={<DeleteOutlined />} disabled={readOnly || deleteDocumentMutation.isPending} /></Popconfirm>,
                ]}>
                  <List.Item.Meta title={document.originalName} description={`${(document.sizeBytes / 1024).toFixed(1)} KB · ${document.textExtracted ? 'Đã trích xuất text' : 'Không có text'}`} />
                </List.Item>
              )} />
            )}
          </Form.Item>
        )}
        <Space>
          {!readOnly && <Button type="primary" htmlType="submit" loading={saveMutation.isPending}>{isCreating ? 'Tạo bài học' : 'Lưu thay đổi'}</Button>}
          {!readOnly && <Button onClick={cancelEdit}>Hủy</Button>}
        </Space>
      </Form>
    </Card>
  )
}
