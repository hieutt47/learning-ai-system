import { apiGet } from '../../../shared/api/client'
import type { Page } from '../../../shared/api/types'
import type { Flashcard } from '../../teaching/ai/api'

export const getPublishedCards = (lessonId: string, page: number, pageSize: number) =>
  apiGet<Page<Flashcard>>(`/lessons/${lessonId}/flashcards`, {
    query: { page, page_size: pageSize },
  })
