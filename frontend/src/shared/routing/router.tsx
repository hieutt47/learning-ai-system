import { LessonEditorPage } from '../../features/teaching/lessons/LessonEditorPage'
import { LearningDocumentsPage } from '../../features/learning/LearningDocumentsPage'
import { LearningLessonLayout } from '../../features/learning/LearningLessonLayout'
import { CourseLessonsPage } from '../../features/teaching/lessons/CourseLessonsPage'
import { AdminCoursesPage } from '../../features/marketplace/AdminCoursesPage'
import { CourseDetailPage } from '../../features/marketplace/CourseDetailPage'
import { CourseInfoPage } from '../../features/marketplace/CourseInfoPage'
import { CourseStudentsPage } from '../../features/marketplace/CourseStudentsPage'
import { ExplorePage } from '../../features/marketplace/ExplorePage'
import { MyCoursesPage } from '../../features/marketplace/MyCoursesPage'
import { TeachingCoursesPage } from '../../features/marketplace/TeachingCoursesPage'
import { createBrowserRouter, type Params, type RouteObject } from 'react-router'
import { LoginPage } from '../../features/auth/LoginPage'
import { RegisterPage } from '../../features/auth/RegisterPage'
import { setUnauthorizedHandler } from '../api/client'
import { AppLayout } from '../components/AppLayout'
import { AuthLayout } from '../components/AuthLayout'
import { LoadingState, NotFoundState } from '../components/PageState'
import { PlaceholderPage } from '../components/PlaceholderPage'
import { RouteErrorPage } from '../components/RouteErrorPage'
import { DevScreensPage } from '../dev/DevScreensPage'
import { GuestOnly, HomeRedirect, RequireAccess } from './guards'
import { paths } from './paths'
import type { RouteHandle, TabItem } from './routeHandle'
import { SCREENS, type ScreenKey } from './screens'

/**
 * Bảng route của cả app, theo các màn (VIEWS), tab (tabsBar) và quyền (ACCESS) trong
 * docs/03-experience/prototype.html. Danh sách màn + người phụ trách: ./screens.ts;
 * việc cần làm từng màn: frontend/README.md.
 *
 * Làm màn hình nào: tạo trang trong src/features/<khu vực>/, rồi thay `...todo('key')` của route đó bằng
 *   element: <TrangMoi />, handle: { title: '...' }
 * Trang có tab: mỗi tab là một route con, mỗi người chỉ sửa route tab của mình.
 */

/** Trang giữ chỗ của một màn trong SCREENS. */
function todo(key: ScreenKey, extra: Omit<RouteHandle, 'title'> = {}): Pick<RouteObject, 'element' | 'handle'> {
  const screen = SCREENS[key]
  return { element: <PlaceholderPage screen={screen} />, handle: { title: screen.title, ...extra } satisfies RouteHandle }
}

const id = (params: Params, key: string) => params[key] ?? ''

/** Tab trang khóa học (prototype courseHead): Thông tin · Bài học · Học viên. */
const courseTabs = (p: Params): TabItem[] => [
  { to: paths.teachingCourse(id(p, 'courseId')), label: 'Thông tin' },
  { to: paths.teachingCourseLessons(id(p, 'courseId')), label: 'Bài học' },
  { to: paths.teachingCourseStudents(id(p, 'courseId')), label: 'Học viên' },
]

/** Tab trang soạn bài học (prototype lessonHead): Nội dung · Quiz · AI & Flashcard. */
const lessonTabs = (p: Params): TabItem[] => [
  { to: paths.teachingLesson(id(p, 'lessonId')), label: 'Nội dung' },
  { to: paths.teachingLessonQuizzes(id(p, 'lessonId')), label: 'Quiz' },
  { to: paths.teachingLessonAi(id(p, 'lessonId')), label: 'AI & Flashcard' },
]

/** Tab trang học bài (prototype learn): Tài liệu · Quiz · Flashcard. */
const learnTabs = (p: Params): TabItem[] => [
  { to: paths.learnLesson(id(p, 'courseId'), id(p, 'lessonId')), label: 'Tài liệu' },
  { to: paths.learnLessonQuiz(id(p, 'courseId'), id(p, 'lessonId')), label: 'Quiz' },
  { to: paths.learnLessonFlashcards(id(p, 'courseId'), id(p, 'lessonId')), label: 'Flashcard' },
]

/** Trang chỉ có khi `npm run dev`: danh sách màn + đổi vai trò. */
const devRoutes: RouteObject[] = import.meta.env.DEV
  ? [{ path: '/dev/screens', element: <DevScreensPage />, handle: { title: 'Danh sách màn hình (dev)' } satisfies RouteHandle }]
  : []

export const router = createBrowserRouter([
  // Đăng nhập / đăng ký: không có sidebar, chỉ cho khách
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
      ...devRoutes,

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
              { path: 'quiz', ...todo('learnQuiz') },
              { path: 'flashcards', ...todo('learnFlashcards') },
            ],
          },
          { path: '/quizzes/:quizId', ...todo('takeQuiz') },
          { path: '/results/:attemptId', ...todo('attemptResult') },
          { path: paths.attempts, ...todo('attempts') },
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
          {
            // Soạn bài học: người 3 đặt tên bài + breadcrumb (usePageHeader)
            path: '/teaching/lessons/:lessonId',
            handle: { title: 'Soạn bài học', tabs: lessonTabs } satisfies RouteHandle,
            children: [
              { index: true, element: <LessonEditorPage /> },
              { path: 'quizzes', ...todo('lessonQuizzes') },
              { path: 'ai', ...todo('lessonAi') },
              { path: 'ai/preview', ...todo('aiPreview') },
            ],
          },
          { path: '/teaching/quizzes/:quizId/edit', ...todo('quizEditor') },
          { path: paths.teachingStatistics, ...todo('courseStatistics') },
          { path: '/teaching/statistics/quizzes/:quizId', ...todo('quizStatistics') },
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
          { path: paths.adminStatistics, ...todo('adminStatistics') },
        ],
      },

      { path: '*', element: <NotFoundState />, handle: { title: 'Không tìm thấy trang' } satisfies RouteHandle },
    ],
  },
])

// API trả 401 → về trang đăng nhập, giữ trang đích để quay lại sau khi đăng nhập (design-brief mục 8).
setUnauthorizedHandler(() => {
  const { pathname, search } = window.location
  if (pathname !== paths.login) {
    void router.navigate(`${paths.login}?redirect=${encodeURIComponent(pathname + search)}`)
  }
})
