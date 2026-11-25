import {
  codePointLength,
  isBlankText,
  MAX_QUIZ_TITLE_LENGTH,
  normalizeOptionContent,
  trimUnicodeWhitespace,
  validateQuestion,
  type QuestionValidationResult,
} from '../quizzes/quizValidation'
import type { QuizQuestion } from './api'

export function normalizeCompareText(value: string): string {
  return normalizeOptionContent(value)
}

export function duplicateIndexes(values: string[]): Set<number> {
  const firstIndexByText = new Map<string, number>()
  const duplicates = new Set<number>()
  values.forEach((raw, index) => {
    const normalized = normalizeCompareText(raw)
    if (!normalized) return
    const existing = firstIndexByText.get(normalized)
    if (existing !== undefined) {
      duplicates.add(existing)
      duplicates.add(index)
    } else {
      firstIndexByText.set(normalized, index)
    }
  })
  return duplicates
}

export function validateAiQuestion(question: QuizQuestion, index = 0): QuestionValidationResult {
  return validateQuestion(
    {
      content: question.content,
      explanation: question.explanation,
      options: question.options.map((option) => ({
        content: option.content,
        isCorrect: option.is_correct,
      })),
    },
    index,
  )
}

export function questionErrors(question: QuizQuestion, isDuplicate = false): string[] {
  const errors = [...validateAiQuestion(question).messages]
  if (isDuplicate) {
    errors.push('Nội dung câu hỏi bị trùng với câu khác trong bản nháp')
  }
  return errors
}

export function cardErrors(
  card: { front_text: string; back_text: string },
  isDuplicate = false,
): string[] {
  const errors: string[] = []
  const front = trimUnicodeWhitespace(card.front_text)
  const back = trimUnicodeWhitespace(card.back_text)
  if (!front) {
    errors.push('Nhập mặt trước')
  } else if (codePointLength(front) > 4000) {
    errors.push('Mặt trước không vượt quá 4000 ký tự')
  }
  if (isDuplicate) {
    errors.push('Mặt trước bị trùng với thẻ khác trong bản nháp')
  }
  if (!back) {
    errors.push('Nhập mặt sau')
  } else if (codePointLength(back) > 4000) {
    errors.push('Mặt sau không vượt quá 4000 ký tự')
  }
  return errors
}

/**
 * Kiểm tra tên quiz trên trang xem trước AI.
 * - Undefined hoặc chuỗi rỗng hoàn toàn: hợp lệ (sẽ dùng mặc định "Quiz do AI tạo").
 * - Có nhập ký tự nhưng chỉ toàn khoảng trắng Unicode: không hợp lệ.
 * - Quá 200 code points: không hợp lệ.
 */
export function aiQuizTitleError(title: string | undefined): string | null {
  if (title === undefined || title === '') {
    return null
  }
  if (isBlankText(title)) {
    return 'Tên quiz không được chỉ chứa khoảng trắng.'
  }
  if (codePointLength(title) > MAX_QUIZ_TITLE_LENGTH) {
    return `Tên quiz tối đa ${MAX_QUIZ_TITLE_LENGTH} ký tự.`
  }
  return null
}

export interface AiQuizPreflightResult {
  titleError: string | null
  questionResults: QuestionValidationResult[]
  duplicateQuestionIndices: Set<number>
  summaryErrors: { key: string; targetId: string; message: string }[]
  isValid: boolean
}

export function preflightAiQuizDraft(
  title: string | undefined,
  items: QuizQuestion[],
): AiQuizPreflightResult {
  const titleError = aiQuizTitleError(title)
  const questionResults = items.map((q, idx) => validateAiQuestion(q, idx))
  const duplicateQuestionIndices = duplicateIndexes(items.map((q) => q.content))
  const summaryErrors: { key: string; targetId: string; message: string }[] = []

  if (titleError) {
    summaryErrors.push({ key: 'ai-quiz-title', targetId: 'quiz-title', message: titleError })
  }
  if (items.length === 0) {
    summaryErrors.push({
      key: 'ai-quiz-empty',
      targetId: 'quiz-title',
      message: 'Cần có ít nhất 1 câu hỏi trước khi lưu.',
    })
  }
  for (const res of questionResults) {
    const qNum = res.questionIndex + 1
    const isDuplicateQuestion = duplicateQuestionIndices.has(res.questionIndex)
    if (res.isValid && !isDuplicateQuestion) continue
    if (res.contentBlank || res.contentTooLong) {
      summaryErrors.push({
        key: `ai-q-${res.questionIndex}-content`,
        targetId: `ai-q-${res.questionIndex}-content`,
        message: res.contentBlank
          ? `Câu ${qNum}: chưa nhập nội dung câu hỏi`
          : `Câu ${qNum}: nội dung câu hỏi vượt quá 4000 ký tự`,
      })
    }
    if (isDuplicateQuestion) {
      summaryErrors.push({
        key: `ai-q-${res.questionIndex}-dup-question`,
        targetId: `ai-q-${res.questionIndex}-content`,
        message: `Câu ${qNum}: nội dung câu hỏi bị trùng với câu khác trong bản nháp`,
      })
    }
    if (res.invalidOptionCount) {
      summaryErrors.push({
        key: `ai-q-${res.questionIndex}-opt-count`,
        targetId: `ai-q-${res.questionIndex}-content`,
        message: `Câu ${qNum}: cần từ 2 đến 6 đáp án`,
      })
    }
    for (const oIdx of res.blankOptionIndices) {
      summaryErrors.push({
        key: `ai-q-${res.questionIndex}-opt-${oIdx}-blank`,
        targetId: `ai-q-${res.questionIndex}-opt-${oIdx}`,
        message: `Câu ${qNum}: đáp án ${String.fromCharCode(65 + oIdx)} đang để trống`,
      })
    }
    for (const oIdx of res.tooLongOptionIndices) {
      summaryErrors.push({
        key: `ai-q-${res.questionIndex}-opt-${oIdx}-long`,
        targetId: `ai-q-${res.questionIndex}-opt-${oIdx}`,
        message: `Câu ${qNum}: đáp án ${String.fromCharCode(65 + oIdx)} vượt quá 1000 ký tự`,
      })
    }
    for (const group of res.duplicateGroups) {
      summaryErrors.push({
        key: `ai-q-${res.questionIndex}-dup-${group.optionIndices.join('-')}`,
        targetId: `ai-q-${res.questionIndex}-opt-${group.optionIndices[1] ?? group.optionIndices[0]}`,
        message: `Câu ${qNum}: đáp án ${group.labels.join(', ')} trùng nội dung`,
      })
    }
    if (res.invalidCorrectCount) {
      summaryErrors.push({
        key: `ai-q-${res.questionIndex}-correct`,
        targetId: `ai-q-${res.questionIndex}-opt-0`,
        message: `Câu ${qNum}: chọn đúng 1 đáp án đúng`,
      })
    }
    if (res.explanationTooLong) {
      summaryErrors.push({
        key: `ai-q-${res.questionIndex}-explanation`,
        targetId: `ai-q-${res.questionIndex}-explanation`,
        message: `Câu ${qNum}: giải thích vượt quá 4000 ký tự`,
      })
    }
  }

  return {
    titleError,
    questionResults,
    duplicateQuestionIndices,
    summaryErrors,
    isValid: summaryErrors.length === 0,
  }
}
