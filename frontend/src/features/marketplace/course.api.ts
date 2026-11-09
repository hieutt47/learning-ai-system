import { apiDelete, apiGet, apiPatch, apiPost } from '../../shared/api/client'
import type { Id, Page, PageParams } from '../../shared/api/types'
import type {
  Category,
  CourseFormValues,
  CourseMember,
  CourseStatus,
  EnrolledCourse,
  OwnedCourse,
  PublicCourse,
} from './course.types'

interface CategoryWire {
  id: string | number
  name: string
  description?: string | null
}

interface OutlineWire {
  id: string | number
  title: string
  order_index: number
}

interface PublicCourseWire {
  id: string | number
  title: string
  description: string
  category: { id: string | number; name: string }
  teacher: { id: string | number; full_name: string }
  thumbnail_url?: string | null
  price: string | number
  currency: string
  total_students: number
  lesson_count?: number
  outline?: OutlineWire[] | null
  archived_at?: string | null
  enrolled_at?: string
}
interface OwnedCourseWire {
  id: string | number
  title: string
  description: string
  category_id: string | number
  category_name: string
  teacher_id: string | number
  teacher_name?: string
  thumbnail_url?: string | null
  price: string | number
  is_published: boolean
  lesson_count?: number
  total_students?: number
  archived_at?: string | null
  created_at: string
  updated_at: string
}
interface MemberWire {
  course_id: string | number
  student_id: string | number
  full_name: string
  enrolled_at: string
}
function statusOf(isPublished: boolean, archivedAt?: string | null): CourseStatus {
  if (archivedAt) {
    return 'Archived'
  }
  return isPublished ? 'Published' : 'Draft'
}
const mapOutline = (item: OutlineWire) => ({
  id: String(item.id),
  title: item.title,
  orderIndex: item.order_index,
})

const mapPublic = (course: PublicCourseWire): PublicCourse => ({
  id: String(course.id),
  title: course.title,
  description: course.description,
  category: { id: String(course.category.id), name: course.category.name },
  teacher: { id: String(course.teacher.id), fullName: course.teacher.full_name },
  thumbnailUrl: course.thumbnail_url ?? undefined,
  price: String(course.price),
  currency: course.currency,
  totalStudents: course.total_students,
  lessonCount: course.lesson_count ?? (course.outline?.length ?? 0),
  outline: (course.outline ?? []).map(mapOutline),
})
const mapOwned = (course: OwnedCourseWire): OwnedCourse => ({
  id: String(course.id),
  title: course.title,
  description: course.description,
  categoryId: String(course.category_id),
  categoryName: course.category_name,
  teacherId: String(course.teacher_id),
  teacherName: course.teacher_name ?? '',
  thumbnailUrl: course.thumbnail_url ?? undefined,
  price: String(course.price),
  status: statusOf(course.is_published, course.archived_at),
  lessonCount: course.lesson_count ?? 0,
  totalStudents: course.total_students ?? 0,
  archivedAt: course.archived_at ?? undefined,
  createdAt: course.created_at,
  updatedAt: course.updated_at,
})
const mapMember = (member: MemberWire): CourseMember => ({
  courseId: String(member.course_id),
  studentId: String(member.student_id),
  fullName: member.full_name,
  enrolledAt: member.enrolled_at,
})
const writeBody = (values: CourseFormValues) => ({
  title: values.title.trim(),
  description: values.description.trim(),
  category_id: values.categoryId,
  price: values.price,
})
export const courseKeys = {
  explore: (params: { q?: string; categoryId?: Id; page?: number; pageSize?: number }) =>
    ['courses', 'explore', params] as const,
  publicDetail: (courseId: Id) => ['courses', 'public', courseId] as const,
  categories: ['categories'] as const,
  owned: (courseId: Id) => ['courses', 'owned', courseId] as const,
  teaching: ['courses', 'teaching'] as const,
  mine: ['courses', 'mine'] as const,
  members: (courseId: Id, page: number) => ['courses', 'members', courseId, page] as const,
}
export interface ExploreParams extends PageParams {
  q?: string
  categoryId?: Id
}
export async function searchCourses(params: ExploreParams): Promise<Page<PublicCourse>> {
  const page = await apiGet<Page<PublicCourseWire>>('/courses', {
    query: { q: params.q, category_id: params.categoryId, page: params.page, page_size: params.page_size },
  })
  return { ...page, items: page.items.map(mapPublic) }
}
export async function getPublicCourse(courseId: Id): Promise<PublicCourse> {
  return mapPublic(await apiGet<PublicCourseWire>(`/courses/${courseId}`))
}
export async function getCategories(): Promise<Category[]> {

  const page = await apiGet<Page<CategoryWire>>('/categories', { query: { page_size: 100 } })
  return page.items.map((category) => ({ id: String(category.id), name: category.name }))
}

export async function enroll(courseId: Id): Promise<void> {
  await apiPost(`/courses/${courseId}/enroll`)
}
export async function getMyCourses(params: PageParams): Promise<Page<EnrolledCourse>> {
  const page = await apiGet<Page<PublicCourseWire>>('/users/me/courses', {
    query: { page: params.page, page_size: params.page_size },
  })
  return {
    ...page,
    items: page.items.map((course) => ({
      ...mapPublic(course),
      enrolledAt: course.enrolled_at ?? '',
      archivedAt: course.archived_at ?? undefined,
    })),
  }
}
export async function listOwnedCourses(params: PageParams): Promise<Page<OwnedCourse>> {
  const page = await apiGet<Page<OwnedCourseWire>>('/teaching/courses', {
    query: { page: params.page, page_size: params.page_size },
  })
  return { ...page, items: page.items.map(mapOwned) }
}
export async function getOwnedCourse(courseId: Id): Promise<OwnedCourse> {
  return mapOwned(await apiGet<OwnedCourseWire>(`/teaching/courses/${courseId}`))
}
export async function createCourse(values: CourseFormValues): Promise<OwnedCourse> {
  return mapOwned(await apiPost<OwnedCourseWire>('/courses', writeBody(values)))
}
export async function updateCourse(courseId: Id, values: CourseFormValues): Promise<OwnedCourse> {
  return mapOwned(await apiPatch<OwnedCourseWire>(`/courses/${courseId}`, writeBody(values)))
}
export async function uploadThumbnail(courseId: Id, file: File): Promise<OwnedCourse> {
  const body = new FormData()
  body.append('file', file)
  return mapOwned(await apiPost<OwnedCourseWire>(`/courses/${courseId}/thumbnail`, body))
}

export async function setPublished(courseId: Id, published: boolean): Promise<OwnedCourse> {
  return mapOwned(
    await apiPatch<OwnedCourseWire>(`/courses/${courseId}/publication`, { is_published: published }),
  )
}

export async function archiveCourse(courseId: Id): Promise<OwnedCourse> {
  return mapOwned(await apiPatch<OwnedCourseWire>(`/courses/${courseId}/archive`, { archived: true }))
}
export async function getMembers(courseId: Id, params: PageParams): Promise<Page<CourseMember>> {
  const page = await apiGet<Page<MemberWire>>(`/courses/${courseId}/members`, {
    query: { page: params.page, page_size: params.page_size },
  })
  return { ...page, items: page.items.map(mapMember) }
}
export const deleteCourse = (courseId: Id) => apiDelete(`/courses/${courseId}`)
