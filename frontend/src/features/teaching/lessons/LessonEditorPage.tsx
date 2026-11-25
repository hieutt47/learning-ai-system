import {
  DeleteOutlined,
  DownloadOutlined,
  ExportOutlined,
  FileTextOutlined,
  PlayCircleOutlined,
  UploadOutlined,
} from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  App,
  Button,
  Card,
  Col,
  Form,
  Input,
  Modal,
  Popconfirm,
  Row,
  Space,
  Spin,
  Tag,
  Tooltip,
  Typography,
  Upload,
  type UploadFile,
} from 'antd'
import { useEffect, useRef, useState } from 'react'
import { useBlocker, useNavigate, useParams, useSearchParams } from 'react-router'
import { isApiError } from '../../../shared/api/client'
import { ErrorState, LoadingState } from '../../../shared/components/PageState'
import { usePageHeader } from '../../../shared/components/pageHeader'
import { paths } from '../../../shared/routing/paths'
import { courseKeys, getOwnedCourse } from '../../marketplace/course.api'
import { LessonLifecycleActions, LessonStatusTag } from './LessonLifecycleControls'
import { documentTypeBadge, formatFileSize } from './document-format'
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
import { MAX_TRANSCRIPT_UTF8_BYTES, utf8ByteLength, validateDocumentFile } from './lesson-validation'
import { useLessonStatusMutation } from './useLessonStatusMutation'
import { getYouTubeEmbedUrl, isOptionalValidYouTubeUrl } from './youtube-url'
import './lessons-ui.css'

const FIELD_MAP: Record<string, keyof LessonFormValues> = {
  title: 'title',
  video_url: 'videoUrl',
  videoUrl: 'videoUrl',
  transcript_text: 'transcriptText',
  transcriptText: 'transcriptText',
}

export function LessonEditorPage() {
  const { lessonId = '' } = useParams()
  return <LessonEditorContent key={lessonId} lessonId={lessonId} />
}

