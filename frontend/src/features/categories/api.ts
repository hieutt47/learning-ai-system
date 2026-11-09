import { apiDelete, apiGet, apiPatch, apiPost } from '../../shared/api/client'
import type { Id, Page, PageParams } from '../../shared/api/types'
import type { Category, CategoryUpdate, CategoryWrite } from './types'

export function getCategories(params: PageParams, signal?: AbortSignal): Promise<Page<Category>> {
  return apiGet<Page<Category>>('/categories', { query: { ...params }, signal })
}

export function createCategory(request: CategoryWrite): Promise<Category> {
  return apiPost<Category>('/categories', request)
}

export function updateCategory(id: Id, request: CategoryUpdate): Promise<Category> {
  return apiPatch<Category>(`/categories/${id}`, request)
}

export function deleteCategory(id: Id): Promise<void> {
  return apiDelete<void>(`/categories/${id}`)
}
