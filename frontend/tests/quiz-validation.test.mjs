import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createServer } from 'vite'
import { fileURLToPath } from 'node:url'

async function loadModules() {
  const server = await createServer({
    configFile: false,
    root: fileURLToPath(new URL('..', import.meta.url)),
    server: { middlewareMode: true, ws: false },
    appType: 'custom',
  })
  const quizValidation = await server.ssrLoadModule('/src/features/teaching/quizzes/quizValidation.ts')
  const aiValidation = await server.ssrLoadModule('/src/features/teaching/ai/validation.ts')
  const saveDraft = await server.ssrLoadModule('/src/features/teaching/quizzes/saveQuizDraft.ts')
  return { server, quizValidation, aiValidation, saveDraft }
}

test('duplicate option normalization matches backend NFC + Unicode White_Space + root lowercase parity', async () => {
  const { server, quizValidation } = await loadModules()
  try {
    const { findDuplicateOptionGroups, normalizeOptionContent, formatDuplicateGroupMessage } = quizValidation

    // Exact, case-only, repeated spaces, tab, newline, NBSP (U+00A0), NEL (U+0085), ideographic space (U+3000), NFD
    const variants = [
      'Hà Nội',
      'HÀ NỘI',
      '   hà    nội   ',
      'hà\t\nnội',
      'hà\u00A0nội',
      'hà\u0085nội',
      'hà\u3000nội',
      'Ha\u0300 No\u0323\u0302i', // NFD decomposed
    ]
    const expected = normalizeOptionContent('Hà Nội')
    for (const variant of variants) {
      assert.equal(normalizeOptionContent(variant), expected, `variant failed parity: ${JSON.stringify(variant)}`)
    }

    // NFC (not NFKC): superscript 2 vs '2' and fi ligature vs 'fi' remain distinct
    assert.notEqual(normalizeOptionContent('2'), normalizeOptionContent('²'))
    assert.notEqual(normalizeOptionContent('fi'), normalizeOptionContent('\uFB01'))

    // Triple duplicate returns one group with A, B, C
    const triple = findDuplicateOptionGroups([
      { content: 'Java' },
      { content: '  java ' },
      { content: 'JAVA\u00A0' },
      { content: 'Python' },
    ])
    assert.equal(triple.length, 1)
    assert.deepEqual(triple[0].optionIndices, [0, 1, 2])
    assert.deepEqual(triple[0].labels, ['A', 'B', 'C'])
    assert.equal(formatDuplicateGroupMessage(triple[0], 1), 'Câu 2: đáp án A, B, C trùng nội dung')

    // Distinct options produce no duplicate groups
    assert.deepEqual(
      findDuplicateOptionGroups([{ content: '2' }, { content: '²' }, { content: 'Ba' }]),
      [],
    )

    // Blank / Unicode-whitespace-only options are ignored by duplicate detector (handled by blank rule)
    assert.deepEqual(
      findDuplicateOptionGroups([{ content: '' }, { content: '   ' }, { content: '\u00A0\u0085' }]),
      [],
    )
  } finally {
    await server.close()
  }
})

test('validateQuizDraft catches blank/overlong title, question 2 duplicate, allows duplicates across questions, and clears on fix', async () => {
  const { server, quizValidation } = await loadModules()
  try {
    const { validateQuizDraft, codePointLength } = quizValidation

    // Blank title rejected
    const blankTitle = validateQuizDraft(' \u00A0 ', [
      {
        content: 'Câu hỏi 1',
        options: [
          { content: 'A', isCorrect: true },
          { content: 'B', isCorrect: false },
        ],
      },
    ])
    assert.equal(blankTitle.validTitle, false)
    assert.equal(blankTitle.canPublish, false)

    // 200 emoji code points valid; 201 emoji code points invalid
    const emoji200 = '🎯'.repeat(200)
    const emoji201 = '🎯'.repeat(201)
    assert.equal(codePointLength(emoji200), 200)
    assert.equal(validateQuizDraft(emoji200, [{
      content: 'Q1',
      options: [{ content: 'A', isCorrect: true }, { content: 'B', isCorrect: false }],
    }]).validTitle, true)
    assert.equal(validateQuizDraft(emoji201, [{
      content: 'Q1',
      options: [{ content: 'A', isCorrect: true }, { content: 'B', isCorrect: false }],
    }]).validTitle, false)

    // Duplicate in question 2 caught; same option text across question 1 and question 2 allowed
    const q1 = {
      content: 'Câu 1',
      options: [
        { content: 'Đúng', isCorrect: true },
        { content: 'Sai', isCorrect: false },
      ],
    }
    const q2Dup = {
      content: 'Câu 2',
      options: [
        { content: 'Đúng', isCorrect: true },
        { content: 'Khác', isCorrect: false },
        { content: '  đúng\u00A0', isCorrect: false },
      ],
    }
    const dupResult = validateQuizDraft('Quiz kiểm tra', [q1, q2Dup])
    assert.equal(dupResult.isValid, false)
    assert.equal(dupResult.noDuplicateOptions, false)
    assert.deepEqual(dupResult.duplicateOptionQuestions, [2])
    assert.deepEqual(dupResult.duplicateSummaries, ['Câu 2: đáp án A, C trùng nội dung'])

    // Fixing Q2 option C makes draft valid (even though Q1 and Q2 both have "Đúng")
    const q2Fixed = {
      content: 'Câu 2',
      options: [
        { content: 'Đúng', isCorrect: true },
        { content: 'Khác', isCorrect: false },
        { content: 'Chưa rõ', isCorrect: false },
      ],
    }
    const fixedResult = validateQuizDraft('Quiz kiểm tra', [q1, q2Fixed])
    assert.equal(fixedResult.isValid, true)
    assert.equal(fixedResult.canPublish, true)
  } finally {
    await server.close()
  }
})