function LessonEditorContent({ lessonId }: { lessonId: string }) {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { message } = App.useApp()
  const [form] = Form.useForm<LessonFormValues>()
  const [files, setFiles] = useState<UploadFile[]>([])
  const [dirty, setDirty] = useState(false)
  const [downloadingId, setDownloadingId] = useState<string | null>(null)
  const bypassBlockerRef = useRef(false)
  const isCreating = lessonId === 'new'
  const courseId = searchParams.get('courseId') ?? ''
  const validCourseContext = /^\d+$/.test(courseId) && Number(courseId) > 0
  const watchedVideoUrl = Form.useWatch('videoUrl', form) ?? ''
  const watchedTranscript = Form.useWatch('transcriptText', form) ?? ''
  const previewEmbedUrl = getYouTubeEmbedUrl(watchedVideoUrl)
  const transcriptBytes = utf8ByteLength(watchedTranscript)

  const lessonQuery = useQuery({
    queryKey: lessonKeys.detail(lessonId),
    queryFn: () => getLesson(lessonId),
    enabled: !isCreating && Boolean(lessonId),
  })
  const activeCourseId = isCreating ? courseId : (lessonQuery.data?.courseId ?? '')
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
  const hasUnsavedChanges = !readOnly && (dirty || files.length > 0)
  const publishedCount = courseLessonsQuery.data?.filter((lesson) => lesson.status === 'PUBLISHED').length ?? 0
  const documentsQuery = useQuery({
    queryKey: lessonKeys.documents(lessonId),
    queryFn: () => getLessonDocuments(lessonId),
    enabled: !isCreating && Boolean(lessonId),
  })

  usePageHeader(
    activeCourseId
      ? {
          title: isCreating ? 'Thêm bài học mới' : (lessonQuery.data?.title ?? 'Soạn bài học'),
          crumbs: [
            { label: 'Khóa học giảng dạy', to: paths.teachingCourses },
            {
              label: courseQuery.data?.title ?? `Khóa học #${activeCourseId}`,
              to: paths.teachingCourseLessons(activeCourseId),
            },
            { label: isCreating ? 'Thêm bài học mới' : (lessonQuery.data?.title ?? 'Soạn bài học') },
          ],
        }
      : null,
  )

  useEffect(() => {
    if (!isCreating && lessonQuery.data && !dirty) {
      form.setFieldsValue({
        title: lessonQuery.data.title,
        videoUrl: lessonQuery.data.videoUrl,
        transcriptText: lessonQuery.data.transcriptText ?? '',
      })
    }
  }, [form, isCreating, lessonQuery.data, dirty])

  const saveMutation = useMutation({
    mutationFn: async (values: LessonFormValues) => {
      const lesson = isCreating ? await createLesson(courseId, values) : await updateLesson(lessonId, values)
      let failedUpload: Error | null = null
      const uploadedFileUids: string[] = []
      for (const item of files) {
        if (!item.originFileObj) continue
        try {
          await uploadLessonDocument(lesson.id, item.originFileObj)
          uploadedFileUids.push(item.uid)
        } catch (error) {
          failedUpload = error instanceof Error ? error : new Error('Không tải được tài liệu.')
          break
        }
      }
      return { lesson, failedUpload, uploadedFileUids }
    },
    onSuccess: async ({ lesson, failedUpload, uploadedFileUids }) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: lessonKeys.course(lesson.courseId) }),
        queryClient.invalidateQueries({ queryKey: lessonKeys.detail(lesson.id) }),
        queryClient.invalidateQueries({ queryKey: lessonKeys.documents(lesson.id) }),
        queryClient.invalidateQueries({ queryKey: ['courses'] }),
      ])
      setFiles((current) => current.filter((file) => !uploadedFileUids.includes(file.uid)))
      setDirty(false)
      if (failedUpload) {
        void message.warning(
          `Bài học đã lưu nhưng tải tài liệu thất bại: ${failedUpload.message}. ${
            isCreating
              ? 'Hãy chọn lại file ở màn hình bài học vừa tạo để thử tiếp.'
              : 'Các file chưa tải lên vẫn được giữ; hãy bấm Lưu thay đổi để thử lại.'
          }`,
        )
      } else {
        void message.success(isCreating ? 'Đã tạo bài học.' : 'Đã lưu thay đổi.')
      }
      if (isCreating) {
        bypassBlockerRef.current = true
        navigate(paths.teachingLesson(lesson.id), { replace: true })
      }
    },
    onError: (error: Error) => {
      if (isApiError(error) && error.details.length > 0) {
        const mapped = Object.entries(error.fieldErrors())
          .map(([field, msg]) => {
            const target = FIELD_MAP[field]
            return target ? { name: target, errors: [msg] } : null
          })
          .filter((item): item is { name: keyof LessonFormValues; errors: string[] } => item !== null)
        if (mapped.length > 0) {
          form.setFields(mapped)
          return
        }
      }
      void message.error(error.message)
    },
  })

  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) =>
      !bypassBlockerRef.current &&
      hasUnsavedChanges &&
      !saveMutation.isPending &&
      currentLocation.pathname !== nextLocation.pathname,
  )

  useEffect(() => {
    if (!hasUnsavedChanges) return
    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault()
      event.returnValue = ''
    }
    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [hasUnsavedChanges])

  const deleteDocumentMutation = useMutation({
    mutationFn: deleteLessonDocument,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: lessonKeys.documents(lessonId) }),
        queryClient.invalidateQueries({ queryKey: lessonKeys.detail(lessonId) }),
        activeCourseId ? queryClient.invalidateQueries({ queryKey: lessonKeys.course(activeCourseId) }) : Promise.resolve(),
      ])
      void message.success('Đã xóa tài liệu.')
    },
    onError: (error: Error) => void message.error(error.message),
  })

  const handleDownloadDocument = async (documentId: string, originalName: string) => {
    if (downloadingId) return
    setDownloadingId(documentId)
    try {
      await downloadLessonDocument(documentId, originalName)
    } catch (error) {
      void message.error(error instanceof Error ? error.message : 'Không tải được tài liệu.')
    } finally {
      setDownloadingId(null)
    }
  }

  const previewFile = (file: UploadFile) => {
    if (!file.originFileObj) return
    const url = URL.createObjectURL(file.originFileObj)
    window.open(url, '_blank', 'noopener,noreferrer')
    window.setTimeout(() => URL.revokeObjectURL(url), 60_000)
  }

  const cancelEdit = () => {
    if (isCreating) {
      bypassBlockerRef.current = true
      navigate(paths.teachingCourseLessons(courseId))
      return
    }
    if (lessonQuery.data) {
      form.setFieldsValue({
        title: lessonQuery.data.title,
        videoUrl: lessonQuery.data.videoUrl,
        transcriptText: lessonQuery.data.transcriptText ?? '',
      })
    }
    setFiles([])
    setDirty(false)
  }

  if (isCreating && !validCourseContext) {
    return (
      <Alert
        type="error"
        showIcon
        title="Không xác định được khóa học"
        description="Hãy mở danh sách bài học của khóa học rồi chọn Thêm bài học."
      />
    )
  }
  if (!isCreating && lessonQuery.isLoading) return <LoadingState tip="Đang tải bài học…" />
  if (!isCreating && lessonQuery.isError) return <ErrorState error={lessonQuery.error} onRetry={() => void lessonQuery.refetch()} />
  if (courseQuery.isLoading || (!isCreating && courseLessonsQuery.isLoading)) {
    return <LoadingState tip="Đang tải trạng thái bài học…" />
  }
  if (courseQuery.isError) return <ErrorState error={courseQuery.error} onRetry={() => void courseQuery.refetch()} />
  if (!isCreating && courseLessonsQuery.isError) {
    return <ErrorState error={courseLessonsQuery.error} onRetry={() => void courseLessonsQuery.refetch()} />
  }

  const savedDocuments = documentsQuery.data ?? []

  return (
    <>
      <Row gutter={[20, 20]} align="top">
        <Col xs={24} lg={15}>
          <Card
            className="r3-card"
            title={isCreating ? 'Thêm bài học mới' : 'Nội dung bài học'}
            extra={
              !isCreating && lessonQuery.data ? (
                <Space size={8} wrap>
                  <LessonStatusTag status={lessonQuery.data.status} />
                  <LessonLifecycleActions
                    lesson={lessonQuery.data}
                    courseStatus={courseQuery.data!.status}
                    publishedCount={publishedCount}
                    hasUnsavedChanges={hasUnsavedChanges}
                    busy={saveMutation.isPending || statusMutation.isPending}
                    onChange={statusMutation.mutateAsync}
                  />
                </Space>
              ) : null
            }
          >
            {readOnly && (
              <Alert
                type="warning"
                showIcon
                title={
                  courseQuery.data?.status === 'Archived'
                    ? 'Khóa đã lưu trữ: chỉ xem nội dung bài học.'
                    : 'Bài học đã lưu trữ: chỉ xem nội dung và lịch sử.'
                }
                style={{ marginBottom: 16 }}
              />
            )}
            {!isCreating && lessonQuery.data?.status === 'DRAFT' && !readOnly && (
              <Alert
                type="info"
                showIcon
                title="Bản nháp chỉ giảng viên nhìn thấy."
                description="Lưu nội dung và tài liệu trước khi xuất bản cho học viên."
                style={{ marginBottom: 16 }}
              />
            )}
            {!isCreating && lessonQuery.data?.status === 'PUBLISHED' && !readOnly && (
              <Alert
                type="info"
                showIcon
                title="Bài học đang hiển thị cho học viên."
                description="Nếu cần thay toàn bộ nội dung, hãy tạo bài mới rồi lưu trữ bài cũ để giữ lịch sử quiz."
                style={{ marginBottom: 16 }}
              />
            )}
            {isCreating && !readOnly && (
              <Alert
                type="info"
                showIcon
                title="Lưu bài học trước khi soạn Quiz hoặc dùng AI & Flashcard"
                description={`Bài học mới sẽ được thêm vào khóa học #${courseId}. Sau khi bấm Tạo bài học thành công, các tab sẽ được mở.`}
                style={{ marginBottom: 16 }}
              />
            )}
            <Form<LessonFormValues>
              form={form}
              layout="vertical"
              onValuesChange={() => setDirty(true)}
              onFinish={(values) => saveMutation.mutate(values)}
              requiredMark="optional"
            >
              <Form.Item
                name="title"
                label="Tên bài học"
                rules={[
                  { required: true, whitespace: true, message: 'Vui lòng nhập tên bài học.' },
                  { max: 200, message: 'Tên bài học không được vượt quá 200 ký tự.' },
                ]}
              >
                <Input
                  placeholder="Ví dụ: JVM và bytecode"
                  disabled={readOnly || saveMutation.isPending || statusMutation.isPending}
                />
              </Form.Item>
              <Form.Item
                name="videoUrl"
                label="Đường dẫn video YouTube"
                rules={[
                  {
                    validator: async (_, value?: string) => {
                      if (isOptionalValidYouTubeUrl(value)) return
                      throw new Error('Vui lòng nhập đúng đường dẫn YouTube (watch, youtu.be, embed hoặc shorts).')
                    },
                  },
                ]}
              >
                <Input
                  placeholder="https://www.youtube.com/watch?v=..."
                  disabled={readOnly || saveMutation.isPending || statusMutation.isPending}
                />
              </Form.Item>
              <Form.Item
                name="transcriptText"
                label="Transcript"
                extra={`Transcript được dùng làm nguồn tạo quiz và flashcard (${transcriptBytes.toLocaleString('vi-VN')} / ${MAX_TRANSCRIPT_UTF8_BYTES.toLocaleString('vi-VN')} byte UTF-8).`}
                rules={[
                  {
                    validator: async (_, value?: string) => {
                      if (!value || utf8ByteLength(value) <= MAX_TRANSCRIPT_UTF8_BYTES) return
                      throw new Error('Transcript vượt quá giới hạn 65.535 byte UTF-8.')
                    },
                  },
                ]}
              >
                <Input.TextArea
                  rows={10}
                  placeholder="Nhập hoặc dán nội dung transcript..."
                  showCount
                  disabled={readOnly || saveMutation.isPending || statusMutation.isPending}
                />
              </Form.Item>
              {!readOnly && (
                <div className="r3-form-footer">
                  <Space size={8}>
                    <Button type="primary" htmlType="submit" loading={saveMutation.isPending}>
                      {isCreating ? 'Tạo bài học' : 'Lưu thay đổi'}
                    </Button>
                    <Button onClick={cancelEdit} disabled={saveMutation.isPending}>
                      Hủy
                    </Button>
                  </Space>
                  {hasUnsavedChanges ? (
                    <Tag color="warning" style={{ margin: 0 }}>
                      Có thay đổi chưa lưu
                    </Tag>
                  ) : null}
                </div>
              )}
            </Form>
          </Card>
        </Col>

        <Col xs={24} lg={9}>
          <div className="r3-editor-side">
            <Card
              className="r3-card"
              title="Xem trước video YouTube"
              extra={
                previewEmbedUrl ? (
                  <Button
                    type="link"
                    size="small"
                    icon={<ExportOutlined />}
                    href={watchedVideoUrl.trim()}
                    target="_blank"
                    rel="noreferrer"
                    style={{ paddingInline: 0 }}
                  >
                    YouTube
                  </Button>
                ) : null
              }
            >
              {previewEmbedUrl ? (
                <div className="r3-video-frame">
                  <iframe
                    title="Xem trước video bài học"
                    src={previewEmbedUrl}
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                    allowFullScreen
                  />
                </div>
              ) : (
                <div className="r3-video-empty">
                  <span className="r3-video-empty-icon" aria-hidden="true">
                    <PlayCircleOutlined />
                  </span>
                  <strong>Chưa có video xem trước</strong>
                  <span>Dán đường dẫn YouTube hợp lệ ở ô bên trái để kiểm tra khả năng nhúng video ngay tại đây.</span>
                </div>
              )}
            </Card>

            <Card
              className="r3-card"
              title={`Tài liệu đính kèm${!isCreating ? ` (${savedDocuments.length})` : ''}`}
              extra={
                !readOnly ? (
                  <Upload
                    multiple
                    accept=".pdf,.docx,.txt"
                    showUploadList={false}
                    fileList={files}
                    disabled={saveMutation.isPending || statusMutation.isPending}
                    beforeUpload={(file) => {
                      const validationError = validateDocumentFile(file)
                      if (validationError) {
                        void message.error(validationError)
                        return Upload.LIST_IGNORE
                      }
                      return false
                    }}
                    onChange={({ fileList }) => setFiles(fileList)}
                  >
                    <Button size="small" icon={<UploadOutlined />} disabled={saveMutation.isPending}>
                      Chọn tệp
                    </Button>
                  </Upload>
                ) : null
              }
            >
              <Typography.Text type="secondary" style={{ display: 'block', fontSize: 12, marginBottom: 12 }}>
                Hỗ trợ PDF, DOCX hoặc TXT · tối đa 10 MiB mỗi tệp. Tệp mới chọn sẽ được tải lên khi bạn bấm Lưu.
              </Typography.Text>

              {files.length > 0 && (
                <div style={{ marginBottom: 14 }}>
                  <Upload
                    multiple
                    accept=".pdf,.docx,.txt"
                    fileList={files}
                    disabled={readOnly || saveMutation.isPending || statusMutation.isPending}
                    beforeUpload={() => false}
                    onChange={({ fileList }) => setFiles(fileList)}
                    onPreview={previewFile}
                  />
                </div>
              )}

              {!isCreating &&
                (documentsQuery.isError ? (
                  <ErrorState error={documentsQuery.error} onRetry={() => void documentsQuery.refetch()} />
                ) : documentsQuery.isLoading ? (
                  <div style={{ textAlign: 'center', padding: '20px 0' }}>
                    <Spin size="small" />
                  </div>
                ) : savedDocuments.length === 0 ? (
                  <div className="r3-video-empty" style={{ aspectRatio: 'auto', padding: '20px 16px' }}>
                    <FileTextOutlined style={{ fontSize: 20, color: 'var(--text-3)' }} />
                    <span>Chưa có tài liệu nào được tải lên cho bài học này.</span>
                  </div>
                ) : (
                  <div className="r3-doc-list">
                    {savedDocuments.map((document) => {
                      const badge = documentTypeBadge(document.mediaType, document.originalName)
                      return (
                        <div key={document.id} className="r3-doc-row">
                          <div className="r3-doc-info">
                            <div className="r3-doc-title">
                              <Tag color={badge.color} style={{ margin: 0, flex: 'none' }}>
                                {badge.label}
                              </Tag>
                              <Tooltip title={document.originalName}>
                                <span className="r3-doc-name">{document.originalName}</span>
                              </Tooltip>
                            </div>
                            <span className="r3-doc-meta">
                              {formatFileSize(document.sizeBytes)} ·{' '}
                              {document.textExtracted ? 'Đã trích xuất text' : 'Không có text'}
                            </span>
                          </div>
                          <div className="r3-doc-actions">
                            <Tooltip title="Tải xuống">
                              <Button
                                type="text"
                                size="small"
                                icon={<DownloadOutlined />}
                                loading={downloadingId === document.id}
                                disabled={Boolean(downloadingId) && downloadingId !== document.id}
                                onClick={() => void handleDownloadDocument(document.id, document.originalName)}
                              />
                            </Tooltip>
                            <Popconfirm
                              title="Xóa tài liệu?"
                              okText="Xóa"
                              cancelText="Hủy"
                              disabled={readOnly || deleteDocumentMutation.isPending}
                              onConfirm={() => deleteDocumentMutation.mutateAsync(document.id)}
                            >
                              <Tooltip title="Xóa tài liệu">
                                <Button
                                  type="text"
                                  size="small"
                                  danger
                                  icon={<DeleteOutlined />}
                                  disabled={readOnly || deleteDocumentMutation.isPending}
                                />
                              </Tooltip>
                            </Popconfirm>
                          </div>
                        </div>
                      )
                    })}
                  </div>
                ))}
            </Card>
          </div>
        </Col>
      </Row>

      <Modal
        open={blocker.state === 'blocked'}
        title="Rời trang khi chưa lưu thay đổi?"
        okText="Rời trang"
        okButtonProps={{ danger: true }}
        cancelText="Ở lại"
        onOk={() => blocker.proceed?.()}
        onCancel={() => blocker.reset?.()}
      >
        <Typography.Paragraph style={{ marginBottom: 0 }}>
          Bạn có nội dung hoặc tài liệu bài học chưa được lưu. Nếu rời trang bây giờ, các chỉnh sửa chưa lưu sẽ bị mất.
        </Typography.Paragraph>
      </Modal>
    </>
  )
}
