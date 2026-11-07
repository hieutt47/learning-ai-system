import { ArrowDownOutlined, ArrowUpOutlined, DeleteOutlined, EditOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Card, Space, Table, Tag, Tooltip, type TableProps } from 'antd'
import { useNavigate, useParams } from 'react-router'
import { EmptyState, ErrorState, LoadingState } from '../../../shared/components/PageState'
import { paths } from '../../../shared/routing/paths'
import { deleteLesson, getCourseLessons, lessonKeys, reorderCourseLessons } from './lesson.api'
import { reorderLessons, type MoveDirection } from './lesson-order'
import type { LessonListItem } from './lesson.types'

export function CourseLessonsPage() {
  const navigate = useNavigate()
  const { courseId = '' } = useParams()
  const queryClient = useQueryClient()
  const { message, modal } = App.useApp()
  const lessonsQuery = useQuery({ queryKey: lessonKeys.course(courseId), queryFn: () => getCourseLessons(courseId), enabled: Boolean(courseId) })
  const lessons = lessonsQuery.data ?? []

  const reorderMutation = useMutation({
    mutationFn: (items: LessonListItem[]) => reorderCourseLessons(courseId, items.map((item) => item.id)),
    onSuccess: (items) => {
      queryClient.setQueryData(lessonKeys.course(courseId), items)
      void message.success('Đã lưu thứ tự bài học.')
    },
    onError: (error: Error) => void message.error(error.message),
  })
  const deleteMutation = useMutation({
    mutationFn: deleteLesson,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: lessonKeys.course(courseId) })
      void message.success('Đã xóa bài học.')
    },
    onError: (error: Error) => void message.error(error.message),
  })

  const moveLesson = (lessonId: string, direction: MoveDirection) => {
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
    { title: 'Transcript', dataIndex: 'hasTranscript', align: 'center', render: (value: boolean) => value ? <Tag color="success">Có</Tag> : <Tag>Chưa có</Tag> },
    { title: 'Tài liệu', dataIndex: 'documentCount', align: 'center' },
    { title: 'Quiz', dataIndex: 'quizCount', align: 'center' },
    { title: 'Flashcard', dataIndex: 'flashcardCount', align: 'center' },
    {
      title: 'Thao tác', key: 'actions', width: 190,
      render: (_, lesson) => (
        <Space>
          <Tooltip title="Chuyển lên"><Button type="text" icon={<ArrowUpOutlined />} disabled={lesson.orderIndex === 1 || reorderMutation.isPending} onClick={() => moveLesson(lesson.id, 'UP')} /></Tooltip>
          <Tooltip title="Chuyển xuống"><Button type="text" icon={<ArrowDownOutlined />} disabled={lesson.orderIndex === lessons.length || reorderMutation.isPending} onClick={() => moveLesson(lesson.id, 'DOWN')} /></Tooltip>
          <Tooltip title="Chỉnh sửa"><Button type="text" icon={<EditOutlined />} onClick={() => navigate(paths.teachingLesson(lesson.id))} /></Tooltip>
          <Tooltip title={lesson.canDelete ? 'Xóa bài học' : lesson.deleteBlockedReason}><span><Button type="text" danger icon={<DeleteOutlined />} disabled={!lesson.canDelete || deleteMutation.isPending} onClick={() => confirmDelete(lesson)} /></span></Tooltip>
        </Space>
      ),
    },
  ]

  if (lessonsQuery.isLoading) return <LoadingState tip="Đang tải bài học…" />
  if (lessonsQuery.isError) return <ErrorState error={lessonsQuery.error} onRetry={() => void lessonsQuery.refetch()} />
  const addButton = <Button type="primary" onClick={() => navigate(paths.teachingLessonNew(courseId))}>+ Thêm bài học</Button>
  return (
    <Card title={`Bài học (${lessons.length})`} extra={addButton}>
      {lessons.length === 0 ? <EmptyState description="Khóa học chưa có bài học." action={addButton} /> : (
        <><Table<LessonListItem> rowKey="id" columns={columns} dataSource={lessons} pagination={false} scroll={{ x: 900 }} /><p>Bài đã có học viên làm quiz không thể xóa để bảo toàn lịch sử học tập.</p></>
      )}
    </Card>
  )
}
