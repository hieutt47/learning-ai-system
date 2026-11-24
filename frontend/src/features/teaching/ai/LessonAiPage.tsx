import { useRef } from 'react'
import { Alert, Space } from 'antd'
import { useParams } from 'react-router'
import { ErrorState, LoadingState } from '../../../shared/components/PageState'
import { useLessonEditability } from '../lessons/useLessonEditability'
import { FlashcardTable, type FlashcardTableHandle } from './FlashcardTable'
import { GeneratePanel } from './GeneratePanel'

export function LessonAiPage() {
  const { lessonId = '' } = useParams()
  const tableRef = useRef<FlashcardTableHandle>(null)
  const editability = useLessonEditability(lessonId)

  if (editability.loading) return <LoadingState tip="Đang tải bài học…" />
  if (editability.error) return <ErrorState error={editability.error} onRetry={editability.retry} />

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      {editability.readOnly
        ? <Alert type="warning" showIcon message="Bài học hoặc khóa học đã lưu trữ: AI và flashcard chỉ xem." />
        : <GeneratePanel lessonId={lessonId} onManualCard={() => tableRef.current?.openCreate()} />}
      <FlashcardTable ref={tableRef} lessonId={lessonId} readOnly={editability.readOnly} />
    </Space>
  )
}
