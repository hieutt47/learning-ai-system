import {
  apiDelete,
  apiGetBlob,
  apiGet,
  apiPatch,
  apiPost,
  apiPut,
} from '../../../shared/api/client'
import type { Id, IsoDateTime } from '../../../shared/api/types'
import type {
  LessonDocumentItem,
  LessonFormValues,
  LessonListItem,
} from './lesson.types'

interface LessonWire {
  id: number | string
  course_id: number | string
  title: string
  video_url: string
  transcript_text?: string | null
  order_index: number
  document_count: number
  quiz_count: number
  flashcard_count: number
  can_delete: boolean
  delete_blocked_reason?: string | null
}

interface LessonDocumentWire {
  id: number | string
  lesson_id: number | string
  original_name: string
  media_type: string
  size_bytes: number
  text_extracted: boolean
  created_at: IsoDateTime
}

const mapLesson = (lesson: LessonWire): LessonListItem => ({
  id: String(lesson.id),
  courseId: String(lesson.course_id),
  title: lesson.title,
  videoUrl: lesson.video_url,
  transcriptText: lesson.transcript_text ?? undefined,
  orderIndex: lesson.order_index,
  hasTranscript: Boolean(lesson.transcript_text?.trim()),
  documentCount: lesson.document_count,
  quizCount: lesson.quiz_count,
  flashcardCount: lesson.flashcard_count,
  canDelete: lesson.can_delete,
  deleteBlockedReason: lesson.delete_blocked_reason ?? undefined,
})

const mapDocument = (document: LessonDocumentWire): LessonDocumentItem => ({
  id: String(document.id),
  lessonId: String(document.lesson_id),
  originalName: document.original_name,
  mediaType: document.media_type,
  sizeBytes: document.size_bytes,
  textExtracted: document.text_extracted,
  createdAt: document.created_at,
})

const writeBody = (values: LessonFormValues) => ({
  title: values.title.trim(),
  video_url: values.videoUrl.trim(),
  transcript_text: values.transcriptText?.trim() || null,
})

export const lessonKeys = {
  course: (courseId: Id) => ['lessons', 'course', courseId] as const,
  detail: (lessonId: Id) => ['lessons', 'detail', lessonId] as const,
  documents: (lessonId: Id) => ['lessons', 'documents', lessonId] as const,
}

export async function getCourseLessons(courseId: Id): Promise<LessonListItem[]> {
  return (await apiGet<LessonWire[]>(`/courses/${courseId}/lessons`)).map(mapLesson)
}

export async function getLearningLessons(courseId: Id): Promise<LessonListItem[]> {
  return getCourseLessons(courseId)
}

export async function getLesson(lessonId: Id): Promise<LessonListItem> {
  return mapLesson(await apiGet<LessonWire>(`/lessons/${lessonId}`))
}

export async function createLesson(courseId: Id, values: LessonFormValues): Promise<LessonListItem> {
  return mapLesson(await apiPost<LessonWire>(`/courses/${courseId}/lessons`, writeBody(values)))
}

export async function updateLesson(lessonId: Id, values: LessonFormValues): Promise<LessonListItem> {
  return mapLesson(await apiPut<LessonWire>(`/lessons/${lessonId}`, writeBody(values)))
}

export async function reorderCourseLessons(courseId: Id, lessonIds: Id[]): Promise<LessonListItem[]> {
  return (await apiPatch<LessonWire[]>(`/courses/${courseId}/lessons/order`, { lesson_ids: lessonIds })).map(mapLesson)
}

export const deleteLesson = (lessonId: Id) => apiDelete(`/lessons/${lessonId}`)

export async function getLessonDocuments(lessonId: Id): Promise<LessonDocumentItem[]> {
  return (await apiGet<LessonDocumentWire[]>(`/lessons/${lessonId}/documents`)).map(mapDocument)
}

export async function uploadLessonDocument(lessonId: Id, file: File): Promise<LessonDocumentItem> {
  const body = new FormData()
  body.append('file', file)
  return mapDocument(await apiPost<LessonDocumentWire>(`/lessons/${lessonId}/documents`, body))
}

export const deleteLessonDocument = (documentId: Id) => apiDelete(`/documents/${documentId}`)

export async function downloadLessonDocument(documentId: Id, originalName: string): Promise<void> {
  const blob = await apiGetBlob(`/documents/${documentId}/content`)
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = originalName
  anchor.click()
  URL.revokeObjectURL(url)
}
