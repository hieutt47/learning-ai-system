import { apiGet } from '../../shared/api/client'
import type { Page } from '../../shared/api/types'
import type { Flashcard, QuizQuestion } from '../teaching/ai/api'

export type AiGenerationType = 'QUIZ' | 'FLASHCARD'
export type AiGenerationStatus = 'SUCCEEDED' | 'FAILED'
export type AiSourceType = 'TRANSCRIPT' | 'DOCUMENT'
export type AiDifficulty = 'EASY' | 'MEDIUM' | 'HARD'

export interface AdminAiGeneration {
  id: string
  lesson_id: string
  lesson_title: string
  course_id: string
  course_title: string
  source_document_id: string | null
  source_document_name: string | null
  source_type: AiSourceType
  generation_type: AiGenerationType
  requested_count: number
  difficulty: AiDifficulty | null
  provider: string
  model: string
  prompt_version: string
  status: AiGenerationStatus
  candidate_json: string | null
  error_code: string | null
  created_by: string
  creator_name: string
  created_at: string
}

export interface ParsedCandidatePreview {
  questions?: QuizQuestion[]
  flashcards?: Pick<Flashcard, 'front_text' | 'back_text'>[]
}

export interface ListLessonGenerationsParams {
  generation_type?: AiGenerationType
  status?: AiGenerationStatus
  page?: number
  page_size?: number
}

export const adminAiKeys = {
  lessonGenerations: (lessonId: string, params: ListLessonGenerationsParams) =>
    ['admin', 'lesson-ai-generations', lessonId, params] as const,
}

export const listLessonAiGenerations = (lessonId: string, params: ListLessonGenerationsParams) =>
  apiGet<Page<AdminAiGeneration>>(`/lessons/${lessonId}/ai-generations`, { query: { ...params } })

export function parseCandidatePreview(raw: string | null): ParsedCandidatePreview | null {
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw) as ParsedCandidatePreview
    if (Array.isArray(parsed?.questions) || Array.isArray(parsed?.flashcards)) {
      return parsed
    }
    return null
  } catch {
    return null
  }
}
