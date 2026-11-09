import { Card, Tag } from 'antd'
import { Link } from 'react-router'
import { paths } from '../../shared/routing/paths'
import type { PublicCourse } from './course.types'

/** Ảnh bìa: dùng thumbnail nếu có, không thì khối màu theo danh mục (như prototype). */
export function CourseCover({ course, height = 160 }: { course: PublicCourse; height?: number }) {
  if (course.thumbnailUrl) {
    return (
      <img
        src={course.thumbnailUrl}
        alt={`Ảnh bìa khóa ${course.title}`}
        style={{ width: '100%', height, objectFit: 'cover', display: 'block' }}
      />
    )
  }
  return (
    <div
      aria-hidden
      style={{
        height,
        display: 'grid',
        placeItems: 'center',
        background: 'var(--surface-2, #eef2ff)',
        color: 'var(--primary, #4338ca)',
        fontWeight: 600,
        fontSize: 14,
      }}
    >
      {course.category.name}
    </div>
  )
}

/** Một thẻ khóa học trong lưới Khám phá / Khóa học của tôi. */
export function CourseCard({ course }: { course: PublicCourse }) {
  return (
    <Link to={paths.courseDetail(course.id)} style={{ color: 'inherit', textDecoration: 'none' }}>
      <Card
        hoverable
        cover={<CourseCover course={course} />}
        styles={{ body: { display: 'flex', flexDirection: 'column', gap: 8 } }}
      >
        <Tag color="blue" style={{ alignSelf: 'flex-start', margin: 0 }}>
          {course.category.name}
        </Tag>
        <strong style={{ fontSize: 16, lineHeight: 1.4 }}>{course.title}</strong>
        <span style={{ color: 'var(--text-2, rgba(0,0,0,0.55))', fontSize: 13 }}>
          {course.teacher.fullName}
        </span>
        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13 }}>
          <span>{course.lessonCount} bài học</span>
          <span>{course.totalStudents} học viên</span>
        </div>
      </Card>
    </Link>
  )
}
