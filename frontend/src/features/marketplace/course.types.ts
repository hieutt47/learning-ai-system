import type { Id, IsoDateTime } from '../../shared/api/types'

export type CourseStatus = 'Draft' | 'Published' | 'Archived'

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
  lessonCount: number
  outline: OutlineItem[]
}

export interface OutlineItem {
  id: Id
  title: string
  orderIndex: number
}

export interface OwnedCourse {
  id: Id
  title: string
  description: string
  categoryId: Id
  categoryName: string
  teacherId: Id
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

export interface EnrolledCourse extends PublicCourse {
  enrolledAt: IsoDateTime
  archivedAt?: IsoDateTime
}

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

export interface CourseFormValues {
  title: string
  description: string
  categoryId: Id
  price: string
}
