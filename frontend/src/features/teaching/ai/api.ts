import { apiDelete, apiGet, apiPatch, apiPost } from '../../../shared/api/client'
import type { Page } from '../../../shared/api/types'

export interface Flashcard {
  id: string
  lesson_id: string
  front_text: string
  back_text: string
  status: 'DRAFT' | 'PUBLISHED' | 'CLOSED'
  generated_by_ai: boolean
  created_at: string
}

export interface Document {
  id: string
  lesson_id: string
  original_name: string
  media_type: string
  size_bytes: number
  created_at: string
}

export interface QuizQuestion {
  content: string
  explanation: string
  options: { content: string; is_correct: boolean }[]
}

export interface GeneratedQuiz {
  generation_id: string
  lesson_id: string
  source_truncated: boolean
  candidate: { questions: QuizQuestion[] }
}

export interface GeneratedCards {
  generation_id: string
  lesson_id: string
  source_truncated: boolean
  candidate: { flashcards: Pick<Flashcard, 'front_text' | 'back_text'>[] }
}

export interface GenerationRequest {
  lesson_id: string
  source_type: 'TRANSCRIPT' | 'DOCUMENT'
  document_id?: string
  count: number
  difficulty?: 'EASY' | 'MEDIUM' | 'HARD'
}

export interface CardWrite {
  front_text: string
  back_text: string
  generation_id?: string
}

export interface QuestionWrite {
  content: string
  explanation: string
  order_index: number
  options: { content: string; is_correct: boolean; order_index: number }[]
  generation_id: string
}

export const getDocuments = (lessonId: string) =>
  apiGet<Page<Document>>(`/lessons/${lessonId}/documents`)

export const getFlashcards = (lessonId: string, page: number, pageSize = 100) =>
  apiGet<Page<Flashcard>>(`/lessons/${lessonId}/flashcards`, {
    query: { page, page_size: pageSize },
  })

export const generateQuiz = (body: GenerationRequest, signal?: AbortSignal) =>
  apiPost<GeneratedQuiz>('/ai/generate-quiz', body, { signal })

export const generateCards = (body: GenerationRequest, signal?: AbortSignal) =>
  apiPost<GeneratedCards>('/ai/generate-flashcards', body, { signal })

export const createCard = (lessonId: string, body: CardWrite) =>
  apiPost<Flashcard>(`/lessons/${lessonId}/flashcards`, body)

export const updateCard = (id: string, body: Pick<CardWrite, 'front_text' | 'back_text'>) =>
  apiPatch<Flashcard>(`/flashcards/${id}`, body)

export const deleteCard = (id: string) => apiDelete(`/flashcards/${id}`)

export const publishCard = (id: string, status: 'PUBLISHED' | 'CLOSED') =>
  apiPatch<Flashcard>(`/flashcards/${id}/publication`, { status })

export const createQuiz = (lessonId: string, body: { title: string; generation_id: string }) =>
  apiPost<{ id: string }>(`/lessons/${lessonId}/quizzes`, body)

export const createQuestion = (quizId: string, body: QuestionWrite) =>
  apiPost(`/quizzes/${quizId}/questions`, body)
