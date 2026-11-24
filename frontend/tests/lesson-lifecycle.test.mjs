import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createServer } from 'vite'
import { fileURLToPath } from 'node:url'

const root = fileURLToPath(new URL('..', import.meta.url))
const lesson = (status) => ({ id: '10', courseId: '20', title: 'Bài cũ', status })

test('lesson lifecycle keeps a visible lesson in every published course', async () => {
  const server = await createServer({ configFile: false, root,
    server: { middlewareMode: true, ws: false }, appType: 'custom' })
  try {
    const { lessonActionDisabledReason } = await server.ssrLoadModule(
      '/src/features/teaching/lessons/lesson-lifecycle.ts')

    assert.match(lessonActionDisabledReason(lesson('PUBLISHED'), 'Published', 1), /ít nhất một bài/)
    assert.equal(lessonActionDisabledReason(lesson('PUBLISHED'), 'Published', 2), null)
    assert.equal(lessonActionDisabledReason(lesson('PUBLISHED'), 'Draft', 1), null)
    assert.match(lessonActionDisabledReason(lesson('DRAFT'), 'Published', 1, true), /Lưu nội dung/)
    assert.equal(lessonActionDisabledReason(lesson('DRAFT'), 'Published', 1), null)
    assert.match(lessonActionDisabledReason(lesson('PUBLISHED'), 'Archived', 2), /chỉ có thể xem/)
    assert.match(lessonActionDisabledReason(lesson('ARCHIVED'), 'Published', 2), /không thể xuất bản lại/)
  } finally {
    await server.close()
  }
})

test('teacher status change sends the backend contract and reads the resulting status', async () => {
  const originalFetch = globalThis.fetch
  const server = await createServer({ configFile: false, root,
    server: { middlewareMode: true, ws: false }, appType: 'custom' })
  try {
    globalThis.fetch = async (url, options) => {
      assert.equal(url, '/api/v1/lessons/10/status')
      assert.equal(options.method, 'PATCH')
      assert.deepEqual(JSON.parse(options.body), { status: 'ARCHIVED' })
      return new Response(JSON.stringify({
        id: '10', course_id: '20', title: 'Bài cũ', status: 'ARCHIVED', video_url: '',
        order_index: 1, document_count: 0, quiz_count: 1, flashcard_count: 0,
        can_delete: false, delete_blocked_reason: 'Bài học đã có lịch sử làm quiz.',
      }), { headers: { 'Content-Type': 'application/json' } })
    }
    const { changeLessonStatus } = await server.ssrLoadModule(
      '/src/features/teaching/lessons/lesson.api.ts')
    const archived = await changeLessonStatus('10', 'ARCHIVED')
    assert.equal(archived.status, 'ARCHIVED')
    assert.equal(archived.canDelete, false)
  } finally {
    globalThis.fetch = originalFetch
    await server.close()
  }
})
