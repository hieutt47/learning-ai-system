import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createServer } from 'vite'
import { fileURLToPath } from 'node:url'

test('enrolled course retains history while learning is unavailable', async () => {
  const originalFetch = globalThis.fetch
  const server = await createServer({ configFile: false, root: fileURLToPath(new URL('..', import.meta.url)),
    server: { middlewareMode: true, ws: false }, appType: 'custom' })
  try {
    globalThis.fetch = async (url) => {
      assert.equal(url, '/api/v1/users/me/courses?page=1&page_size=100')
      return new Response(JSON.stringify({ items: [{
        id: '20', title: 'Khóa đã gỡ xuất bản', description: '',
        category: { id: '1', name: 'Java' }, teacher: { id: '2', full_name: 'GV' },
        price: '0', currency: 'VND', total_students: 1, lesson_count: 2,
        enrolled_at: '2026-11-24T00:00:00Z', learning_available: false,
      }], page: 1, page_size: 100, total: 1 }),
      { headers: { 'Content-Type': 'application/json' } })
    }
    const { getMyCourses } = await server.ssrLoadModule('/src/features/marketplace/course.api.ts')
    const page = await getMyCourses({ page: 1, page_size: 100 })
    assert.equal(page.items[0].learningAvailable, false)
    assert.equal(page.items[0].enrolledAt, '2026-11-24T00:00:00Z')
  } finally {
    globalThis.fetch = originalFetch
    await server.close()
  }
})
