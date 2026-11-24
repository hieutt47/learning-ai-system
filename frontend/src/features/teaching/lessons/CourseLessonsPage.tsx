import { ArrowDownOutlined, ArrowUpOutlined, DeleteOutlined, EditOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, App, Button, Card, Space, Table, Tag, Tooltip, type TableProps } from 'antd'
import { Link, useNavigate, useParams } from 'react-router'
import { EmptyState, ErrorState, LoadingState } from '../../../shared/components/PageState'
import { paths } from '../../../shared/routing/paths'
import { courseKeys, getOwnedCourse } from '../../marketplace/course.api'
import { LessonLifecycleActions, LessonStatusTag } from './LessonLifecycleControls'
import { deleteLesson, getCourseLessons, lessonKeys, reorderCourseLessons } from './lesson.api'
import { reorderLessons, type MoveDirection } from './lesson-order'
import type { LessonListItem } from './lesson.types'
import { useLessonStatusMutation } from './useLessonStatusMutation'

export function CourseLessonsPage() {
  const navigate = useNavigate()
  const { courseId = '' } = useParams()
  const queryClient = useQueryClient()
  const { message, modal } = App.useApp()
  const courseQuery = useQuery({ queryKey: courseKeys.owned(courseId), queryFn: () => getOwnedCourse(courseId), enabled: Boolean(courseId) })
  const lessonsQuery = useQuery({ queryKey: lessonKeys.course(courseId), queryFn: () => getCourseLessons(courseId), enabled: Boolean(courseId) })
  const lessons = lessonsQuery.data ?? []
  const courseStatus = courseQuery.data?.status ?? 'Draft'
  const publishedCount = lessons.filter((lesson) => lesson.status === 'PUBLISHED').length
  const readOnly = courseStatus === 'Archived'
  const statusMutation = useLessonStatusMutation()

  const reorderMutation = useMutation({
    mutationFn: (items: LessonListItem[]) => reorderCourseLessons(courseId, items.map((item) => item.id)),
    onSuccess: (items) => {
      queryClient.setQueryData(lessonKeys.course(courseId), items)
      void queryClient.invalidateQueries({ queryKey: courseKeys.publicDetail(courseId) })
      void message.success('Đã lưu thứ tự bài học.')
    },
    onError: (error: Error) => void message.error(error.message),
  })
  const deleteMutation = useMutation({
    mutationFn: deleteLesson,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: lessonKeys.course(courseId) }),
        queryClient.invalidateQueries({ queryKey: ['courses'] }),
      ])
      void message.success('Đã xóa bài học.')
    },
    onError: (error: Error) => void message.error(error.message),
  })

  const actionBusy = reorderMutation.isPending || deleteMutation.isPending || statusMutation.isPending

  const moveLesson = (lessonId: string, direction: MoveDirection) => {
    if (readOnly || actionBusy) return
    reorderMutation.mutate(reorderLessons(lessons, lessonId, direction))
  }
  const confirmDelete = (lesson: LessonListItem) => {
    modal.confirm({
      title: 'Xóa bài học?',
      content: `Bài “${lesson.title}” và tài liệu của bài sẽ bị xóa. Không thể hoàn tác.`,
      okText: 'Xóa bài học',
      okButtonProps: { danger: true },
      cancelText: 'Hủy',
      onOk: () => deleteMutation.mutateAsync(lesson.id),
    })
  }
  const columns: TableProps<LessonListItem>['columns'] = [
    { title: '#', dataIndex: 'orderIndex', width: 70, align: 'center' },
    { title: 'Tên bài', dataIndex: 'title', render: (title, lesson) => <Button type="link" onClick={() => navigate(paths.teachingLesson(lesson.id))}>{title}</Button> },
    { title: 'Trạng thái', dataIndex: 'status', width: 150, render: (status: LessonListItem['status']) => <LessonStatusTag status={status} /> },
    { title: 'Transcript', dataIndex: 'hasTranscript', align: 'center', render: (value: boolean) => value ? <Tag color="success">Có</Tag> : <Tag>Chưa có</Tag> },
    { title: 'Tài liệu', dataIndex: 'documentCount', align: 'center' },
    { title: 'Quiz', dataIndex: 'quizCount', align: 'center' },
    { title: 'Flashcard', dataIndex: 'flashcardCount', align: 'center' },
    {
      title: 'Thao tác', key: 'actions', width: 360,
      render: (_, lesson) => (
        <Space wrap>
          <Tooltip title="Chuyển lên"><Button type="text" icon={<ArrowUpOutlined />} disabled={readOnly || lesson.orderIndex === 1 || actionBusy} onClick={() => moveLesson(lesson.id, 'UP')} /></Tooltip>
          <Tooltip title="Chuyển xuống"><Button type="text" icon={<ArrowDownOutlined />} disabled={readOnly || lesson.orderIndex === lessons.length || actionBusy} onClick={() => moveLesson(lesson.id, 'DOWN')} /></Tooltip>
          <Tooltip title="Chỉnh sửa"><Button type="text" icon={<EditOutlined />} onClick={() => navigate(paths.teachingLesson(lesson.id))} /></Tooltip>
          <LessonLifecycleActions lesson={lesson} courseStatus={courseStatus} publishedCount={publishedCount}
            busy={actionBusy} onChange={statusMutation.mutateAsync} />
          <Tooltip title={readOnly ? 'Khóa học đã lưu trữ, chỉ có thể xem.' : lesson.canDelete ? 'Xóa bài học' : lesson.deleteBlockedReason}><span><Button type="text" danger icon={<DeleteOutlined />} disabled={readOnly || !lesson.canDelete || actionBusy} onClick={() => confirmDelete(lesson)} /></span></Tooltip>
        </Space>
      ),
    },
  ]

  if (courseQuery.isLoading || lessonsQuery.isLoading) return <LoadingState tip="Đang tải bài học…" />
  if (courseQuery.isError) return <ErrorState error={courseQuery.error} onRetry={() => void courseQuery.refetch()} />
  if (lessonsQuery.isError) return <ErrorState error={lessonsQuery.error} onRetry={() => void lessonsQuery.refetch()} />
  const addButton = <Button type="primary" disabled={readOnly} onClick={() => navigate(paths.teachingLessonNew(courseId))}>+ Thêm bài học</Button>
  return (
    <Card title={`Bài học (${lessons.length})`} extra={addButton}>
      <Alert type={readOnly ? 'warning' : 'info'} showIcon style={{ marginBottom: 16 }}
        message={readOnly ? 'Khóa đã lưu trữ: chỉ xem bài học.' : courseStatus === 'Published' ? `${publishedCount} bài đang hiển thị cho học viên.` : 'Khóa chưa xuất bản: học viên chưa thấy bài học.'}
        description={readOnly ? 'Quiz và lịch sử học vẫn được giữ.' : courseStatus === 'Published'
          ? <>Bài mới được tạo ở trạng thái bản nháp. Hãy xuất bản bài mới trước khi lưu trữ bài hiển thị cuối cùng. Nếu cần ẩn toàn bộ nội dung, <Link to={paths.teachingCourse(courseId)}>gỡ xuất bản khóa</Link> trước.</>
          : 'Bài mới trong khóa chưa xuất bản được đặt sẵn trạng thái xuất bản; học viên chỉ thấy khi khóa được xuất bản.'} />
      {lessons.length === 0 ? <EmptyState description="Khóa học chưa có bài học." action={addButton} /> : (
        <><Table<LessonListItem> rowKey="id" columns={columns} dataSource={lessons} pagination={false} scroll={{ x: 1000 }} /><p>Bài đã có lịch sử quiz không thể xóa; hãy dùng Lưu trữ để giữ kết quả học tập.</p></>
      )}
    </Card>
  )
}
