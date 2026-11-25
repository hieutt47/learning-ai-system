/**
 * Quy tắc chuẩn hóa và kiểm tra hợp lệ quiz phía frontend, đối chiếu trực tiếp với
 * backend WriteQuestionRequest.java / SaveQuizDraftRequest.java:
 * - Chuẩn hóa NFC (không NFKC)
 * - Gộp mọi chuỗi Unicode White_Space (\p{White_Space}, gồm NBSP, U+0085, tab, newline, ...) thành 1 dấu cách
 * - Cắt khoảng trắng Unicode hai đầu
 * - Chuyển chữ thường không phụ thuộc locale trình duyệt (ECMA-262 toLowerCase ≈ Java Locale.ROOT)
 * - Đếm độ dài theo Unicode code points (an toàn với emoji surrogate pairs)
 */

export const MAX_QUIZ_TITLE_LENGTH = 200
export const MAX_QUESTION_CONTENT_LENGTH = 4000
export const MAX_EXPLANATION_LENGTH = 4000
export const MAX_OPTION_CONTENT_LENGTH = 1000
export const MIN_OPTIONS_PER_QUESTION = 2
export const MAX_OPTIONS_PER_QUESTION = 6
export const MAX_QUESTIONS_PER_QUIZ = 200

const UNICODE_WHITESPACE_RUN = /\p{White_Space}+/gu
const UNICODE_WHITESPACE_EDGE = /^\p{White_Space}+|\p{White_Space}+$/gu

export interface ValidatableOption {
  content: string
  isCorrect: boolean
}

export interface ValidatableQuestion {
  content: string
  explanation?: string | null
  options: ValidatableOption[]
}

export interface DuplicateOptionGroup {
  normalized: string
  optionIndices: number[]
  labels: string[]
}

export interface QuestionValidationResult {
  questionIndex: number
  contentBlank: boolean
  contentTooLong: boolean
  explanationTooLong: boolean
  invalidOptionCount: boolean
  invalidCorrectCount: boolean
  blankOptionIndices: number[]
  tooLongOptionIndices: number[]
  duplicateGroups: DuplicateOptionGroup[]
  duplicateOptionIndices: Set<number>
  messages: string[]
  isValid: boolean
}

export interface ValidationIssueLink {
  key: string
  targetId: string
  message: string
}

export interface QuizValidationResult {
  titleError: string | null
  hasQuestions: boolean
  tooManyQuestions: boolean
  validTitle: boolean
  validOptionCount: boolean
  invalidOptionCounts: number[]
  exactlyOneCorrect: boolean
  invalidCorrectAnswers: number[]
  allContentFilled: boolean
  emptyContents: number[]
  allLengthsValid: boolean
  lengthErrorQuestions: number[]
  noDuplicateOptions: boolean
  duplicateOptionQuestions: number[]
  duplicateSummaries: string[]
  questions: QuestionValidationResult[]
  issues: ValidationIssueLink[]
  isValid: boolean
  canPublish: boolean
}

export function optionLabel(index: number): string {
  return String.fromCharCode(65 + index)
}

export function codePointLength(value: string): number {
  let count = 0
  for (const _ of value) {
    count += 1
  }
  return count
}

export function isBlankText(value: string | null | undefined): boolean {
  if (!value) return true
  return value.replace(UNICODE_WHITESPACE_RUN, '').length === 0
}

export function trimUnicodeWhitespace(value: string): string {
  return value.replace(UNICODE_WHITESPACE_EDGE, '')
}

export function normalizeOptionContent(value: string): string {
  return value
    .normalize('NFC')
    .replace(UNICODE_WHITESPACE_RUN, ' ')
    .replace(UNICODE_WHITESPACE_EDGE, '')
    .toLowerCase()
}

export function validateQuizTitle(title: string | null | undefined): string | null {
  if (isBlankText(title)) {
    return 'Vui lòng nhập tên quiz.'
  }
  if (codePointLength(title!) > MAX_QUIZ_TITLE_LENGTH) {
    return `Tên quiz tối đa ${MAX_QUIZ_TITLE_LENGTH} ký tự.`
  }
  return null
}

/**
 * Tìm các nhóm đáp án trùng nội dung trong cùng 1 câu hỏi sau khi chuẩn hóa.
 * Đáp án rỗng / chỉ chứa khoảng trắng được bỏ qua để tránh báo trùng cho rule bỏ trống.
 */
export function findDuplicateOptionGroups(options: Pick<ValidatableOption, 'content'>[]): DuplicateOptionGroup[] {
  const buckets = new Map<string, number[]>()
  for (let i = 0; i < options.length; i++) {
    const raw = options[i]?.content ?? ''
    if (isBlankText(raw)) continue
    const normalized = normalizeOptionContent(raw)
    if (!normalized) continue
    const list = buckets.get(normalized)
    if (list) {
      list.push(i)
    } else {
      buckets.set(normalized, [i])
    }
  }

  const groups: DuplicateOptionGroup[] = []
  for (const [normalized, optionIndices] of buckets) {
    if (optionIndices.length >= 2) {
      groups.push({
        normalized,
        optionIndices,
        labels: optionIndices.map(optionLabel),
      })
    }
  }
  return groups
}

