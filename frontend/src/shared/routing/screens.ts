import type { ViewerRole } from '../auth/currentUser'

/**
 * Danh sách TẤT CẢ màn hình theo docs/03-experience/prototype.html (VIEWS) và người phụ trách
 * (design-brief mục 6). Router dùng danh sách này cho trang giữ chỗ, trang /dev/screens dùng để
 * liệt kê link. Việc cụ thể của từng màn ghi trong frontend/README.md.
 */

export type Owner = 1 | 2 | 3 | 4 | 5

export type Access = 'public' | 'guest' | 'auth' | Exclude<ViewerRole, 'GUEST'>

export interface Screen {
  /** Tiêu đề header của trang giữ chỗ. */
  title: string
  owner: Owner
  /** Màn tương ứng trong prototype.html (mở prototype, gõ #<tên> trên thanh địa chỉ). */
  prototype: string
  access: Access
  /** Đường dẫn mẫu để mở thử (ID nào cũng được khi trang chưa gọi API). */
  sample: string
}

export const OWNER_LABEL: Record<Owner, string> = {
  1: 'người 1 — tài khoản & quản trị',
  2: 'người 2 — khóa học & ghi danh',
  3: 'người 3 — bài học & tài liệu',
  4: 'người 4 — quiz, kết quả & thống kê',
  5: 'người 5 — AI & flashcard',
}

export const SCREENS = {
  // Người 1
  login: { title: 'Đăng nhập', owner: 1, prototype: 'login', access: 'guest', sample: '/login' },
  register: { title: 'Đăng ký', owner: 1, prototype: 'register', access: 'guest', sample: '/register' },
  profile: { title: 'Hồ sơ', owner: 1, prototype: 'profile', access: 'auth', sample: '/profile' },
  adminUsers: { title: 'Người dùng', owner: 1, prototype: 'admusers', access: 'ADMIN', sample: '/admin/users' },
  adminTeacherRequests: { title: 'Duyệt giảng viên', owner: 1, prototype: 'admreq', access: 'ADMIN', sample: '/admin/teacher-requests' },
  adminCategories: { title: 'Danh mục', owner: 1, prototype: 'admcat', access: 'ADMIN', sample: '/admin/categories' },

  // Người 2
  explore: { title: 'Khám phá khóa học', owner: 2, prototype: 'explore', access: 'public', sample: '/explore' },
  courseDetail: { title: 'Chi tiết khóa học', owner: 2, prototype: 'course', access: 'public', sample: '/courses/801' },
  myCourses: { title: 'Khóa học của tôi', owner: 2, prototype: 'mycourses', access: 'STUDENT', sample: '/my-courses' },
  teachingCourses: { title: 'Khóa học giảng dạy', owner: 2, prototype: 'teach', access: 'TEACHER', sample: '/teaching/courses' },
  courseInfo: { title: 'Khóa học · Thông tin', owner: 2, prototype: 'tcourse', access: 'TEACHER', sample: '/teaching/courses/801' },
  courseStudents: { title: 'Khóa học · Học viên', owner: 2, prototype: 'tstudents', access: 'TEACHER', sample: '/teaching/courses/801/students' },
  adminCourses: { title: 'Khóa học (quản trị)', owner: 2, prototype: 'admcourses', access: 'ADMIN', sample: '/admin/courses' },

  // Người 3
  courseLessons: { title: 'Khóa học · Bài học', owner: 3, prototype: 'tlessons', access: 'TEACHER', sample: '/teaching/courses/801/lessons' },
  lessonContent: { title: 'Soạn bài học · Nội dung', owner: 3, prototype: 'tlesson', access: 'TEACHER', sample: '/teaching/lessons/901' },
  learnDocs: { title: 'Học bài · Tài liệu', owner: 3, prototype: 'learn (tab Tài liệu)', access: 'STUDENT', sample: '/learn/801/lessons/901' },

  // Người 4
  lessonQuizzes: { title: 'Soạn bài học · Quiz', owner: 4, prototype: 'tquizzes', access: 'TEACHER', sample: '/teaching/lessons/901/quizzes' },
  quizEditor: { title: 'Soạn quiz', owner: 4, prototype: 'tquiz', access: 'TEACHER', sample: '/teaching/quizzes/1/edit' },
  learnQuiz: { title: 'Học bài · Quiz', owner: 4, prototype: 'learn (tab Quiz)', access: 'STUDENT', sample: '/learn/801/lessons/901/quiz' },
  takeQuiz: { title: 'Làm bài quiz', owner: 4, prototype: 'quiz', access: 'STUDENT', sample: '/quizzes/1' },
  attemptResult: { title: 'Kết quả', owner: 4, prototype: 'result', access: 'STUDENT', sample: '/results/1' },
  attempts: { title: 'Lịch sử làm bài', owner: 4, prototype: 'history', access: 'STUDENT', sample: '/attempts' },
  courseStatistics: { title: 'Thống kê khóa học', owner: 4, prototype: 'stats', access: 'TEACHER', sample: '/teaching/statistics' },
  quizStatistics: { title: 'Thống kê quiz', owner: 4, prototype: 'statsquiz', access: 'TEACHER', sample: '/teaching/statistics/quizzes/1' },
  adminStatistics: { title: 'Thống kê hệ thống', owner: 4, prototype: 'admstats', access: 'ADMIN', sample: '/admin/statistics' },

  // Người 5
  lessonAi: { title: 'Soạn bài học · AI & Flashcard', owner: 5, prototype: 'tai', access: 'TEACHER', sample: '/teaching/lessons/901/ai' },
  aiPreview: { title: 'Xem trước kết quả AI', owner: 5, prototype: 'taipreview', access: 'TEACHER', sample: '/teaching/lessons/901/ai/preview' },
  learnFlashcards: { title: 'Học bài · Flashcard', owner: 5, prototype: 'learn (tab Flashcard)', access: 'STUDENT', sample: '/learn/801/lessons/901/flashcards' },
} satisfies Record<string, Screen>

export type ScreenKey = keyof typeof SCREENS
