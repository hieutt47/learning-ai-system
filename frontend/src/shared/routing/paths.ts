import type { Id } from '../api/types'
import type { ViewerRole } from '../auth/currentUser'

/**
 * Đường dẫn các trang. Dùng các hàm này khi điều hướng, không tự ghép chuỗi URL trong feature,
 * để đổi route chỉ phải sửa một chỗ.
 */
export const paths = {
  home: '/',
  login: '/login',
  register: '/register',
  profile: '/profile',

  // Khách & sinh viên
  explore: '/explore',
  courseDetail: (courseId: Id) => `/courses/${courseId}`,
  myCourses: '/my-courses',
  /** Học bài, tab Tài liệu (prototype: learn-<course>-<lesson>-docs). */
  learnLesson: (courseId: Id, lessonId: Id) => `/learn/${courseId}/lessons/${lessonId}`,
  learnLessonQuiz: (courseId: Id, lessonId: Id) => `/learn/${courseId}/lessons/${lessonId}/quiz`,
  learnLessonFlashcards: (courseId: Id, lessonId: Id) => `/learn/${courseId}/lessons/${lessonId}/flashcards`,
  /** Làm bài quiz (prototype: quiz-<quiz>). */
  takeQuiz: (quizId: Id) => `/quizzes/${quizId}`,
  /** Kết quả một lượt làm (prototype: result-<attempt>). */
  attemptResult: (attemptId: Id) => `/results/${attemptId}`,
  attempts: '/attempts',

  // Giảng viên
  teachingCourses: '/teaching/courses',
  /** Khóa học, tab Thông tin / Bài học / Học viên (prototype: tcourse, tlessons, tstudents). */
  teachingCourse: (courseId: Id) => `/teaching/courses/${courseId}`,
  teachingCourseLessons: (courseId: Id) => `/teaching/courses/${courseId}/lessons`,
  teachingCourseStudents: (courseId: Id) => `/teaching/courses/${courseId}/students`,
  teachingLessonNew: (courseId: Id) => `/teaching/lessons/new?courseId=${courseId}`,
  /** Bài học, tab Nội dung / Quiz / AI & Flashcard (prototype: tlesson, tquizzes, tai). */
  teachingLesson: (lessonId: Id) => `/teaching/lessons/${lessonId}`,
  teachingLessonQuizzes: (lessonId: Id) => `/teaching/lessons/${lessonId}/quizzes`,
  teachingLessonAi: (lessonId: Id) => `/teaching/lessons/${lessonId}/ai`,
  /** Xem trước kết quả AI (prototype: taipreview). */
  teachingLessonAiPreview: (lessonId: Id) => `/teaching/lessons/${lessonId}/ai/preview`,
  quizEditor: (quizId: Id) => `/teaching/quizzes/${quizId}/edit`,
  teachingStatistics: '/teaching/statistics',
  quizStatistics: (quizId: Id) => `/teaching/statistics/quizzes/${quizId}`,

  // Quản trị
  adminUsers: '/admin/users',
  adminTeacherRequests: '/admin/teacher-requests',
  adminCategories: '/admin/categories',
  adminCourses: '/admin/courses',
  adminStatistics: '/admin/statistics',
} as const

/** Trang chủ theo vai trò, giống HOME() của prototype. */
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
