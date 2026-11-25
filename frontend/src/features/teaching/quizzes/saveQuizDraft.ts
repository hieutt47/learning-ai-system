import { isApiError } from '../../../shared/api/client'
import { saveDraftSnapshot } from './quiz.api'
import { trimUnicodeWhitespace } from './quizValidation'
import type { QuestionWriteRequest, QuizQuestionAuthor } from './quiz.types'

export function questionPayload(question: QuizQuestionAuthor, index: number): QuestionWriteRequest {
  const trimmedExplanation = question.explanation ? trimUnicodeWhitespace(question.explanation) : ''
  return {
    content: trimUnicodeWhitespace(question.content),
    explanation: trimmedExplanation || null,
    order_index: index,
    options: question.options.map((option, position) => ({
      content: trimUnicodeWhitespace(option.content),
      is_correct: option.isCorrect,
      order_index: position,
    })),
  }
}

// One saver per mounted editor. On ANY failure retain the exact snapshot/key;
// the next explicit save replays it before accepting edits/new IDs.
export function createDraftSaver() {
  let pending: { quizId: string; body: Parameters<typeof saveDraftSnapshot>[1] } | undefined
  return async (quizId: string, title: string, questions: QuizQuestionAuthor[]) => {
    if (!pending) {
      pending = { quizId, body: {
        request_id: crypto.randomUUID(), title: trimUnicodeWhitespace(title),
        questions: questions.map((q, i) => ({ ...questionPayload(q, i), ...(q.id ? { id: q.id } : {}) })),
      } }
    }
    try {
      const saved = await saveDraftSnapshot(pending.quizId, pending.body)
      pending = undefined
      return saved
    } catch (error) {
      // Rejected payloads did not commit. Network/5xx are ambiguous: replay exact key.
      if (isApiError(error) && error.status >= 400 && error.status < 500) pending = undefined
      throw error
    }
  }
}