export function formatDuplicateGroupMessage(
  group: DuplicateOptionGroup,
  questionIndex?: number,
): string {
  const labelList = group.labels.join(', ')
  if (questionIndex !== undefined) {
    return `Câu ${questionIndex + 1}: đáp án ${labelList} trùng nội dung`
  }
  return `Đáp án ${labelList} trùng nội dung`
}

export function validateQuestion(
  question: ValidatableQuestion,
  questionIndex: number,
): QuestionValidationResult {
  const contentBlank = isBlankText(question.content)
  const contentTooLong = !contentBlank && codePointLength(question.content) > MAX_QUESTION_CONTENT_LENGTH
  const explanationTooLong =
    question.explanation != null && codePointLength(question.explanation) > MAX_EXPLANATION_LENGTH

  const invalidOptionCount =
    question.options.length < MIN_OPTIONS_PER_QUESTION ||
    question.options.length > MAX_OPTIONS_PER_QUESTION

  const correctCount = question.options.filter((o) => o.isCorrect).length
  const invalidCorrectCount = correctCount !== 1

  const blankOptionIndices: number[] = []
  const tooLongOptionIndices: number[] = []
  question.options.forEach((opt, idx) => {
    if (isBlankText(opt.content)) {
      blankOptionIndices.push(idx)
    } else if (codePointLength(opt.content) > MAX_OPTION_CONTENT_LENGTH) {
      tooLongOptionIndices.push(idx)
    }
  })

  const duplicateGroups = findDuplicateOptionGroups(question.options)
  const duplicateOptionIndices = new Set<number>()
  for (const group of duplicateGroups) {
    for (const idx of group.optionIndices) {
      duplicateOptionIndices.add(idx)
    }
  }

  const messages: string[] = []
  if (contentBlank) messages.push('Nhập nội dung câu hỏi')
  if (contentTooLong) messages.push(`Nội dung câu hỏi tối đa ${MAX_QUESTION_CONTENT_LENGTH} ký tự`)
  if (invalidOptionCount) {
    messages.push(`Cần từ ${MIN_OPTIONS_PER_QUESTION} đến ${MAX_OPTIONS_PER_QUESTION} đáp án`)
  }
  if (blankOptionIndices.length > 0) {
    messages.push(`Nhập nội dung cho đáp án ${blankOptionIndices.map(optionLabel).join(', ')}`)
  }
  if (tooLongOptionIndices.length > 0) {
    messages.push(
      `Đáp án ${tooLongOptionIndices.map(optionLabel).join(', ')} vượt quá ${MAX_OPTION_CONTENT_LENGTH} ký tự`,
    )
  }
  for (const group of duplicateGroups) {
    messages.push(formatDuplicateGroupMessage(group))
  }
  if (invalidCorrectCount) {
    messages.push('Chọn đúng 1 đáp án đúng')
  }
  if (explanationTooLong) {
    messages.push(`Giải thích tối đa ${MAX_EXPLANATION_LENGTH} ký tự`)
  }

  return {
    questionIndex,
    contentBlank,
    contentTooLong,
    explanationTooLong,
    invalidOptionCount,
    invalidCorrectCount,
    blankOptionIndices,
    tooLongOptionIndices,
    duplicateGroups,
    duplicateOptionIndices,
    messages,
    isValid: messages.length === 0,
  }
}

