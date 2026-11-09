import { useQuery } from '@tanstack/react-query'
import { Button, Col, Input, Pagination, Row, Segmented, Space, Typography } from 'antd'
import { useState } from 'react'
import { useSearchParams } from 'react-router'
import { EmptyState, ErrorState, LoadingState } from '../../shared/components/PageState'
import { courseKeys, getCategories, searchCourses } from './course.api'
import { CourseCard } from './CourseCard'

const PAGE_SIZE = 12

/**
 * Khám phá khóa học (prototype #explore): khoá công khai, lọc danh mục, tìm kiếm
 * theo `?q=` từ ô tìm trên header, lưới thẻ, phân trang.
 *
 * Không có trạng thái "chưa đăng nhập": guest xem được đúng như sinh viên, chỉ
 * khác ở trang chi tiết khi bấm ghi danh.
 */
export function ExplorePage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const q = searchParams.get('q') ?? ''
  const categoryId = searchParams.get('category_id') ?? undefined
  const page = Number(searchParams.get('page') ?? '1') || 1

  // Ô tìm riêng của màn, đồng bộ với ?q= trên URL để tải lại/back vẫn giữ từ khóa.
  const [draftQuery, setDraftQuery] = useState(q)

  const categoriesQuery = useQuery({ queryKey: courseKeys.categories, queryFn: getCategories })
  const coursesQuery = useQuery({
    queryKey: courseKeys.explore({ q, categoryId, page, pageSize: PAGE_SIZE }),
    queryFn: () => searchCourses({ q: q || undefined, categoryId, page, page_size: PAGE_SIZE }),
  })

  const updateParams = (next: Record<string, string | undefined>) => {
    const params = new URLSearchParams(searchParams)
    for (const [key, value] of Object.entries(next)) {
      if (value) {
        params.set(key, value)
      } else {
        params.delete(key)
      }
    }
    // Đổi bộ lọc luôn quay về trang 1, trừ khi chính trang được đổi.
    if (!('page' in next)) {
      params.delete('page')
    }
    setSearchParams(params, { replace: true })
  }

  const submitSearch = () => updateParams({ q: draftQuery.trim() || undefined })
  const clearFilters = () => {
    setDraftQuery('')
    setSearchParams(new URLSearchParams(), { replace: true })
  }

  const categories = categoriesQuery.data ?? []
  const total = coursesQuery.data?.total ?? 0
  const items = coursesQuery.data?.items ?? []

  return (
    <Space orientation="vertical" size={20} style={{ width: '100%' }}>
      <div>
        <Typography.Title level={2} style={{ margin: 0 }}>
          Học gì hôm nay?
        </Typography.Title>
        <Typography.Text type="secondary">
          Tất cả khóa học đều miễn phí. Chọn danh mục hoặc tìm theo tên khóa học.
        </Typography.Text>
      </div>

      <Row gutter={[12, 12]} align="middle" justify="space-between">
        <Col flex="auto">
          <Segmented
            value={categoryId ?? 'all'}
            onChange={(value) => updateParams({ category_id: value === 'all' ? undefined : String(value) })}
            options={[
              { label: 'Tất cả', value: 'all' },
              ...categories.map((category) => ({ label: category.name, value: category.id })),
            ]}
          />
        </Col>
        <Col>
          <Space.Compact>
            <Input.Search
              allowClear
              value={draftQuery}
              placeholder="Tìm theo tên khóa học"
              onChange={(event) => setDraftQuery(event.target.value)}
              onSearch={submitSearch}
              enterButton
              style={{ width: 280 }}
            />
          </Space.Compact>
        </Col>
      </Row>

      {coursesQuery.isLoading && <LoadingState tip="Đang tải khóa học…" />}
      {coursesQuery.isError && (
        <ErrorState error={coursesQuery.error} onRetry={() => void coursesQuery.refetch()} />
      )}

      {coursesQuery.isSuccess && items.length === 0 && (
        <EmptyState
          description={
            q
              ? `Không có khóa học nào khớp “${q}”. Thử từ khóa khác hoặc xóa bộ lọc.`
              : 'Danh mục này chưa có khóa học nào.'
          }
          action={
            <Button type="primary" onClick={clearFilters}>
              Xóa bộ lọc
            </Button>
          }
        />
      )}

      {coursesQuery.isSuccess && items.length > 0 && (
        <>
          <Typography.Text type="secondary">{total} khóa học</Typography.Text>
          <Row gutter={[16, 16]}>
            {items.map((course) => (
              <Col key={course.id} xs={24} sm={12} lg={8} xxl={6}>
                <CourseCard course={course} />
              </Col>
            ))}
          </Row>
          <Pagination
            align="center"
            current={page}
            pageSize={PAGE_SIZE}
            total={total}
            showSizeChanger={false}
            onChange={(nextPage) => updateParams({ page: String(nextPage) })}
          />
        </>
      )}
    </Space>
  )
}
