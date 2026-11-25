import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createServer } from 'vite'
import { fileURLToPath } from 'node:url'

const root = fileURLToPath(new URL('..', import.meta.url))

test('statistics formatters and quiz filter enforce published lesson & quiz rules', async () => {
  const server = await createServer({
    configFile: false,
    root,
    server: { middlewareMode: true, ws: false },
    appType: 'custom',
  })
  try {
    const mod = await server.ssrLoadModule('/src/features/teaching/statistics/statistics.format.ts')

    assert.equal(mod.formatScore(null), 'Chưa có')
    assert.equal(mod.formatScore(85), '85')
    assert.equal(mod.formatPercent(0), '0%')
    assert.equal(mod.quizStatusMeta('PUBLISHED').label, 'Đang xuất bản')
    assert.equal(mod.quizStatusMeta('CLOSED').label, 'Đã đóng')

    const quizzes = [
      { quizId: '1', status: 'PUBLISHED', lessonStatus: 'PUBLISHED' },
      { quizId: '2', status: 'CLOSED', lessonStatus: 'PUBLISHED' },
      { quizId: '3', status: 'PUBLISHED', lessonStatus: 'ARCHIVED' },
    ]
    assert.deepEqual(
      mod.filterCourseQuizzes(quizzes, 'active_published').map((q) => q.quizId),
      ['1'],
    )
    assert.deepEqual(
      mod.filterCourseQuizzes(quizzes, 'all').map((q) => q.quizId),
      ['1', '2', '3'],
    )
  } finally {
    await server.close()
  }
})

test('statistics API adapter maps course and quiz statistics wire contracts cleanly', async () => {
  const originalFetch = globalThis.fetch
  const server = await createServer({
    configFile: false,
    root,
    server: { middlewareMode: true, ws: false },
    appType: 'custom',
  })
  try {
    globalThis.fetch = async (url) => {
      if (url === '/api/v1/statistics/courses/2') {
        return new Response(
          JSON.stringify({
            course_id: '2',
            course_title: 'Java cơ bản',
            enrolled_students: 4,
            lesson_count: 3,
            published_lesson_count: 2,
            quiz_count: 1,
            submitted_attempts: 5,
            completed_students: 2,
            completion_rate: 50,
            average_score: 72.5,
            quizzes: [
              {
                quiz_id: '30',
                quiz_title: 'Quiz JVM',
                lesson_id: '10',
                lesson_title: 'Bài 1',
                lesson_order_index: 1,
                lesson_status: 'PUBLISHED',
                status: 'PUBLISHED',
                question_count: 5,
                submitted_attempts: 5,
                unique_students: 2,
                average_score: 72.5,
                latest_submitted_at: '2026-11-25T10:00:00Z',
              },
            ],
            generated_at: '2026-11-25T10:30:00Z',
          }),
          { headers: { 'Content-Type': 'application/json' } },
        )
      }
      if (url === '/api/v1/statistics/quizzes/30') {
        return new Response(
          JSON.stringify({
            quiz_id: '30',
            quiz_title: 'Quiz JVM',
            quiz_status: 'PUBLISHED',
            course_id: '2',
            course_title: 'Java cơ bản',
            lesson_id: '10',
            lesson_title: 'Bài 1',
            lesson_status: 'PUBLISHED',
            question_count: 5,
            submitted_attempts: 3,
            unique_students: 2,
            average_score: 66.67,
            highest_score: 100,
            lowest_score: 40,
            best_scores: [
              {
                student_id: '4',
                student_name: 'Nguyễn Văn A',
                score: 100,
                attempt_count: 2,
                latest_submitted_at: '2026-11-25T10:00:00Z',
              },
            ],
            hardest_questions: [
              {
                question_id: '101',
                order_index: 0,
                question_content: 'JVM là gì?',
                wrong_count: 2,
                wrong_rate: 66.67,
              },
            ],
            generated_at: '2026-11-25T10:30:00Z',
          }),
          { headers: { 'Content-Type': 'application/json' } },
        )
      }
      throw new Error(`Unexpected URL: ${url}`)
    }

    const { getCourseStatistics, getQuizStatistics } = await server.ssrLoadModule(
      '/src/features/teaching/statistics/statistics.api.ts',
    )

    const course = await getCourseStatistics('2')
    assert.equal(course.courseTitle, 'Java cơ bản')
    assert.equal(course.completionRate, 50)
    assert.equal(course.quizzes.length, 1)
    assert.equal(course.quizzes[0].quizId, '30')

    const quiz = await getQuizStatistics('30')
    assert.equal(quiz.quizTitle, 'Quiz JVM')
    assert.equal(quiz.highestScore, 100)
    assert.equal(quiz.bestScores[0].studentName, 'Nguyễn Văn A')
    assert.equal(quiz.hardestQuestions[0].questionContent, 'JVM là gì?')
    assert.equal(quiz.hardestQuestions[0].wrongRate, 66.67)
  } finally {
    globalThis.fetch = originalFetch
    await server.close()
  }
})
