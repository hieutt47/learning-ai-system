import { DownloadOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { App, Button, Card, Tag, Tooltip } from 'antd'
import { useState } from 'react'
import { useParams } from 'react-router'
import { EmptyState, ErrorState, LoadingState } from '../../shared/components/PageState'
import { documentTypeBadge, formatFileSize } from '../teaching/lessons/document-format'
import { downloadLessonDocument, getLessonDocuments, lessonKeys } from '../teaching/lessons/lesson.api'
import '../teaching/lessons/lessons-ui.css'

export function LearningDocumentsPage() {
  const { lessonId = '' } = useParams()
  const { message } = App.useApp()
  const [downloadingId, setDownloadingId] = useState<string | null>(null)
  const documentsQuery = useQuery({
    queryKey: lessonKeys.documents(lessonId),
    queryFn: () => getLessonDocuments(lessonId),
    enabled: Boolean(lessonId),
  })

  const handleDownload = async (documentId: string, originalName: string) => {
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

  if (documentsQuery.isLoading) return <LoadingState tip="Đang tải tài liệu…" />
  if (documentsQuery.isError) return <ErrorState error={documentsQuery.error} onRetry={() => void documentsQuery.refetch()} />
  const documents = documentsQuery.data ?? []

  return (
    <Card className="r3-card" title={`Tài liệu bài học (${documents.length})`}>
      {documents.length === 0 ? (
        <EmptyState description="Bài học chưa có tài liệu." />
      ) : (
        <div className="r3-doc-list">
          {documents.map((document) => {
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
                  <span className="r3-doc-meta">{formatFileSize(document.sizeBytes)}</span>
                </div>
                <div className="r3-doc-actions">
                  <Button
                    type="primary"
                    size="small"
                    icon={<DownloadOutlined />}
                    loading={downloadingId === document.id}
                    disabled={Boolean(downloadingId) && downloadingId !== document.id}
                    onClick={() => void handleDownload(document.id, document.originalName)}
                  >
                    Tải xuống
                  </Button>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </Card>
  )
}
