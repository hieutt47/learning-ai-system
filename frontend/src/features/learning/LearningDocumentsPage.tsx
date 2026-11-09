import { DownloadOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { App, Button, Card, List } from 'antd'
import { useParams } from 'react-router'
import { EmptyState, ErrorState, LoadingState } from '../../shared/components/PageState'
import { downloadLessonDocument, getLessonDocuments, lessonKeys } from '../teaching/lessons/lesson.api'

export function LearningDocumentsPage() {
  const { lessonId = '' } = useParams()
  const { message } = App.useApp()
  const documentsQuery = useQuery({ queryKey: lessonKeys.documents(lessonId), queryFn: () => getLessonDocuments(lessonId), enabled: Boolean(lessonId) })
  if (documentsQuery.isLoading) return <LoadingState tip="Đang tải tài liệu…" />
  if (documentsQuery.isError) return <ErrorState error={documentsQuery.error} onRetry={() => void documentsQuery.refetch()} />
  const documents = documentsQuery.data ?? []
  return (
    <Card title="Tài liệu bài học">
      {documents.length === 0 ? <EmptyState description="Bài học chưa có tài liệu." /> : (
        <List dataSource={documents} renderItem={(document) => (
          <List.Item actions={[<Button key="download" type="primary" icon={<DownloadOutlined />} onClick={() => void downloadLessonDocument(document.id, document.originalName).catch((error: Error) => message.error(error.message))}>Tải xuống</Button>]}>
            <List.Item.Meta title={document.originalName} description={`${(document.sizeBytes / 1024).toFixed(1)} KB`} />
          </List.Item>
        )} />
      )}
    </Card>
  )
}