export function validateQuizDraft(
  title: string,
  questions: ValidatableQuestion[],
): QuizValidationResult {
  const titleError = validateQuizTitle(title)
  const hasQuestions = questions.length >= 1
  const tooManyQuestions = questions.length > MAX_QUESTIONS_PER_QUIZ

  const questionResults = questions.map((q, idx) => validateQuestion(q, idx))

  const invalidOptionCounts = questionResults
    .filter((r) => r.invalidOptionCount)
    .map((r) => r.questionIndex + 1)

  const invalidCorrectAnswers = questionResults
    .filter((r) => r.invalidCorrectCount)
    .map((r) => r.questionIndex + 1)

  const emptyContents = questionResults
    .filter((r) => r.contentBlank || r.blankOptionIndices.length > 0)
    .map((r) => r.questionIndex + 1)

  const lengthErrorQuestions = questionResults
    .filter((r) => r.contentTooLong || r.explanationTooLong || r.tooLongOptionIndices.length > 0)
    .map((r) => r.questionIndex + 1)

  const duplicateOptionQuestions = questionResults
    .filter((r) => r.duplicateGroups.length > 0)
    .map((r) => r.questionIndex + 1)

  const duplicateSummaries: string[] = []
  const issues: ValidationIssueLink[] = []

  if (titleError) {
    issues.push({ key: 'quiz-title', targetId: 'quiz-title-input', message: titleError })
  }
  if (!hasQuestions) {
    issues.push({
      key: 'quiz-no-questions',
      targetId: 'quiz-add-question-btn',
      message: 'Cần có ít nhất 1 câu hỏi.',
    })
  }
  if (tooManyQuestions) {
    issues.push({
      key: 'quiz-too-many-questions',
      targetId: 'quiz-title-input',
      message: `Mỗi quiz tối đa ${MAX_QUESTIONS_PER_QUIZ} câu hỏi.`,
    })
  }

  for (const r of questionResults) {
    const qNum = r.questionIndex + 1
    if (r.contentBlank) {
      issues.push({
        key: `q-${r.questionIndex}-content-blank`,
        targetId: `q-${r.questionIndex}-content`,
        message: `Câu ${qNum}: chưa nhập nội dung câu hỏi`,
      })
    } else if (r.contentTooLong) {
      issues.push({
        key: `q-${r.questionIndex}-content-long`,
        targetId: `q-${r.questionIndex}-content`,
        message: `Câu ${qNum}: nội dung câu hỏi vượt quá ${MAX_QUESTION_CONTENT_LENGTH} ký tự`,
      })
    }
    if (r.invalidOptionCount) {
      issues.push({
        key: `q-${r.questionIndex}-opt-count`,
        targetId: `q-${r.questionIndex}-content`,
        message: `Câu ${qNum}: cần từ ${MIN_OPTIONS_PER_QUESTION} đến ${MAX_OPTIONS_PER_QUESTION} đáp án`,
      })
    }
    for (const oIdx of r.blankOptionIndices) {
      issues.push({
        key: `q-${r.questionIndex}-opt-${oIdx}-blank`,
        targetId: `q-${r.questionIndex}-opt-${oIdx}`,
        message: `Câu ${qNum}: đáp án ${optionLabel(oIdx)} đang để trống`,
      })
    }
    for (const oIdx of r.tooLongOptionIndices) {
      issues.push({
        key: `q-${r.questionIndex}-opt-${oIdx}-long`,
        targetId: `q-${r.questionIndex}-opt-${oIdx}`,
        message: `Câu ${qNum}: đáp án ${optionLabel(oIdx)} vượt quá ${MAX_OPTION_CONTENT_LENGTH} ký tự`,
      })
    }
    for (const group of r.duplicateGroups) {
      const msg = formatDuplicateGroupMessage(group, r.questionIndex)
      duplicateSummaries.push(msg)
      issues.push({
        key: `q-${r.questionIndex}-dup-${group.optionIndices.join('-')}`,
        targetId: `q-${r.questionIndex}-opt-${group.optionIndices[1] ?? group.optionIndices[0]}`,
        message: msg,
      })
    }
    if (r.invalidCorrectCount) {
      issues.push({
        key: `q-${r.questionIndex}-correct`,
        targetId: `q-${r.questionIndex}-opt-0`,
        message: `Câu ${qNum}: cần chọn đúng 1 đáp án đúng`,
      })
    }
    if (r.explanationTooLong) {
      issues.push({
        key: `q-${r.questionIndex}-explanation-long`,
        targetId: `q-${r.questionIndex}-explanation`,
        message: `Câu ${qNum}: giải thích vượt quá ${MAX_EXPLANATION_LENGTH} ký tự`,
      })
    }
  }

  const isValid = issues.length === 0

  return {
    titleError,
    hasQuestions,
    tooManyQuestions,
    validTitle: titleError === null,
    validOptionCount: invalidOptionCounts.length === 0,
    invalidOptionCounts,
    exactlyOneCorrect: invalidCorrectAnswers.length === 0,
    invalidCorrectAnswers,
    allContentFilled: emptyContents.length === 0,
    emptyContents,
    allLengthsValid: lengthErrorQuestions.length === 0,
    lengthErrorQuestions,
    noDuplicateOptions: duplicateOptionQuestions.length === 0,
    duplicateOptionQuestions,
    duplicateSummaries,
    questions: questionResults,
    issues,
    isValid,
    canPublish: isValid,
  }
}

/**
 * Tạo tiêu đề bản sao không vượt quá 200 Unicode code points (kể cả hậu tố),
 * cắt theo code point để không làm vỡ emoji (surrogate pairs).
 */
export function buildCloneTitle(
  originalTitle: string,
  suffix = ' (bản sao)',
  maxCodePoints = MAX_QUIZ_TITLE_LENGTH,
): string {
  const base = trimUnicodeWhitespace(originalTitle) || 'Quiz'
  const baseChars = Array.from(base)
  const suffixChars = Array.from(suffix)
  if (baseChars.length + suffixChars.length <= maxCodePoints) {
    return `${base}${suffix}`
  }
  const budget = Math.max(0, maxCodePoints - suffixChars.length)
  if (budget === 0) {
    return suffixChars.slice(0, maxCodePoints).join('')
  }
  const truncated = trimUnicodeWhitespace(baseChars.slice(0, budget).join(''))
  if (!truncated) {
    return baseChars.slice(0, budget).join('') + suffix
  }
  return `${truncated}${suffix}`
}
