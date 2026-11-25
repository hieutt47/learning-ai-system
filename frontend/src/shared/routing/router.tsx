import { CourseStatisticsPage, QuizStatisticsPage, SystemStatisticsPage } from '../../features/teaching/statistics/StatisticsPages'
import { LessonEditorPage } from '../../features/teaching/lessons/LessonEditorPage'
import { LearningDocumentsPage } from '../../features/learning/LearningDocumentsPage'
import { LearningLessonLayout } from '../../features/learning/LearningLessonLayout'
import { CourseLessonsPage } from '../../features/teaching/lessons/CourseLessonsPage'
import { AdminCoursesPage } from '../../features/marketplace/AdminCoursesPage'
import { AdminCourseViewPage } from '../../features/admin/AdminCourseViewPage'
import { AdminLessonViewPage } from '../../features/admin/AdminLessonViewPage'
import { AdminLessonAiPage, AdminLessonQuizPage } from '../../features/admin/AdminLessonSectionPages'
import { CourseDetailPage } from '../../features/marketplace/CourseDetailPage'
import { CourseInfoPage } from '../../features/marketplace/CourseInfoPage'
import { CourseStudentsPage } from '../../features/marketplace/CourseStudentsPage'
import { ExplorePage } from '../../features/marketplace/ExplorePage'
import { MyCoursesPage } from '../../features/marketplace/MyCoursesPage'
import { TeachingCoursesPage } from '../../features/marketplace/TeachingCoursesPage'
import { lessonTabs } from './lessonTabs'
import { NewLessonRedirect } from './NewLessonRedirect'
import { createBrowserRouter, type Params } from 'react-router'
import { LoginPage } from '../../features/auth/LoginPage'
import { ForgotPasswordPage } from '../../features/auth/ForgotPasswordPage'
import { ResetPasswordPage } from '../../features/auth/ResetPasswordPage'
import { VerifyEmailPage } from '../../features/auth/VerifyEmailPage'
import { RegisterPage } from '../../features/auth/RegisterPage'
import { setUnauthorizedHandler } from '../api/client'
import { AppLayout } from '../components/AppLayout'
import { AuthLayout } from '../components/AuthLayout'
import { LoadingState, NotFoundState } from '../components/PageState'
import { RouteErrorPage } from '../components/RouteErrorPage'
import { GuestOnly, HomeRedirect, RequireAccess } from './guards'
import { paths } from './paths'
import type { RouteHandle, TabItem } from './routeHandle'
import { LessonAiPage } from '../../features/teaching/ai/LessonAiPage'
import { AiPreviewPage } from '../../features/teaching/ai/AiPreviewPage'
import { LearnFlashcardsPage } from '../../features/learning/flashcards/LearnFlashcardsPage'
import { LessonQuizzesPage } from '../../features/teaching/quizzes/LessonQuizzesPage'
import { QuizEditorPage } from '../../features/teaching/quizzes/QuizEditorPage'
import { LearningQuizPage } from '../../features/learning/LearningQuizPage'
import { AttemptsHistoryPage } from '../../features/learning/attempts/AttemptsHistoryPage'
import { TakeQuizPage } from '../../features/learning/quizzes/TakeQuizPage'
import { AttemptResultPage } from '../../features/learning/quizzes/AttemptResultPage'

/**
 * Bảng route của cả app, theo các màn (VIEWS), tab (tabsBar) và quyền (ACCESS) trong
 * docs/03-experience/prototype.html. Danh sách màn + người phụ trách: ./screens.ts;
 * việc cần làm từng màn: frontend/README.md.
 *
 * Làm màn hình nào: tạo trang trong src/features/<khu vực>/, rồi thay `...todo('key')` của route đó bằng
 *   element: <TrangMoi />, handle: { title: '...' }
 * Trang có tab: mỗi tab là một route con, mỗi người chỉ sửa route tab của mình.
 */

const id = (params: Params, key: string) => params[key] ?? ''

/** Tab trang khóa học (prototype courseHead): Thông tin · Bài học · Học viên. */
const courseTabs = (p: Params): TabItem[] => [
  { to: paths.teachingCourse(id(p, 'courseId')), label: 'Thông tin' },
  { to: paths.teachingCourseLessons(id(p, 'courseId')), label: 'Bài học' },
  { to: paths.teachingCourseStudents(id(p, 'courseId')), label: 'Học viên' },
]

