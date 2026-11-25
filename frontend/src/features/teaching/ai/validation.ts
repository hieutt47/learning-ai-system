import type { QuizQuestion } from './api'

export function normalizeCompareText(value: string): string {
  return value.normalize('NFC').replace(/\s+/g, ' ').trim().toLowerCase()
}

function codePointLength(value: string): number {
  return Array.from(value).length
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

export function questionErrors(question: QuizQuestion, isDuplicate = false): string[] {
  const errors: string[] = []
  const trimmedContent = question.content.trim()
  if (!trimmedContent) {
    errors.push('Nhập nội dung câu hỏi')
  } else if (codePointLength(trimmedContent) > 4000) {
    errors.push('Nội dung câu hỏi không vượt quá 4000 ký tự')
  }
  if (isDuplicate) {
    errors.push('Nội dung câu hỏi bị trùng với câu khác trong bản nháp')
  }

  if (question.options.length < 2 || question.options.length > 6) {
    errors.push('Cần từ 2 đến 6 đáp án')
  }
  if (question.options.some(option => !option.content.trim())) {
    errors.push('Nhập nội dung cho mọi đáp án')
  }
  if (question.options.some(option => codePointLength(option.content.trim()) > 1000)) {
    errors.push('Mỗi đáp án không vượt quá 1000 ký tự')
  }
  if (question.options.filter(option => option.is_correct).length !== 1) {
    errors.push('Chọn đúng một đáp án đúng')
  }

  const seenOptions = new Set<string>()
  for (const option of question.options) {
    const normalized = normalizeCompareText(option.content)
    if (normalized) {
      if (seenOptions.has(normalized)) {
        errors.push('Các phương án đáp án không được trùng lặp nội dung')
        break
      }
      seenOptions.add(normalized)
    }
  }

  if (question.explanation && codePointLength(question.explanation.trim()) > 4000) {
    errors.push('Giải thích không vượt quá 4000 ký tự')
  }
  return errors
}

export function cardErrors(
  card: { front_text: string; back_text: string },
  isDuplicate = false,
): string[] {
  const errors: string[] = []
  const front = card.front_text.trim()
  const back = card.back_text.trim()
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
