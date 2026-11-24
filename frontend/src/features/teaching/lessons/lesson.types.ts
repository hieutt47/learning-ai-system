import type { Id, IsoDateTime } from '../../../shared/api/types'

export type LessonStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'

export interface LessonListItem {
  id: Id
  courseId: Id
  title: string
  status: LessonStatus
  videoUrl: string
  transcriptText?: string
  orderIndex: number
  hasTranscript: boolean
  documentCount: number
  quizCount: number
  flashcardCount: number
  canDelete: boolean
  deleteBlockedReason?: string
}

export interface LessonDocumentItem {
  id: Id
  lessonId: Id
  originalName: string
  mediaType: string
  sizeBytes: number
  textExtracted: boolean
  createdAt: IsoDateTime
}

export interface LessonFormValues {
  title: string
  videoUrl: string
  transcriptText?: string
}
