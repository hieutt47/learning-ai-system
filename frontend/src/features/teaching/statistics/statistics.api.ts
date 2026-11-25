import { apiGet } from '../../../shared/api/client'
import type { Id, IsoDateTime } from '../../../shared/api/types'
import type { LessonStatus } from '../lessons/lesson.types'
import type {
  CourseQuizStatistics,
  CourseStatistics,
  HardestQuestion,
  QuizStatistics,
  QuizStatus,
  StudentBestScore,
  SystemStatistics,
} from './statistics.types'

interface CourseQuizWire {
  quiz_id: Id | number
  quiz_title: string
  lesson_id: Id | number
  lesson_title: string
  lesson_order_index: number
  lesson_status: LessonStatus
  status: QuizStatus
  question_count: number
  submitted_attempts: number
  unique_students: number
  average_score: number | null
  latest_submitted_at: IsoDateTime | null
}

interface CourseStatisticsWire {
  course_id: Id | number
  course_title?: string
  enrolled_students: number
  lesson_count?: number
  published_lesson_count?: number
  quiz_count: number
  submitted_attempts: number
  completed_students: number
  completion_rate: number
  average_score?: number | null
  quizzes?: CourseQuizWire[]
  generated_at: IsoDateTime
}

interface StudentBestScoreWire {
  student_id: Id | number
  student_name?: string
  score: number
  attempt_count?: number
  latest_submitted_at?: IsoDateTime | null
}

interface HardestQuestionWire {
  question_id: Id | number
  order_index?: number
  question_content?: string
  wrong_count: number
  wrong_rate?: number
}

interface QuizStatisticsWire {
  quiz_id: Id | number
  quiz_title?: string
  quiz_status?: QuizStatus
  course_id?: Id | number
  course_title?: string
  lesson_id?: Id | number
  lesson_title?: string
  lesson_status?: LessonStatus
  question_count?: number
  submitted_attempts: number
  unique_students: number
  average_score: number | null
  highest_score?: number | null
  lowest_score?: number | null
  best_scores: StudentBestScoreWire[]
  hardest_questions: HardestQuestionWire[]
  generated_at: IsoDateTime
}

interface SystemStatisticsWire {
  active_users: number
  teachers: number
  published_courses: number
  enrollments: number
  submitted_attempts: number
  generated_at: IsoDateTime
}

const mapCourseQuiz = (item: CourseQuizWire): CourseQuizStatistics => ({
  quizId: String(item.quiz_id),
  quizTitle: item.quiz_title,
  lessonId: String(item.lesson_id),
  lessonTitle: item.lesson_title,
  lessonOrderIndex: item.lesson_order_index,
  lessonStatus: item.lesson_status,
  status: item.status,
  questionCount: item.question_count,
  submittedAttempts: item.submitted_attempts,
  uniqueStudents: item.unique_students,
  averageScore: item.average_score,
  latestSubmittedAt: item.latest_submitted_at ?? null,
})

const mapBestScore = (item: StudentBestScoreWire): StudentBestScore => ({
  studentId: String(item.student_id),
  studentName: item.student_name ?? `Học viên #${item.student_id}`,
  score: Number(item.score),
  attemptCount: item.attempt_count ?? 1,
  latestSubmittedAt: item.latest_submitted_at ?? null,
})

const mapHardestQuestion = (item: HardestQuestionWire, submittedAttempts: number): HardestQuestion => ({
  questionId: String(item.question_id),
  orderIndex: item.order_index ?? 0,
  questionContent: item.question_content ?? `Câu hỏi #${item.question_id}`,
  wrongCount: item.wrong_count,
  wrongRate:
    item.wrong_rate !== undefined
      ? Number(item.wrong_rate)
      : submittedAttempts > 0
        ? Math.round((item.wrong_count / submittedAttempts) * 10000) / 100
        : 0,
})

export const statisticsKeys = {
  courseOptions: ['statistics', 'courses', 'options'] as const,
  course: (courseId: Id) => ['statistics', 'course', courseId] as const,
  quiz: (quizId: Id) => ['statistics', 'quiz', quizId] as const,
  system: ['statistics', 'system'] as const,
}

export async function getCourseStatistics(courseId: Id): Promise<CourseStatistics> {
  const wire = await apiGet<CourseStatisticsWire>(`/statistics/courses/${courseId}`)
  return {
    courseId: String(wire.course_id),
    courseTitle: wire.course_title ?? '',
    enrolledStudents: wire.enrolled_students,
    lessonCount: wire.lesson_count ?? 0,
    publishedLessonCount: wire.published_lesson_count ?? 0,
    quizCount: wire.quiz_count,
    submittedAttempts: wire.submitted_attempts,
    completedStudents: wire.completed_students,
    completionRate: Number(wire.completion_rate),
    averageScore: wire.average_score ?? null,
    quizzes: (wire.quizzes ?? []).map(mapCourseQuiz),
    generatedAt: wire.generated_at,
  }
}

export async function getQuizStatistics(quizId: Id): Promise<QuizStatistics> {
  const wire = await apiGet<QuizStatisticsWire>(`/statistics/quizzes/${quizId}`)
  return {
    quizId: String(wire.quiz_id),
    quizTitle: wire.quiz_title ?? `Quiz #${wire.quiz_id}`,
    quizStatus: wire.quiz_status ?? 'PUBLISHED',
    courseId: wire.course_id !== undefined ? String(wire.course_id) : '',
    courseTitle: wire.course_title ?? '',
    lessonId: wire.lesson_id !== undefined ? String(wire.lesson_id) : '',
    lessonTitle: wire.lesson_title ?? '',
    lessonStatus: wire.lesson_status ?? 'PUBLISHED',
    questionCount: wire.question_count ?? 0,
    submittedAttempts: wire.submitted_attempts,
    uniqueStudents: wire.unique_students,
    averageScore: wire.average_score === null ? null : Number(wire.average_score),
    highestScore: wire.highest_score === undefined || wire.highest_score === null ? null : Number(wire.highest_score),
    lowestScore: wire.lowest_score === undefined || wire.lowest_score === null ? null : Number(wire.lowest_score),
    bestScores: (wire.best_scores ?? []).map(mapBestScore),
    hardestQuestions: (wire.hardest_questions ?? []).map((q) => mapHardestQuestion(q, wire.submitted_attempts)),
    generatedAt: wire.generated_at,
  }
}

export async function getSystemStatistics(): Promise<SystemStatistics> {
  const wire = await apiGet<SystemStatisticsWire>('/statistics/system')
  return {
    activeUsers: wire.active_users,
    teachers: wire.teachers,
    publishedCourses: wire.published_courses,
    enrollments: wire.enrollments,
    submittedAttempts: wire.submitted_attempts,
    generatedAt: wire.generated_at,
  }
}
