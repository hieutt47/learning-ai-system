import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, App, Button, Card, Form, Input, Modal, Select, Space, Table, type TableColumnsType } from 'antd'
import { useState } from 'react'
import { isApiError } from '../../shared/api/client'
import type { Id } from '../../shared/api/types'
import { EmptyState, ErrorState, LoadingState } from '../../shared/components/PageState'
import { Icon } from '../../shared/components/Icon'
import '../../shared/styles/management.css'
import { createCategory, deleteCategory, getCategories, updateCategory } from '../categories/api'
import type { Category, CategoryWrite } from '../categories/types'

export function CategoriesPage() {
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [sort, setSort] = useState('name,asc')
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<Category | null>(null)
  const [deleting, setDeleting] = useState<Category | null>(null)
  const [form] = Form.useForm<CategoryWrite>()
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const params = { page, page_size: pageSize, sort }
  const query = useQuery({
    queryKey: ['categories', params],
    queryFn: ({ signal }) => getCategories(params, signal),
    placeholderData: keepPreviousData,
  })
  const saveMutation = useMutation({
    mutationFn: ({ id, request }: { id: Id | null; request: CategoryWrite }) => id ? updateCategory(id, request) : createCategory(request),
    onSuccess: async (_, variables) => {
      setFormOpen(false)
      void message.success(variables.id ? 'Đã lưu danh mục' : 'Đã thêm danh mục')
      await queryClient.invalidateQueries({ queryKey: ['categories'] })
    },
    onError: (error) => {
      if (isApiError(error)) {
        if (error.code === 'INVALID_STATE') {
          form.setFields([{ name: 'name', errors: ['Danh mục này đã tồn tại.'] }])
        } else {
          const errors = error.fieldErrors()
          for (const name of ['name', 'description'] as const) {
            if (errors[name]) form.setFields([{ name, errors: [errors[name]] }])
          }
        }
        if (error.status === 404) {
          setFormOpen(false)
          setPage(1)
          void message.warning('Danh mục không còn tồn tại. Vui lòng kiểm tra danh sách mới.')
          void queryClient.invalidateQueries({ queryKey: ['categories'] })
        }
      }
    },
  })
  const deleteMutation = useMutation({
    mutationFn: (category: Category) => deleteCategory(category.id),
    onSuccess: async () => {
      setDeleting(null)
      const lastPage = Math.max(1, Math.ceil(((query.data?.total ?? 1) - 1) / pageSize))
      setPage(Math.min(page, lastPage))
      void message.success('Đã xóa danh mục')
      await queryClient.invalidateQueries({ queryKey: ['categories'] })
    },
    onError: (error, category) => {
      if (isApiError(error) && error.code === 'RESOURCE_IN_USE') {
        setDeleting(null)
        modal.warning({
          title: 'Không thể xóa danh mục',
          content: `“${category.name}” đang được khóa học sử dụng. Hãy chuyển các khóa học sang danh mục khác trước khi xóa.`,
          okText: 'Đã hiểu',
        })
      } else if (isApiError(error) && error.status === 404) {
        setDeleting(null)
        setPage(1)
        void message.warning('Danh mục không còn tồn tại. Vui lòng kiểm tra danh sách mới.')
        void queryClient.invalidateQueries({ queryKey: ['categories'] })
      }
    },
  })
  const busy = saveMutation.isPending || deleteMutation.isPending

  function openForm(category: Category | null): void {
    saveMutation.reset()
    form.resetFields()
    setEditing(category)
    form.setFieldsValue({ name: category?.name ?? '', description: category?.description ?? '' })
    setFormOpen(true)
  }

  const columns: TableColumnsType<Category> = [
    { title: 'Tên danh mục', dataIndex: 'name', width: 260, render: (name: string, category) => <div><b>{name}</b><div className="account-id">Mã: {category.id}</div></div> },
    { title: 'Mô tả', dataIndex: 'description', width: 360, render: (description: string | null) => description ? <p className="account-note">{description}</p> : <span className="account-muted">Chưa có mô tả</span> },
    {
      title: 'Hành động', key: 'actions', width: 180, fixed: 'right',
      render: (_, category) => (
        <Space>
          <Button size="small" icon={<Icon name="edit" />} aria-label={`Sửa danh mục ${category.name}`} disabled={busy || query.isPlaceholderData} onClick={() => openForm(category)}>Sửa</Button>
          <Button size="small" danger icon={<Icon name="trash" />} aria-label={`Xóa danh mục ${category.name}`} disabled={busy || query.isPlaceholderData} onClick={() => { deleteMutation.reset(); setDeleting(category) }}>Xóa</Button>
        </Space>
      ),
    },
  ]

  const saveFieldError = isApiError(saveMutation.error) && (saveMutation.error.code === 'INVALID_STATE' || saveMutation.error.details.some((detail) => detail.field === 'name' || detail.field === 'description'))

  return (
    <div className="account-page">
      <p className="account-description">Danh mục giúp sinh viên lọc khóa học ở trang Khám phá. Giảng viên chọn danh mục khi tạo khóa học.</p>
      <Card>
        <div className="account-toolbar">
          <h2>Tất cả danh mục</h2>
          <div className="account-controls">
            <Select aria-label="Sắp xếp danh mục" value={sort} disabled={busy} options={[
              { value: 'name,asc', label: 'Tên A–Z' },
              { value: 'name,desc', label: 'Tên Z–A' },
              { value: 'created_at,desc', label: 'Mới nhất' },
            ]} onChange={(value) => { setSort(value); setPage(1) }} />
            <Button icon={<Icon name="refresh" />} loading={query.isFetching} disabled={busy} aria-label="Làm mới" aria-busy={query.isFetching} onClick={() => void query.refetch()}>Làm mới</Button>
            <Button type="primary" icon={<Icon name="plus" />} disabled={busy} onClick={() => openForm(null)}>Thêm danh mục</Button>
          </div>
        </div>
        {query.isPending ? <LoadingState /> : query.isError ? <ErrorState error={query.error} onRetry={() => void query.refetch()} /> : (
          <Table<Category>
            rowKey="id"
            columns={columns}
            dataSource={query.data.items}
            loading={query.isFetching}
            scroll={{ x: 800 }}
            locale={{ emptyText: <EmptyState description="Chưa có danh mục." /> }}
            pagination={{
              current: page, pageSize, total: query.data.total, showSizeChanger: true, pageSizeOptions: [10, 20, 50, 100],
              showTotal: (total) => `${total} danh mục`, disabled: busy || query.isFetching,
              onChange: (nextPage, nextSize) => { setPage(nextSize === pageSize ? nextPage : 1); setPageSize(nextSize) },
            }}
          />
        )}
        <p className="account-description">Chỉ xóa được danh mục không có khóa học nào đang dùng.</p>
      </Card>
      <Modal
        open={formOpen}
        title={editing ? 'Sửa danh mục' : 'Thêm danh mục'}
        okText={editing ? 'Lưu' : 'Thêm'}
        cancelText="Hủy"
        forceRender
        confirmLoading={saveMutation.isPending}
        okButtonProps={{ disabled: saveMutation.isPending, 'aria-label': editing ? 'Lưu' : 'Thêm', 'aria-busy': saveMutation.isPending }}
        cancelButtonProps={{ disabled: saveMutation.isPending }}
        closable={!saveMutation.isPending}
        mask={{ closable: !saveMutation.isPending }}
        keyboard={!saveMutation.isPending}
        onCancel={() => { if (!saveMutation.isPending) setFormOpen(false) }}
        onOk={() => { if (!saveMutation.isPending) form.submit() }}
      >
        <div className="account-stack">
          {saveMutation.error && !saveFieldError && <Alert type="error" showIcon title={saveMutation.error.message} />}
          <Form<CategoryWrite>
            form={form}
            name="category"
            layout="vertical"
            requiredMark={false}
            disabled={saveMutation.isPending}
            onValuesChange={() => saveMutation.reset()}
            onFinish={(values) => {
              if (!saveMutation.isPending) saveMutation.mutate({ id: editing?.id ?? null, request: { name: values.name.trim(), description: values.description ?? '' } })
            }}
          >
            <Form.Item name="name" label="Tên danh mục" rules={[
              { required: true, whitespace: true, message: 'Nhập tên danh mục.' },
              { max: 120, message: 'Tên danh mục tối đa 120 ký tự.' },
            ]}>
              <Input autoComplete="off" />
            </Form.Item>
            <Form.Item name="description" label="Mô tả" extra="Không bắt buộc.">
              <Input.TextArea rows={4} />
            </Form.Item>
          </Form>
        </div>
      </Modal>
      <Modal
        open={deleting !== null}
        title="Xóa danh mục?"
        okText="Xóa danh mục"
        cancelText="Hủy"
        okButtonProps={{ danger: true, disabled: deleteMutation.isPending, 'aria-label': 'Xóa danh mục', 'aria-busy': deleteMutation.isPending }}
        confirmLoading={deleteMutation.isPending}
        cancelButtonProps={{ disabled: deleteMutation.isPending }}
        closable={!deleteMutation.isPending}
        mask={{ closable: !deleteMutation.isPending }}
        keyboard={!deleteMutation.isPending}
        onCancel={() => { if (!deleteMutation.isPending) setDeleting(null) }}
        onOk={() => { if (deleting && !deleteMutation.isPending) deleteMutation.mutate(deleting) }}
      >
        <div className="account-stack">
          <p>Danh mục “{deleting?.name}” sẽ bị xóa.</p>
          {deleteMutation.error && <Alert type="error" showIcon title={deleteMutation.error.message} />}
        </div>
      </Modal>
    </div>
  )
}
