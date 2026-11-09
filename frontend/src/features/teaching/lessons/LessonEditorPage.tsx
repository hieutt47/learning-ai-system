import { DeleteOutlined, DownloadOutlined, UploadOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, App, Button, Card, Form, Input, List, Popconfirm, Space, Typography, Upload, type UploadFile } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router'
import { ErrorState, LoadingState } from '../../../shared/components/PageState'
import { paths } from '../../../shared/routing/paths'
import {
  createLesson,
  deleteLessonDocument,
  downloadLessonDocument,
  getLesson,
  getLessonDocuments,
  lessonKeys,
  updateLesson,
  uploadLessonDocument,
} from './lesson.api'
import type { LessonFormValues } from './lesson.types'
import { isValidYouTubeUrl } from './youtube-url'

export function LessonEditorPage() {
  const { lessonId = '' } = useParams()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { message } = App.useApp()
  const [form] = Form.useForm<LessonFormValues>()
  const [files, setFiles] = useState<UploadFile[]>([])
  const isCreating = lessonId === 'new'
  const courseId = searchParams.get('courseId') ?? ''
  const validCourseContext = /^\d+$/.test(courseId) && Number(courseId) > 0

  const lessonQuery = useQuery({
    queryKey: lessonKeys.detail(lessonId),
    queryFn: () => getLesson(lessonId),
    enabled: !isCreating && Boolean(lessonId),
  })
  const documentsQuery = useQuery({
    queryKey: lessonKeys.documents(lessonId),
    queryFn: () => getLessonDocuments(lessonId),
    enabled: !isCreating && Boolean(lessonId),
  })

  useEffect(() => {
    if (lessonQuery.data) {
      form.setFieldsValue({
        title: lessonQuery.data.title,
        videoUrl: lessonQuery.data.videoUrl,
        transcriptText: lessonQuery.data.transcriptText,
      })
    }
  }, [form, lessonQuery.data])

  const saveMutation = useMutation({
    mutationFn: async (values: LessonFormValues) => {
      const lesson = isCreating
        ? await createLesson(courseId, values)
        : await updateLesson(lessonId, values)
      for (const item of files) {
        if (item.originFileObj) await uploadLessonDocument(lesson.id, item.originFileObj)
      }
      return lesson
    },
    onSuccess: async (lesson) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: lessonKeys.course(lesson.courseId) }),
        queryClient.invalidateQueries({ queryKey: lessonKeys.detail(lesson.id) }),
        queryClient.invalidateQueries({ queryKey: lessonKeys.documents(lesson.id) }),
      ])
      setFiles([])
      void message.success(isCreating ? 'Đã tạo bài học.' : 'Đã lưu thay đổi.')
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

  if (isCreating && !validCourseContext) {
    return <Alert type="error" showIcon message="Không xác định được khóa học" description="Hãy mở danh sách bài học của khóa học rồi chọn Thêm bài học." />
  }
  if (!isCreating && lessonQuery.isLoading) return <LoadingState tip="Đang tải bài học…" />
  if (!isCreating && lessonQuery.isError) return <ErrorState error={lessonQuery.error} onRetry={() => void lessonQuery.refetch()} />

  return (
    <Card title={isCreating ? 'Thêm bài học mới' : 'Chỉnh sửa bài học'}>
      {isCreating && <Typography.Paragraph type="secondary">Bài học mới sẽ được thêm vào khóa học #{courseId}.</Typography.Paragraph>}
      <Form<LessonFormValues> form={form} layout="vertical" onFinish={(values) => saveMutation.mutate(values)} requiredMark="optional">
        <Form.Item name="title" label="Tên bài học" rules={[{ required: true, whitespace: true, message: 'Vui lòng nhập tên bài học.' }, { max: 200, message: 'Tên bài học không được vượt quá 200 ký tự.' }]}>
          <Input placeholder="Ví dụ: JVM và bytecode" />
        </Form.Item>
        <Form.Item name="videoUrl" label="Đường dẫn video YouTube" rules={[{ validator: async (_, value?: string) => { if (!value || isValidYouTubeUrl(value)) return; throw new Error('Vui lòng nhập đúng đường dẫn YouTube.') } }]}>
          <Input placeholder="https://www.youtube.com/watch?v=..." />
        </Form.Item>
        <Form.Item name="transcriptText" label="Transcript" extra="Transcript được dùng làm nguồn tạo quiz và flashcard.">
          <Input.TextArea rows={8} placeholder="Nhập hoặc dán nội dung transcript..." showCount />
        </Form.Item>
        <Form.Item label="Tài liệu mới" extra="PDF, DOCX hoặc TXT; mỗi file tối đa 10 MiB.">
          <Upload multiple accept=".pdf,.docx,.txt" fileList={files} beforeUpload={() => false} onChange={({ fileList }) => setFiles(fileList)} onPreview={previewFile}>
            <Button icon={<UploadOutlined />}>Chọn tài liệu</Button>
          </Upload>
        </Form.Item>
        {!isCreating && (
          <Form.Item label="Tài liệu đã lưu">
            {documentsQuery.isError ? <ErrorState error={documentsQuery.error} onRetry={() => void documentsQuery.refetch()} /> : (
              <List loading={documentsQuery.isLoading} dataSource={documentsQuery.data ?? []} locale={{ emptyText: 'Chưa có tài liệu.' }} renderItem={(document) => (
                <List.Item actions={[
                  <Button key="download" type="link" icon={<DownloadOutlined />} onClick={() => void downloadLessonDocument(document.id, document.originalName).catch((error: Error) => message.error(error.message))}>Tải xuống</Button>,
                  <Popconfirm key="delete" title="Xóa tài liệu?" okText="Xóa" cancelText="Hủy" onConfirm={() => deleteDocumentMutation.mutateAsync(document.id)}><Button type="text" danger icon={<DeleteOutlined />} /></Popconfirm>,
                ]}>
                  <List.Item.Meta title={document.originalName} description={`${(document.sizeBytes / 1024).toFixed(1)} KB · ${document.textExtracted ? 'Đã trích xuất text' : 'Không có text'}`} />
                </List.Item>
              )} />
            )}
          </Form.Item>
        )}
        <Space>
          <Button type="primary" htmlType="submit" loading={saveMutation.isPending}>{isCreating ? 'Tạo bài học' : 'Lưu thay đổi'}</Button>
          <Button onClick={() => isCreating ? navigate(paths.teachingCourseLessons(courseId)) : form.resetFields()}>Hủy</Button>
        </Space>
      </Form>
    </Card>
  )
}
