import type { Id, IsoDateTime } from '../../../shared/api/types'
import type { LessonStatus } from '../lessons/lesson.types'

export type QuizStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED'
export type CourseQuizFilter = 'active_published' | 'all'

export interface CourseQuizStatistics {
  quizId: Id
  quizTitle: string
  lessonId: Id
  lessonTitle: string
  lessonOrderIndex: number
  lessonStatus: LessonStatus
  status: QuizStatus
  questionCount: number
  submittedAttempts: number
  uniqueStudents: number
  averageScore: number | null
  latestSubmittedAt: IsoDateTime | null
}

export interface CourseStatistics {
  courseId: Id
  courseTitle: string
  enrolledStudents: number
  lessonCount: number
  publishedLessonCount: number
  quizCount: number
  submittedAttempts: number
  completedStudents: number
  completionRate: number
  averageScore: number | null
  quizzes: CourseQuizStatistics[]
  generatedAt: IsoDateTime
}

export interface StudentBestScore {
  studentId: Id
  studentName: string
  score: number
  attemptCount: number
  latestSubmittedAt: IsoDateTime | null
}

export interface HardestQuestion {
  questionId: Id
  orderIndex: number
  questionContent: string
  wrongCount: number
  wrongRate: number
}

export interface QuizStatistics {
  quizId: Id
  quizTitle: string
  quizStatus: QuizStatus
  courseId: Id
  courseTitle: string
  lessonId: Id
  lessonTitle: string
  lessonStatus: LessonStatus
  questionCount: number
  submittedAttempts: number
  uniqueStudents: number
  averageScore: number | null
  highestScore: number | null
  lowestScore: number | null
  bestScores: StudentBestScore[]
  hardestQuestions: HardestQuestion[]
  generatedAt: IsoDateTime
}

export interface SystemStatistics {
  activeUsers: number
  teachers: number
  publishedCourses: number
  enrollments: number
  submittedAttempts: number
  generatedAt: IsoDateTime
}
