import type { Id } from '../api/types'
import type { ViewerRole } from '../auth/currentUser'

export const paths = {
  home: '/',
  login: '/login',
  register: '/register',
  verifyEmail: '/verify-email',
  profile: '/profile',
  explore: '/explore',
  courseDetail: (courseId: Id) => `/courses/${courseId}`,
  myCourses: '/my-courses',
  learnLesson: (courseId: Id, lessonId: Id) => `/learn/${courseId}/lessons/${lessonId}`,
  learnLessonQuiz: (courseId: Id, lessonId: Id) => `/learn/${courseId}/lessons/${lessonId}/quiz`,
  learnLessonFlashcards: (courseId: Id, lessonId: Id) => `/learn/${courseId}/lessons/${lessonId}/flashcards`,
  takeQuiz: (quizId: Id) => `/quizzes/${quizId}`,
  attemptResult: (attemptId: Id) => `/results/${attemptId}`,
  attempts: '/attempts',
  teachingCourses: '/teaching/courses',
  teachingCourse: (courseId: Id) => `/teaching/courses/${courseId}`,
  teachingCourseLessons: (courseId: Id) => `/teaching/courses/${courseId}/lessons`,
  teachingCourseStudents: (courseId: Id) => `/teaching/courses/${courseId}/students`,
  teachingLessonNew: (courseId: Id) => `/teaching/lessons/new?courseId=${courseId}`,
  teachingLesson: (lessonId: Id) => `/teaching/lessons/${lessonId}`,
  teachingLessonQuizzes: (lessonId: Id) => `/teaching/lessons/${lessonId}/quizzes`,
  teachingLessonAi: (lessonId: Id) => `/teaching/lessons/${lessonId}/ai`,
  teachingLessonAiPreview: (lessonId: Id) => `/teaching/lessons/${lessonId}/ai/preview`,
  quizEditor: (quizId: Id) => `/teaching/quizzes/${quizId}/edit`,
  teachingStatistics: '/teaching/statistics',
  quizStatistics: (quizId: Id) => `/teaching/statistics/quizzes/${quizId}`,
  adminUsers: '/admin/users',
  adminTeacherRequests: '/admin/teacher-requests',
  adminCategories: '/admin/categories',
  adminCourses: '/admin/courses',
  adminStatistics: '/admin/statistics',
} as const
export function homePathFor(role: ViewerRole): string {
  switch (role) {
    case 'TEACHER':
      return paths.teachingCourses
    case 'ADMIN':
      return paths.adminUsers
    default:
      return paths.explore
  }
}
