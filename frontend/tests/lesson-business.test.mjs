import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createServer } from 'vite'
import { fileURLToPath } from 'node:url'

const root = fileURLToPath(new URL('..', import.meta.url))

test('YouTube URL validation and embed extraction match backend policy', async () => {
  const server = await createServer({
    configFile: false,
    root,
    server: { middlewareMode: true, ws: false },
    appType: 'custom',
  })
  try {
    const yt = await server.ssrLoadModule('/src/features/teaching/lessons/youtube-url.ts')

    assert.equal(yt.isOptionalValidYouTubeUrl(''), true)
    assert.equal(yt.isOptionalValidYouTubeUrl('   '), true)
    assert.equal(yt.isValidYouTubeUrl('https://www.youtube.com/watch?v=eIrMbAQSU34'), true)
    assert.equal(yt.isValidYouTubeUrl('https://youtu.be/eIrMbAQSU34?si=123'), true)
    assert.equal(yt.isValidYouTubeUrl('https://www.youtube-nocookie.com/embed/eIrMbAQSU34'), true)
    assert.equal(yt.isValidYouTubeUrl('https://youtube.com/shorts/secret-a1/'), true)
    assert.equal(
      yt.getYouTubeEmbedUrl('https://youtu.be/eIrMbAQSU34?si=abc'),
      'https://www.youtube-nocookie.com/embed/eIrMbAQSU34',
    )

    assert.equal(yt.isValidYouTubeUrl('https://www.youtube.com/embed/'), false)
    assert.equal(yt.isValidYouTubeUrl('https://www.youtube.com/shorts/'), false)
    assert.equal(yt.isValidYouTubeUrl('https://www.youtube.com/watch?v=   '), false)
    assert.equal(yt.isValidYouTubeUrl('https://youtu.be/eIrMbAQSU34/extra'), false)
    assert.equal(yt.isValidYouTubeUrl('<iframe src="https://youtu.be/eIrMbAQSU34"></iframe>'), false)
  } finally {
    await server.close()
  }
})

test('lesson ordering prevents moving archived lessons or swapping across them', async () => {
  const server = await createServer({
    configFile: false,
    root,
    server: { middlewareMode: true, ws: false },
    appType: 'custom',
  })
  try {
    const order = await server.ssrLoadModule('/src/features/teaching/lessons/lesson-order.ts')
    const lessons = [
      { id: '1', orderIndex: 1, status: 'ARCHIVED' },
      { id: '2', orderIndex: 2, status: 'PUBLISHED' },
      { id: '3', orderIndex: 3, status: 'DRAFT' },
    ]

    assert.equal(order.canMoveLesson(lessons, '1', 'DOWN'), false)
    assert.equal(order.canMoveLesson(lessons, '2', 'UP'), false)
    assert.equal(order.canMoveLesson(lessons, '2', 'DOWN'), true)
    assert.equal(order.canMoveLesson(lessons, '3', 'UP'), true)

    const reordered = order.reorderLessons(lessons, '3', 'UP')
    assert.deepEqual(
      reordered.map((l) => [l.id, l.orderIndex]),
      [
        ['1', 1],
        ['3', 2],
        ['2', 3],
      ],
    )
  } finally {
    await server.close()
  }
})

test('lesson client validation and document formatting helpers work accurately', async () => {
  const server = await createServer({
    configFile: false,
    root,
    server: { middlewareMode: true, ws: false },
    appType: 'custom',
  })
  try {
    const val = await server.ssrLoadModule('/src/features/teaching/lessons/lesson-validation.ts')
    const fmt = await server.ssrLoadModule('/src/features/teaching/lessons/document-format.ts')

    assert.equal(val.utf8ByteLength('😀'.repeat(16384)), 65536)
    assert.ok(val.utf8ByteLength('😀'.repeat(16384)) > val.MAX_TRANSCRIPT_UTF8_BYTES)
    assert.equal(val.validateDocumentFile({ name: 'slides.pdf', size: 1024 }), null)
    assert.match(val.validateDocumentFile({ name: 'empty.txt', size: 0 }), /rỗng/)
    assert.match(
      val.validateDocumentFile({ name: 'huge.pdf', size: val.MAX_DOCUMENT_BYTES + 1 }),
      /10 MiB/,
    )
    assert.match(val.validateDocumentFile({ name: 'script.exe', size: 512 }), /định dạng/)

    assert.equal(fmt.formatFileSize(512), '512 B')
    assert.equal(fmt.formatFileSize(1536), '1,5 KB')
    assert.equal(fmt.formatFileSize(2 * 1024 * 1024), '2,00 MB')
    assert.equal(fmt.documentTypeBadge('application/pdf', 'slides.pdf').label, 'PDF')
    assert.equal(fmt.documentTypeBadge('text/plain', 'notes.txt').label, 'TXT')
    assert.equal(
      fmt.documentTypeBadge('application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'doc.docx').label,
      'DOCX',
    )
  } finally {
    await server.close()
  }
})
