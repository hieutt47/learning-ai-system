import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createServer } from 'vite'
import { fileURLToPath } from 'node:url'

test('admin lesson AI helpers parse candidate JSON safely and call lesson ai-generations endpoint', async () => {
  const original = globalThis.fetch
  const server = await createServer({
    configFile: false,
    root: fileURLToPath(new URL('..', import.meta.url)),
    server: { middlewareMode: true, ws: false },
    appType: 'custom',
  })

  try {
    const api = await server.ssrLoadModule('/src/features/admin/ai.api.ts')

    assert.equal(api.parseCandidatePreview(null), null)
    assert.equal(api.parseCandidatePreview('{broken'), null)
    const quizPreview = api.parseCandidatePreview(JSON.stringify({
      questions: [{ content: 'Q1', explanation: 'Exp', options: [{ content: 'A', is_correct: true }] }],
    }))
    assert.equal(quizPreview?.questions?.length, 1)
    assert.equal(quizPreview?.questions?.[0]?.content, 'Q1')

    const calls = []
    globalThis.fetch = async (url) => {
      calls.push(String(url))
      return new Response(JSON.stringify({ items: [], page: 1, page_size: 10, total: 0 }), {
        headers: { 'Content-Type': 'application/json' },
      })
    }

    await api.listLessonAiGenerations('901', { generation_type: 'QUIZ', status: 'SUCCEEDED', page: 1, page_size: 10 })

    assert.match(calls[0], /\/api\/v1\/lessons\/901\/ai-generations\?.*generation_type=QUIZ.*status=SUCCEEDED/)
  } finally {
    globalThis.fetch = original
    await server.close()
  }
})
