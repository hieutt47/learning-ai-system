import { useQuery } from '@tanstack/react-query'
import { Button, Card, Col, Input, Pagination, Row, Space, Tag, Typography } from 'antd'
import { useState } from 'react'
import { Link } from 'react-router'
import { EmptyState, ErrorState, LoadingState } from '../../shared/components/PageState'
import { paths } from '../../shared/routing/paths'
import { courseKeys, getMyCourses } from './course.api'
import { CourseCover } from './CourseCard'
import { formatDate } from './format'

const PAGE_SIZE = 12

export function MyCoursesPage() {
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(1)
  const mineQuery = useQuery({
    queryKey: courseKeys.mine,
    queryFn: () => getMyCourses({ page: 1, page_size: 100 }),
  })

  if (mineQuery.isLoading) return <LoadingState tip="Đang tải khóa học của bạn…" />
  if (mineQuery.isError) return <ErrorState error={mineQuery.error} onRetry={() => void mineQuery.refetch()} />
  const all = mineQuery.data?.items ?? []
  const normalized = query.trim().toLowerCase()
  const filtered = normalized ? all.filter((course) => course.title.toLowerCase().includes(normalized)) : all
  const visible = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE)

  if (all.length === 0) {
    return (
      <Card>
        <EmptyState
          description="Bạn chưa ghi danh khóa học nào. Khám phá các khóa học miễn phí và bắt đầu học ngay hôm nay."
          action={
            <Link to={paths.explore}>
              <Button type="primary">Khám phá khóa học</Button>
            </Link>
          }
        />
      </Card>
    )
  }
  return (
    <Space orientation="vertical" size={20} style={{ width: '100%' }}>
      <Row gutter={[12, 12]} justify="space-between" align="middle">
        <Col>
          <Typography.Title level={3} style={{ margin: 0 }}>
            Đã ghi danh
          </Typography.Title>
          <Typography.Text type="secondary">Sắp xếp theo ngày ghi danh, mới nhất trước.</Typography.Text>
        </Col>
        <Col>
          <Input.Search
            allowClear
            placeholder="Tìm trong khóa của tôi"
            value={query}
            onChange={(event) => {
              setQuery(event.target.value)
              setPage(1)
            }}
            style={{ width: 260 }}
          />
        </Col>
      </Row>
      {filtered.length === 0 ? (
        <Card>
          <EmptyState description="Không có khóa học khớp. Thử từ khóa khác." />
        </Card>
      ) : (
        <>
          <Row gutter={[16, 16]}>
            {visible.map((course) => (
              <Col key={course.id} xs={24} sm={12} lg={8}>
                <Card
                  cover={<CourseCover course={course} />}
                  styles={{ body: { display: 'flex', flexDirection: 'column', gap: 10 } }}
                >
                  <Tag color="blue" style={{ alignSelf: 'flex-start', margin: 0 }}>
                    {course.category.name}
                  </Tag>
                  <strong style={{ fontSize: 16 }}>{course.title}</strong>
                  <Typography.Text type="secondary" style={{ fontSize: 13 }}>
                    {course.teacher.fullName}
                  </Typography.Text>
                  {course.archivedAt ? (
                    <Typography.Text type="warning" style={{ fontSize: 13 }}>
                      Giảng viên đã lưu trữ khóa này. Bạn vẫn vào học và xem lại nội dung được.
                    </Typography.Text>
                  ) : null}
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Typography.Text type="secondary" style={{ fontSize: 13 }}>
                      Ghi danh {formatDate(course.enrolledAt)}
                    </Typography.Text>
                    <Link to={paths.learnLesson(course.id, course.outline[0]?.id ?? '')}>
                      <Button type="primary" size="small">
                        Vào học
                      </Button>
                    </Link>
                  </div>
                </Card>
              </Col>
            ))}
          </Row>
          {filtered.length > PAGE_SIZE && (
            <Pagination
              align="center"
              current={page}
              pageSize={PAGE_SIZE}
              total={filtered.length}
              showSizeChanger={false}
              onChange={setPage}
            />
          )}
        </>
      )}
    </Space>
  )
}
