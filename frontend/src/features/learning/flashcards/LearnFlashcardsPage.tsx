import { useEffect, useState } from 'react'
import { Button, Card, Progress, Space, Typography } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { useParams } from 'react-router'
import { EmptyState, ErrorState, LoadingState } from '../../../shared/components/PageState'
import { Icon } from '../../../shared/components/Icon'
import { getPublishedCards } from './api'
import './flashcard.css'

export function LearnFlashcardsPage() {
  const { lessonId = '' } = useParams()
  const [index, setIndex] = useState(0)
  const [flipped, setFlipped] = useState(false)
  const query = useQuery({
    queryKey: ['published-flashcards', lessonId],
    queryFn: async () => {
      const first = await getPublishedCards(lessonId, 1, 100)
      const pageCount = Math.ceil(first.total / 100)
      const rest = await Promise.all(
        Array.from({ length: Math.max(0, pageCount - 1) }, (_, offset) =>
          getPublishedCards(lessonId, offset + 2, 100),
        ),
      )
      return [first, ...rest]
        .flatMap(page => page.items)
        .filter(card => card.status === 'PUBLISHED')
    },
  })
  const cards = query.data ?? []
  const currentIndex = Math.min(index, Math.max(0, cards.length - 1))
  const move = (direction: number) => {
    setIndex(value => Math.max(0, Math.min(cards.length - 1, value + direction)))
    setFlipped(false)
  }

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (
        event.target instanceof HTMLElement
        && event.target.closest('input, textarea, select, [contenteditable="true"]')
      ) return
      if (event.key === 'ArrowLeft') {
        event.preventDefault()
        setIndex(value => Math.max(0, value - 1))
        setFlipped(false)
      }
      if (event.key === 'ArrowRight') {
        event.preventDefault()
        setIndex(value => Math.min(cards.length - 1, value + 1))
        setFlipped(false)
      }
      if (event.code === 'Space') {
        // preventDefault chặn nút đang focus tự click thêm một lần.
        event.preventDefault()
        if (event.repeat) return
        setFlipped(value => !value)
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => {
      window.removeEventListener('keydown', onKeyDown)
    }
  }, [cards.length])

  if (query.isPending) return <LoadingState />
  if (query.isError) {
    return <ErrorState error={query.error} onRetry={() => void query.refetch()} />
  }
  if (!cards.length) {
    return <EmptyState description="Giảng viên chưa xuất bản flashcard cho bài này" />
  }
  const card = cards[currentIndex]
  const flip = () => setFlipped(value => !value)

  return (
    <Card>
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        <Space style={{ width: '100%', justifyContent: 'space-between' }}>
          <Typography.Text strong>
            {currentIndex + 1} / {cards.length}
          </Typography.Text>
          <Progress
            percent={Math.round((currentIndex + 1) / cards.length * 100)}
            showInfo={false}
            style={{ width: 160 }}
          />
        </Space>
        <button
          type="button"
          className="learn-flip-card"
          aria-label={`Thẻ ${currentIndex + 1}, nhấn để lật`}
          aria-pressed={flipped}
          onClick={flip}
        >
          <span className={`learn-flip-inner${flipped ? ' is-flipped' : ''}`}>
            <span className="learn-flip-face learn-flip-front">
              <span className="learn-flip-label">
                Câu hỏi
              </span>
              <span className="learn-flip-text">
                {card.front_text}
              </span>
              <span className="learn-flip-hint">
                Nhấn Space để lật thẻ
              </span>
            </span>
            <span className="learn-flip-face learn-flip-back">
              <span className="learn-flip-label">
                Đáp án
              </span>
              <span className="learn-flip-text">
                {card.back_text}
              </span>
              <span className="learn-flip-hint">
                Nhấn Space để lật lại
              </span>
            </span>
          </span>
        </button>
        <Space style={{ width: '100%', justifyContent: 'center' }}>
          <Button
            disabled={currentIndex === 0}
            icon={<Icon name="left" />}
            onClick={() => move(-1)}
          >
            Trước
          </Button>
          <Button type="primary" onClick={flip}>
            Lật thẻ
          </Button>
          <Button
            disabled={currentIndex === cards.length - 1}
            onClick={() => move(1)}
          >
            Sau <Icon name="right" />
          </Button>
        </Space>
        <Typography.Text type="secondary" style={{ textAlign: 'center' }}>
          ← → chuyển thẻ · Space lật thẻ
        </Typography.Text>
      </Space>
    </Card>
  )
}
