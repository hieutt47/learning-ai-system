import { createBrowserRouter, type Params, type RouteObject } from 'react-router'
import { setUnauthorizedHandler } from '../api/client'
import { AppLayout } from '../components/AppLayout'
import { AuthLayout } from '../components/AuthLayout'
import { NotFoundState } from '../components/PageState'
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
          { path: paths.login, ...todo('login') },
          { path: paths.register, ...todo('register') },
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
      { path: paths.explore, ...todo('explore', { search: true }) },
      { path: '/courses/:courseId', ...todo('courseDetail', { search: true }) },

      // Đã đăng nhập
      { element: <RequireAccess allow="auth" />, children: [{ path: paths.profile, ...todo('profile') }] },

      // Sinh viên
      {
        element: <RequireAccess allow={['STUDENT']} />,
        children: [
          { path: paths.myCourses, ...todo('myCourses', { search: true }) },
          {
            // Học bài: người 3 dựng phần chung (video, danh sách bài) làm element của route cha, có <Outlet /> cho tab
            path: '/learn/:courseId/lessons/:lessonId',
            handle: { title: 'Học bài', tabs: learnTabs } satisfies RouteHandle,
            children: [
              { index: true, ...todo('learnDocs') },
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
          { path: paths.teachingCourses, ...todo('teachingCourses') },
          {
            // Khóa học: người 2 đặt tên khóa học + breadcrumb (usePageHeader) và nút thao tác
            path: '/teaching/courses/:courseId',
            handle: { title: 'Khóa học', tabs: courseTabs } satisfies RouteHandle,
            children: [
              { index: true, ...todo('courseInfo') },
              { path: 'lessons', ...todo('courseLessons') },
              { path: 'students', ...todo('courseStudents') },
            ],
          },
          {
            // Soạn bài học: người 3 đặt tên bài + breadcrumb (usePageHeader)
            path: '/teaching/lessons/:lessonId',
            handle: { title: 'Soạn bài học', tabs: lessonTabs } satisfies RouteHandle,
            children: [
              { index: true, ...todo('lessonContent') },
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
          { path: paths.adminUsers, ...todo('adminUsers') },
          { path: paths.adminTeacherRequests, ...todo('adminTeacherRequests') },
          { path: paths.adminCategories, ...todo('adminCategories') },
          { path: paths.adminCourses, ...todo('adminCourses') },
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
