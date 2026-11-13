import type { Id, IsoDateTime } from '../../../shared/api/types'

export type QuizStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED'

export interface QuizSummaryItem {
  id: Id
  lessonId: Id
  title: string
  status: QuizStatus
  questionCount: number
}

export interface QuizOptionAuthor {
  id?: Id
  content: string
  isCorrect: boolean
  orderIndex: number
}

export interface QuizQuestionAuthor {
  id?: Id
  content: string
  explanation?: string
  orderIndex: number
  generatedByAi?: boolean
  options: QuizOptionAuthor[]
}

export interface QuizAuthorDetail {
  id: Id
  lessonId: Id
  title: string
  status: QuizStatus
  createdAt: IsoDateTime
  questions: QuizQuestionAuthor[]
}

export interface QuizCreateRequest {
  title: string
  generation_id?: string
}

export interface QuizUpdateRequest {
  title: string
}

export interface QuestionWriteRequest {
  content: string
  explanation?: string
  order_index: number
  options: {
    content: string
    is_correct: boolean
    order_index: number
  }[]
  generation_id?: string
}
