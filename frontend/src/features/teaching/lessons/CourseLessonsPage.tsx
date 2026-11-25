import {
  ArrowDownOutlined,
  ArrowUpOutlined,
  DeleteOutlined,
  EditOutlined,
  InfoCircleOutlined,
  PlayCircleOutlined,
  PlusOutlined,
} from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, App, Button, Card, Table, Tag, Tooltip, type TableProps } from 'antd'
import { Link, useNavigate, useParams } from 'react-router'
import { EmptyState, ErrorState, LoadingState } from '../../../shared/components/PageState'
import { usePageHeader } from '../../../shared/components/pageHeader'
import { paths } from '../../../shared/routing/paths'
import { courseKeys, getOwnedCourse } from '../../marketplace/course.api'
import { LessonLifecycleActions, LessonStatusTag } from './LessonLifecycleControls'
import { deleteLesson, getCourseLessons, lessonKeys, reorderCourseLessons } from './lesson.api'
import { canMoveLesson, reorderLessons, type MoveDirection } from './lesson-order'
import type { LessonListItem } from './lesson.types'
import { useLessonStatusMutation } from './useLessonStatusMutation'
import './lessons-ui.css'

export function CourseLessonsPage() {
  const navigate = useNavigate()
  const { courseId = '' } = useParams()
  const queryClient = useQueryClient()
  const { message, modal } = App.useApp()
  const courseQuery = useQuery({
    queryKey: courseKeys.owned(courseId),
    queryFn: () => getOwnedCourse(courseId),
    enabled: Boolean(courseId),
  })
  const lessonsQuery = useQuery({
    queryKey: lessonKeys.course(courseId),
    queryFn: () => getCourseLessons(courseId),
    enabled: Boolean(courseId),
  })

  usePageHeader(
    courseQuery.data
      ? {
          title: courseQuery.data.title,
          crumbs: [
            { label: 'Khóa học giảng dạy', to: paths.teachingCourses },
            { label: courseQuery.data.title, to: paths.teachingCourse(courseId) },
            { label: 'Bài học' },
          ],
        }
      : null,
  )

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
    if (readOnly || actionBusy || !canMoveLesson(lessons, lessonId, direction)) return
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
    {
      title: '#',
      dataIndex: 'orderIndex',
      width: 60,
      align: 'center',
      render: (order: number) => <span className="r3-order-pill">{order}</span>,
    },
    {
      title: 'Tên bài',
      dataIndex: 'title',
      render: (title: string, lesson) => (
        <Link to={paths.teachingLesson(lesson.id)} className="r3-lesson-title-link">
          <span>{title}</span>
          {lesson.videoUrl ? (
            <Tooltip title="Có video YouTube">
              <PlayCircleOutlined className="r3-video-dot" />
            </Tooltip>
          ) : null}
        </Link>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      width: 132,
      align: 'center',
      render: (status: LessonListItem['status']) => <LessonStatusTag status={status} />,
    },
    {
      title: 'Transcript',
      dataIndex: 'hasTranscript',
      width: 108,
      align: 'center',
      render: (value: boolean) => (
        <Tag color={value ? 'success' : 'default'} style={{ margin: 0 }}>
          {value ? 'Có' : 'Chưa có'}
        </Tag>
      ),
    },
    {
      title: 'Tài liệu',
      dataIndex: 'documentCount',
      width: 84,
      align: 'center',
      render: (count: number) => <span className={`r3-count${count === 0 ? ' is-zero' : ''}`}>{count}</span>,
    },
    {
      title: 'Quiz',
      dataIndex: 'quizCount',
      width: 72,
      align: 'center',
      render: (count: number) => <span className={`r3-count${count === 0 ? ' is-zero' : ''}`}>{count}</span>,
    },
    {
      title: 'Flashcard',
      dataIndex: 'flashcardCount',
      width: 92,
      align: 'center',
      render: (count: number) => <span className={`r3-count${count === 0 ? ' is-zero' : ''}`}>{count}</span>,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 280,
      align: 'right',
      render: (_, lesson) => {
        const canMoveUp = !readOnly && canMoveLesson(lessons, lesson.id, 'UP')
        const canMoveDown = !readOnly && canMoveLesson(lessons, lesson.id, 'DOWN')
        const moveTip =
          lesson.status === 'ARCHIVED'
            ? 'Bài đã lưu trữ không thể đổi thứ tự.'
            : readOnly
              ? 'Khóa đã lưu trữ, chỉ có thể xem.'
              : undefined
        return (
          <div className="r3-row-actions">
            <Tooltip title={moveTip ?? (canMoveUp ? 'Chuyển lên' : 'Không thể chuyển lên')}>
              <span>
                <Button
                  type="text"
                  size="small"
                  icon={<ArrowUpOutlined />}
                  disabled={!canMoveUp || actionBusy}
                  onClick={() => moveLesson(lesson.id, 'UP')}
                />
              </span>
            </Tooltip>
            <Tooltip title={moveTip ?? (canMoveDown ? 'Chuyển xuống' : 'Không thể chuyển xuống')}>
              <span>
                <Button
                  type="text"
                  size="small"
                  icon={<ArrowDownOutlined />}
                  disabled={!canMoveDown || actionBusy}
                  onClick={() => moveLesson(lesson.id, 'DOWN')}
                />
              </span>
            </Tooltip>
            <span className="r3-action-divider" aria-hidden="true" />
            <Tooltip title={lesson.status === 'ARCHIVED' || readOnly ? 'Xem nội dung' : 'Chỉnh sửa'}>
              <Button
                type="text"
                size="small"
                icon={<EditOutlined />}
                onClick={() => navigate(paths.teachingLesson(lesson.id))}
              />
            </Tooltip>
            <LessonLifecycleActions
              lesson={lesson}
              courseStatus={courseStatus}
              publishedCount={publishedCount}
              busy={actionBusy}
              onChange={statusMutation.mutateAsync}
            />
            <Tooltip
              title={
                readOnly
                  ? 'Khóa học đã lưu trữ, chỉ có thể xem.'
                  : lesson.canDelete
                    ? 'Xóa bài học'
                    : lesson.deleteBlockedReason
              }
            >
              <span>
                <Button
                  type="text"
                  size="small"
                  danger
                  icon={<DeleteOutlined />}
                  disabled={readOnly || !lesson.canDelete || actionBusy}
                  onClick={() => confirmDelete(lesson)}
                />
              </span>
            </Tooltip>
          </div>
        )
      },
    },
  ]

  if (courseQuery.isLoading || lessonsQuery.isLoading) return <LoadingState tip="Đang tải bài học…" />
  if (courseQuery.isError) return <ErrorState error={courseQuery.error} onRetry={() => void courseQuery.refetch()} />
  if (lessonsQuery.isError) return <ErrorState error={lessonsQuery.error} onRetry={() => void lessonsQuery.refetch()} />
  const addButton = (
    <Button
      type="primary"
      icon={<PlusOutlined />}
      disabled={readOnly}
      onClick={() => navigate(paths.teachingLessonNew(courseId))}
    >
      Thêm bài học
    </Button>
  )
  return (
    <Card className="r3-card" title={`Bài học (${lessons.length})`} extra={addButton}>
      <Alert
        type={readOnly ? 'warning' : 'info'}
        showIcon
        style={{ marginBottom: 16 }}
        title={
          readOnly
            ? 'Khóa đã lưu trữ: chỉ xem bài học.'
            : courseStatus === 'Published'
              ? `${publishedCount} bài đang hiển thị cho học viên.`
              : 'Khóa chưa xuất bản: học viên chưa thấy bài học.'
        }
        description={
          readOnly ? (
            'Quiz và lịch sử học vẫn được giữ.'
          ) : courseStatus === 'Published' ? (
            <>
              Bài mới được tạo ở trạng thái bản nháp. Hãy xuất bản bài mới trước khi lưu trữ bài hiển thị cuối cùng. Nếu
              cần ẩn toàn bộ nội dung, <Link to={paths.teachingCourse(courseId)}>gỡ xuất bản khóa</Link> trước.
            </>
          ) : (
            'Bài mới trong khóa chưa xuất bản được đặt sẵn trạng thái xuất bản; học viên chỉ thấy khi khóa được xuất bản.'
          )
        }
      />
      {lessons.length === 0 ? (
        <EmptyState description="Khóa học chưa có bài học." action={addButton} />
      ) : (
        <>
          <Table<LessonListItem>
            className="r3-table"
            rowKey="id"
            columns={columns}
            dataSource={lessons}
            pagination={false}
            scroll={{ x: 960 }}
          />
          <p className="r3-footnote">
            <InfoCircleOutlined />
            <span>Bài đã có lịch sử quiz không thể xóa; hãy dùng Lưu trữ để giữ kết quả học tập.</span>
          </p>
        </>
      )}
    </Card>
  )
}
