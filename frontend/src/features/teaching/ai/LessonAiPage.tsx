import { useRef } from 'react'
import { Space } from 'antd'
import { useParams } from 'react-router'
import { FlashcardTable, type FlashcardTableHandle } from './FlashcardTable'
import { GeneratePanel } from './GeneratePanel'

export function LessonAiPage() {
  const { lessonId = '' } = useParams()
  const tableRef = useRef<FlashcardTableHandle>(null)

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <GeneratePanel lessonId={lessonId} onManualCard={() => tableRef.current?.openCreate()} />
      <FlashcardTable ref={tableRef} lessonId={lessonId} />
    </Space>
  )
}
