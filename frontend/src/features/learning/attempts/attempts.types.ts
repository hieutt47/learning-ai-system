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
