import type { QuizQuestion } from './api'

export function questionErrors(question: QuizQuestion): string[] {
  const errors: string[] = []
  if (!question.content.trim()) errors.push('Nhập nội dung câu hỏi')
  if (question.options.length < 2 || question.options.length > 6) {
    errors.push('Cần từ 2 đến 6 đáp án')
  }
  if (question.options.some(option => !option.content.trim())) {
    errors.push('Nhập nội dung cho mọi đáp án')
  }
  if (question.options.filter(option => option.is_correct).length !== 1) {
    errors.push('Chọn đúng một đáp án')
  }
  return errors
}
