import type { Id, IsoDateTime } from '../../../shared/api/types'

export type AttemptStatus = 'IN_PROGRESS' | 'SUBMITTED'

export interface AttemptSummaryItem {
  id: Id
  quizId: Id
  quizTitle?: string
  courseId?: Id
  courseTitle?: string
  status: AttemptStatus
  score?: number | null
  startedAt: IsoDateTime
  submittedAt?: IsoDateTime | null
}

export interface AttemptAnswerReview {
  questionId: Id
  questionContent: string
  selectedOptionId?: Id | null
  isCorrect: boolean
  options: {
    id: Id
    content: string
    isCorrect?: boolean
  }[]
  explanation?: string
}

export interface AttemptDetail {
  id: Id
  quizId: Id
  quizTitle: string
  courseId?: Id
  lessonId?: Id
  studentId?: Id
  status: AttemptStatus
  score?: number | null
  totalQuestions: number
  correctCount: number
  startedAt: IsoDateTime
  submittedAt?: IsoDateTime | null
  reviews: AttemptAnswerReview[]
}
