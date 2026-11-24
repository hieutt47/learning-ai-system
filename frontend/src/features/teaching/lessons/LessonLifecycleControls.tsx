import { App, Button, Space, Tag, Tooltip } from 'antd'
import type { CourseStatus } from '../../marketplace/course.types'
import { lessonActionDisabledReason, lessonStatusLabels } from './lesson-lifecycle'
import type { LessonListItem, LessonStatus } from './lesson.types'
import type { LessonStatusChange } from './useLessonStatusMutation'

const statusColors: Record<LessonStatus, string> = {
  DRAFT: 'warning',
  PUBLISHED: 'success',
  ARCHIVED: 'default',
}

export function LessonStatusTag({ status }: { status: LessonStatus }) {
  return <Tag color={statusColors[status]}>{lessonStatusLabels[status]}</Tag>
}

export function LessonLifecycleActions({
  lesson, courseStatus, publishedCount, busy, hasUnsavedChanges = false, onChange,
}: {
  lesson: LessonListItem
  courseStatus: CourseStatus
  publishedCount: number
  busy: boolean
  hasUnsavedChanges?: boolean
  onChange: (change: LessonStatusChange) => Promise<unknown>
}) {
  const { modal } = App.useApp()
  if (lesson.status === 'ARCHIVED') return null

  const reason = lessonActionDisabledReason(lesson, courseStatus, publishedCount, hasUnsavedChanges)
  const publish = lesson.status === 'DRAFT'
  const target = publish ? 'PUBLISHED' : 'ARCHIVED'
  const confirm = () => modal.confirm({
    title: publish ? 'Xuất bản bài học?' : 'Lưu trữ bài học?',
    content: publish
      ? `Sau khi xuất bản, học viên đã ghi danh sẽ thấy bài “${lesson.title}” và nội dung đã lưu.`
      : `Bài “${lesson.title}” sẽ biến mất khỏi luồng học. Học viên không thể bắt đầu quiz mới của bài này; lịch sử quiz vẫn được giữ. Không thể xuất bản lại bài đã lưu trữ.`,
    okText: publish ? 'Xuất bản' : 'Lưu trữ',
    okButtonProps: publish ? undefined : { danger: true },
    cancelText: 'Hủy',
    onOk: () => onChange({ lessonId: lesson.id, status: target }),
  })

  return (
    <Space>
      <Tooltip title={reason ?? undefined}>
        <span>
          <Button size="small" type={publish ? 'primary' : 'default'} danger={!publish}
            disabled={Boolean(reason) || busy} loading={busy} onClick={confirm}>
            {publish ? 'Xuất bản' : 'Lưu trữ'}
          </Button>
        </span>
      </Tooltip>
    </Space>
  )
}