/** Tab trang học bài (prototype learn): Tài liệu · Quiz · Flashcard. */
const learnTabs = (p: Params): TabItem[] => [
  { to: paths.learnLesson(id(p, 'courseId'), id(p, 'lessonId')), label: 'Tài liệu' },
  { to: paths.learnLessonQuiz(id(p, 'courseId'), id(p, 'lessonId')), label: 'Quiz' },
  { to: paths.learnLessonFlashcards(id(p, 'courseId'), id(p, 'lessonId')), label: 'Flashcard' },
]

/** Tab trang xem bài học của ADMIN: Nội dung · Quiz · AI & Flashcard. */
const adminLessonTabs = (p: Params): TabItem[] => [
  { to: paths.adminCourseLesson(id(p, 'courseId'), id(p, 'lessonId')), label: 'Nội dung' },
  { to: paths.adminCourseLessonQuizzes(id(p, 'courseId'), id(p, 'lessonId')), label: 'Quiz' },
  { to: paths.adminCourseLessonAi(id(p, 'courseId'), id(p, 'lessonId')), label: 'AI & Flashcard' },
]

export const router = createBrowserRouter([
  {
    element: <AuthLayout />,
    children: [
      { path: paths.verifyEmail, element: <VerifyEmailPage />, handle: { title: 'Xác minh email' } satisfies RouteHandle },
      { path: paths.forgotPassword, element: <ForgotPasswordPage />, handle: { title: 'Quên mật khẩu' } satisfies RouteHandle },
      { path: paths.resetPassword, element: <ResetPasswordPage />, handle: { title: 'Đặt lại mật khẩu' } satisfies RouteHandle },
    ],
  },
  {
    element: <GuestOnly />,
    errorElement: <RouteErrorPage />,
    children: [
      {
        element: <AuthLayout />,
        children: [
          { path: paths.login, element: <LoginPage />, handle: { title: 'Đăng nhập' } satisfies RouteHandle },
          { path: paths.register, element: <RegisterPage />, handle: { title: 'Đăng ký' } satisfies RouteHandle },
        ],
      },
    ],
  },
  {
    element: <AppLayout />,
    errorElement: <RouteErrorPage />,
    children: [
      { path: paths.home, element: <HomeRedirect /> },
      // Ai cũng xem được
      { path: paths.explore, element: <ExplorePage />, handle: { title: 'Khám phá khóa học', search: true } satisfies RouteHandle },
      {
        path: '/courses/:courseId',
        element: <CourseDetailPage />,
        handle: { title: 'Chi tiết khóa học', search: true } satisfies RouteHandle,
      },
      // Đã đăng nhập
      {
        element: <RequireAccess allow="auth" />,
        children: [{
          path: paths.profile,
          hydrateFallbackElement: <LoadingState />,
          lazy: async () => {
            const { ProfilePage } = await import('../../features/auth/ProfilePage')
            return { Component: ProfilePage }
          },
          handle: { title: 'Hồ sơ' } satisfies RouteHandle,
        }],
      },

      // Sinh viên
      {
        element: <RequireAccess allow={['STUDENT']} />,
        children: [
          { path: paths.myCourses, element: <MyCoursesPage />, handle: { title: 'Khóa học của tôi', search: true } satisfies RouteHandle },
          {
            // Học bài: người 3 dựng phần chung (video, danh sách bài) làm element của route cha, có <Outlet /> cho tab
            path: '/learn/:courseId/lessons/:lessonId',
            element: <LearningLessonLayout />,
            handle: { title: 'Học bài', tabs: learnTabs } satisfies RouteHandle,
            children: [
              { index: true, element: <LearningDocumentsPage /> },
              {
                path: 'quiz',
                element: <LearningQuizPage />,
                handle: { title: 'Học bài · Quiz' } satisfies RouteHandle,
              },
              {
                path: 'flashcards',
                element: <LearnFlashcardsPage />,
                handle: { title: 'Học bài · Flashcard' } satisfies RouteHandle,
              },
            ],
          },
          {
            path: '/quizzes/:quizId',
            element: <TakeQuizPage />,
            handle: { title: 'Làm bài quiz' } satisfies RouteHandle,
          },
          {
            path: '/results/:attemptId',
            element: <AttemptResultPage />,
            handle: { title: 'Kết quả' } satisfies RouteHandle,
          },
          {
            path: paths.attempts,
            element: <AttemptsHistoryPage />,
            handle: { title: 'Lịch sử làm bài' } satisfies RouteHandle,
          },
        ],
      },
      // Giảng viên
      {
        element: <RequireAccess allow={['TEACHER']} />,
        children: [
          { path: paths.teachingCourses, element: <TeachingCoursesPage />, handle: { title: 'Khóa học giảng dạy' } satisfies RouteHandle },
          {
            // Khóa học: người 2 đặt tên khóa học + breadcrumb (usePageHeader) và nút thao tác
            path: '/teaching/courses/:courseId',
            handle: { title: 'Khóa học', tabs: courseTabs } satisfies RouteHandle,
            children: [
              { index: true, element: <CourseInfoPage /> },
              {
                path: 'lessons',
                element: <CourseLessonsPage />,
                handle: { title: 'Khóa học · Bài học' } satisfies RouteHandle,
              },
              { path: 'students', element: <CourseStudentsPage /> },
            ],
          },
          { path: '/teaching/lessons/new/quizzes', element: <NewLessonRedirect /> },
          { path: '/teaching/lessons/new/ai/*', element: <NewLessonRedirect /> },
          {
            // Soạn bài học: người 3 đặt tên bài + breadcrumb (usePageHeader)
            path: '/teaching/lessons/:lessonId',
            handle: { title: 'Soạn bài học', tabs: lessonTabs } satisfies RouteHandle,
            children: [
              { index: true, element: <LessonEditorPage /> },
              {
                path: 'quizzes',
                element: <LessonQuizzesPage />,
                handle: { title: 'Soạn bài học · Quiz' } satisfies RouteHandle,
              },
              {
                path: 'ai',
                element: <LessonAiPage />,
                handle: { title: 'Soạn bài học · AI & Flashcard' } satisfies RouteHandle,
              },
              {
                path: 'ai/preview',
                element: <AiPreviewPage />,
                handle: { title: 'Xem trước kết quả AI' } satisfies RouteHandle,
              },
            ],
          },
          {
            path: '/teaching/quizzes/:quizId/edit',
            element: <QuizEditorPage />,
            handle: { title: 'Soạn quiz' } satisfies RouteHandle,
          },
          { path: paths.teachingStatistics, element: <CourseStatisticsPage /> },
          { path: '/teaching/statistics/quizzes/:quizId', element: <QuizStatisticsPage /> },
        ],
      },
      // Quản trị
      {
        element: <RequireAccess allow={['ADMIN']} />,
        children: [
          {
            path: paths.adminUsers,
            hydrateFallbackElement: <LoadingState />,
            lazy: async () => {
              const { UsersPage } = await import('../../features/admin/UsersPage')
              return { Component: UsersPage }
            },
            handle: { title: 'Người dùng' } satisfies RouteHandle,
          },
          {
            path: paths.adminTeacherRequests,
            hydrateFallbackElement: <LoadingState />,
            lazy: async () => {
              const { TeacherRequestsPage } = await import('../../features/admin/TeacherRequestsPage')
              return { Component: TeacherRequestsPage }
            },
            handle: { title: 'Duyệt giảng viên' } satisfies RouteHandle,
          },
          {
            path: paths.adminCategories,
            hydrateFallbackElement: <LoadingState />,
            lazy: async () => {
              const { CategoriesPage } = await import('../../features/admin/CategoriesPage')
              return { Component: CategoriesPage }
            },
            handle: { title: 'Danh mục' } satisfies RouteHandle,
          },
          { path: paths.adminCourses, element: <AdminCoursesPage />, handle: { title: 'Khóa học' } satisfies RouteHandle },
          {
            path: '/admin/courses/:courseId',
            element: <AdminCourseViewPage />,
            handle: { title: 'Xem khóa học' } satisfies RouteHandle,
          },
          {
            path: '/admin/courses/:courseId/lessons/:lessonId',
            handle: { title: 'Xem bài học', tabs: adminLessonTabs } satisfies RouteHandle,
            children: [
              { index: true, element: <AdminLessonViewPage /> },
              {
                path: 'quizzes',
                element: <AdminLessonQuizPage />,
                handle: { title: 'Xem bài học · Quiz' } satisfies RouteHandle,
              },
              {
                path: 'ai',
                element: <AdminLessonAiPage />,
                handle: { title: 'Xem bài học · AI & Flashcard' } satisfies RouteHandle,
              },
            ],
          },
          { path: paths.adminStatistics, element: <SystemStatisticsPage /> },
        ],
      },
      { path: '*', element: <NotFoundState />, handle: { title: 'Không tìm thấy trang' } satisfies RouteHandle },
    ],
  },
])

setUnauthorizedHandler(() => {
  const { pathname, search } = window.location
  if (pathname === paths.login || pathname === paths.register) {
    return
  }
  void router.navigate(`${paths.login}?redirect=${encodeURIComponent(pathname + search)}`, { replace: true })
})