test('buildCloneTitle respects 200 code-point limit without splitting UTF-16 surrogate pairs', async () => {
  const { server, quizValidation } = await loadModules()
  try {
    const { buildCloneTitle, codePointLength } = quizValidation
    const hasBrokenSurrogate = (str) => {
      for (let i = 0; i < str.length; i++) {
        const code = str.charCodeAt(i)
        if (code >= 0xD800 && code <= 0xDBFF) {
          const next = str.charCodeAt(i + 1)
          if (!(next >= 0xDC00 && next <= 0xDFFF)) return true
          i++
        } else if (code >= 0xDC00 && code <= 0xDFFF) {
          return true
        }
      }
      return false
    }

    for (const raw of ['Quiz ngắn', 'A'.repeat(195), 'B'.repeat(200), '🚀'.repeat(195), `${'Đề thi '.repeat(30)}🎉🔥`]) {
      const cloned = buildCloneTitle(raw)
      assert.ok(codePointLength(cloned) <= 200, `clone title exceeded 200 code points: ${codePointLength(cloned)}`)
      assert.ok(cloned.endsWith(' (bản sao)'), `missing suffix: ${cloned}`)
      assert.equal(hasBrokenSurrogate(cloned), false, 'clone title split an emoji surrogate pair')
    }
  } finally {
    await server.close()
  }
})

test('preflightAiQuizDraft blocks duplicate options and whitespace-only title before any create request', async () => {
  const { server, aiValidation } = await loadModules()
  try {
    const { preflightAiQuizDraft } = aiValidation
    const res = preflightAiQuizDraft('   ', [
      {
        content: 'Câu 1 AI',
        explanation: 'Giải thích',
        options: [
          { content: 'Hà Nội', is_correct: true },
          { content: 'Ha\u0300\u00A0No\u0323\u0302i', is_correct: false },
        ],
      },
    ])
    assert.equal(res.isValid, false)
    assert.ok(res.titleError)
    assert.ok(res.summaryErrors.some((e) => e.message === 'Câu 1: đáp án A, B trùng nội dung'))

    const valid = preflightAiQuizDraft('', [
      {
        content: 'Câu 1 AI',
        explanation: 'Giải thích',
        options: [
          { content: 'Hà Nội', is_correct: true },
          { content: 'Đà Nẵng', is_correct: false },
        ],
      },
    ])
    assert.equal(valid.isValid, true)
  } finally {
    await server.close()
  }
})

test('createDraftSaver clears pending snapshot on 400/409 so edited draft saves with fresh request_id', async () => {
  const { server, saveDraft } = await loadModules()
  const originalFetch = globalThis.fetch
  const keys = []
  let attempt = 0
  try {
    globalThis.fetch = async (_url, options) => {
      const body = JSON.parse(options.body)
      keys.push(body.request_id)
      attempt++
      if (attempt === 1) {
        return new Response(JSON.stringify({ code: 'VALIDATION_ERROR', message: 'Request body is invalid' }), {
          status: 400,
          headers: { 'Content-Type': 'application/json' },
        })
      }
      if (attempt === 2) {
        return new Response(JSON.stringify({ code: 'INVALID_STATE', message: 'Conflict' }), {
          status: 409,
          headers: { 'Content-Type': 'application/json' },
        })
      }
      return new Response(JSON.stringify({
        id: '30', lesson_id: '10', title: body.title, status: 'DRAFT', questions: [],
      }), { status: 200, headers: { 'Content-Type': 'application/json' } })
    }
    const save = saveDraft.createDraftSaver()
    const q = [{ content: 'Q1', orderIndex: 0, options: [{ content: 'A', isCorrect: true }, { content: 'B', isCorrect: false }] }]
    await assert.rejects(save('30', 'Draft 1', q))
    await assert.rejects(save('30', 'Draft 2', q))
    const ok = await save('30', 'Draft 3', q)
    assert.equal(ok.title, 'Draft 3')
    assert.equal(new Set(keys).size, 3, '400/409 rejections must clear pending snapshot so next save gets a fresh request_id')
  } finally {
    globalThis.fetch = originalFetch
    await server.close()
  }
})
