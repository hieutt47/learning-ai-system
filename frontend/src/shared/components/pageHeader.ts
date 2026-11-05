import { createContext, useContext, useEffect } from 'react'

export interface Crumb {
  label: string
  /** Bỏ trống ở mục cuối (trang hiện tại). */
  to?: string
}

export interface PageHeader {
  title?: string
  crumbs?: Crumb[]
}

export const PageHeaderContext = createContext<(header: PageHeader | null) => void>(() => {})

/**
 * Đặt tiêu đề + breadcrumb của header từ trong trang (giống shell({title, crumbs}) của prototype),
 * dùng khi tiêu đề phụ thuộc dữ liệu, vd tên khóa học:
 *   usePageHeader({ title: course.title, crumbs: [{ label: 'Khóa học giảng dạy', to: paths.teachingCourses }, { label: course.title }] })
 * Không gọi thì header dùng `handle.title` của route.
 */
export function usePageHeader(header: PageHeader | null): void {
  const setHeader = useContext(PageHeaderContext)
  // So sánh header theo nội dung để không đặt lại mỗi lần render.
  const key = JSON.stringify(header)
  useEffect(() => {
    setHeader(JSON.parse(key) as PageHeader | null)
    return () => setHeader(null)
  }, [setHeader, key])
}
