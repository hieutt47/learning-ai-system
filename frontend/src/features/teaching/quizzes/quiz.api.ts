import { apiDelete, apiGet, apiPatch, apiPost, apiPut } from '../../../shared/api/client'
import type { Id, IsoDateTime, Page } from '../../../shared/api/types'
import type {
  QuestionWriteRequest,
  QuizAuthorDetail,
  QuizCreateRequest,
  QuizOptionAuthor,
  QuizQuestionAuthor,
  QuizStatus,
  QuizSummaryItem,
  QuizUpdateRequest,
} from './quiz.types'

interface QuizSummaryWire {
  id: string | number
  lesson_id: string | number
  title: string
  status: QuizStatus
  question_count: number
}

interface OptionAuthorWire {
  id?: string | number
  content: string
  is_correct: boolean
  order_index: number
}

interface QuestionAuthorWire {
  id?: string | number
  content: string
  explanation?: string | null
  order_index: number
  generated_by_ai?: boolean
  options: OptionAuthorWire[]
}

interface QuizAuthorWire {
  id: string | number
  lesson_id: string | number
  title: string
  status: QuizStatus
  created_at: IsoDateTime
  questions: QuestionAuthorWire[]
}

const mapOption = (opt: OptionAuthorWire): QuizOptionAuthor => ({
  id: opt.id != null ? String(opt.id) : undefined,
  content: opt.content,
  isCorrect: opt.is_correct,
  orderIndex: opt.order_index,
})

const mapQuestion = (q: QuestionAuthorWire): QuizQuestionAuthor => ({
  id: q.id != null ? String(q.id) : undefined,
  content: q.content,
  explanation: q.explanation ?? undefined,
  orderIndex: q.order_index,
  generatedByAi: q.generated_by_ai,
  options: (q.options ?? []).map(mapOption),
})

const mapQuizAuthor = (wire: QuizAuthorWire): QuizAuthorDetail => ({
  id: String(wire.id),
  lessonId: String(wire.lesson_id),
  title: wire.title,
  status: wire.status,
  createdAt: wire.created_at,
  questions: (wire.questions ?? []).map(mapQuestion),
})

const mapSummary = (wire: QuizSummaryWire): QuizSummaryItem => ({
  id: String(wire.id),
  lessonId: String(wire.lesson_id),
  title: wire.title,
  status: wire.status,
  questionCount: wire.question_count,
})

export const quizKeys = {
  all: ['quizzes'] as const,
  lesson: (lessonId: Id) => ['quizzes', 'lesson', lessonId] as const,
  detail: (quizId: Id) => ['quizzes', 'detail', quizId] as const,
}

export async function getLessonQuizzes(lessonId: Id, page = 1, pageSize = 100): Promise<Page<QuizSummaryItem>> {
  const wirePage = await apiGet<Page<QuizSummaryWire>>(`/lessons/${lessonId}/quizzes`, {
    query: { page, page_size: pageSize },
  })
  return {
    ...wirePage,
    items: wirePage.items.map(mapSummary),
  }
}

export async function getQuiz(quizId: Id): Promise<QuizAuthorDetail> {
  const wire = await apiGet<QuizAuthorWire>(`/quizzes/${quizId}`)
  return mapQuizAuthor(wire)
}

export async function createQuiz(lessonId: Id, data: QuizCreateRequest): Promise<QuizAuthorDetail> {
  const wire = await apiPost<QuizAuthorWire>(`/lessons/${lessonId}/quizzes`, data)
  return mapQuizAuthor(wire)
}

export async function updateQuiz(quizId: Id, data: QuizUpdateRequest): Promise<QuizAuthorDetail> {
  const wire = await apiPatch<QuizAuthorWire>(`/quizzes/${quizId}`, data)
  return mapQuizAuthor(wire)
}

export async function deleteQuiz(quizId: Id): Promise<void> {
  await apiDelete(`/quizzes/${quizId}`)
}

export async function publishQuiz(quizId: Id, status: 'PUBLISHED' | 'CLOSED'): Promise<QuizAuthorDetail> {
  const wire = await apiPatch<QuizAuthorWire>(`/quizzes/${quizId}/publication`, { status })
  return mapQuizAuthor(wire)
}

export async function addQuestion(quizId: Id, data: QuestionWriteRequest): Promise<QuizQuestionAuthor> {
  const wire = await apiPost<QuestionAuthorWire>(`/quizzes/${quizId}/questions`, data)
  return mapQuestion(wire)
}

export async function updateQuestion(questionId: Id, data: QuestionWriteRequest): Promise<QuizQuestionAuthor> {
  const wire = await apiPut<QuestionAuthorWire>(`/questions/${questionId}`, data)
  return mapQuestion(wire)
}

export async function deleteQuestion(questionId: Id): Promise<void> {
  await apiDelete(`/questions/${questionId}`)
}
