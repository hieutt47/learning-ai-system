import type { Id, IsoDateTime } from '../../shared/api/types'

/** Trạng thái khóa học hiển thị trên UI, suy ra từ is_published + archived_at. */
export type CourseStatus = 'Draft' | 'Published' | 'Archived'

/** Một khóa học công khai (COURSE-02). Không có field nội bộ nào của người dạy. */
export interface PublicCourse {
  id: Id
  title: string
  description: string
  category: { id: Id; name: string }
  teacher: { id: Id; fullName: string }
  thumbnailUrl?: string
  price: string
  currency: string
  totalStudents: number
  /** Số bài học. Luôn có; `outline` chỉ có ở trang chi tiết. */
  lessonCount: number
  outline: OutlineItem[]
}

/** Một bài trong đề cương công khai: không có video hay transcript. */
export interface OutlineItem {
  id: Id
  title: string
  orderIndex: number
}

/** Khóa học nhìn từ phía chủ sở hữu (COURSE-01). */
export interface OwnedCourse {
  id: Id
  title: string
  description: string
  categoryId: Id
  categoryName: string
  teacherId: Id
  /** Tên giảng viên, để bảng quản trị không phải hiện id trần. */
  teacherName: string
  thumbnailUrl?: string
  price: string
  status: CourseStatus
  lessonCount: number
  totalStudents: number
  archivedAt?: IsoDateTime
  createdAt: IsoDateTime
  updatedAt: IsoDateTime
}

/** Một khóa trong "Khóa học của tôi": CoursePublic + enrolled_at. */
export interface EnrolledCourse extends PublicCourse {
  enrolledAt: IsoDateTime
  /** Có khi giảng viên lưu trữ khóa sau khi học viên đã ghi danh. */
  archivedAt?: IsoDateTime
}

/** Một học viên của khóa, nhìn từ phía chủ sở hữu. Không có email. */
export interface CourseMember {
  courseId: Id
  studentId: Id
  fullName: string
  enrolledAt: IsoDateTime
}

export interface Category {
  id: Id
  name: string
}

/** Giá trị form thông tin khóa học. */
export interface CourseFormValues {
  title: string
  description: string
  categoryId: Id
  price: string
}
