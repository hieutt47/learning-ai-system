import type { Id } from '../../shared/api/types'

export interface Category {
  id: Id
  name: string
  description: string | null
}

export interface CategoryWrite {
  name: string
  description?: string | null
}

export interface CategoryUpdate {
  name?: string | null
  description?: string | null
}
