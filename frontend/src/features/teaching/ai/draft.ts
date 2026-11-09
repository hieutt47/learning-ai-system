import type { GeneratedCards, GeneratedQuiz, GenerationRequest, QuizQuestion } from './api'

export type AiDraft =
  | {
    kind: 'quiz'
    generation_id: string
    source_truncated: boolean
    items: QuizQuestion[]
    /** Quiz đã tạo; Lưu tiếp sẽ dùng ID này, tránh tạo quiz trùng. */
    quiz_id?: string
    /** Số câu đã lưu; Lưu tiếp bắt đầu từ câu kế tiếp. */
    saved_count: number
    request?: GenerationRequest
    title?: string
  }
  | {
    kind: 'flashcard'
    generation_id: string
    source_truncated: boolean
    items: { front_text: string; back_text: string }[]
    /** Số thẻ đã lưu; Lưu tiếp bắt đầu từ thẻ kế tiếp. */
    saved_count: number
    request?: GenerationRequest
  }

const key = (lessonId: string) => `lesson-ai-preview:${lessonId}`

export function readDraft(lessonId: string): AiDraft | null {
  try {
    return JSON.parse(sessionStorage.getItem(key(lessonId)) ?? 'null') as AiDraft | null
  } catch {
    return null
  }
}

export function writeDraft(lessonId: string, draft: AiDraft): void {
  sessionStorage.setItem(key(lessonId), JSON.stringify(draft))
}

export function clearDraft(lessonId: string): void {
  sessionStorage.removeItem(key(lessonId))
}

export function quizDraft(result: GeneratedQuiz): AiDraft {
  return {
    kind: 'quiz',
    generation_id: result.generation_id,
    source_truncated: result.source_truncated,
    items: result.candidate.questions,
    saved_count: 0,
  }
}

export function cardsDraft(result: GeneratedCards): AiDraft {
  return {
    kind: 'flashcard',
    generation_id: result.generation_id,
    source_truncated: result.source_truncated,
    items: result.candidate.flashcards,
    saved_count: 0,
  }
}
