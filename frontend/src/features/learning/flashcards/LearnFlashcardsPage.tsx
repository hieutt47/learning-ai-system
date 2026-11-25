import { useCallback, useEffect, useMemo, useState } from 'react'
import { Button, Card, Progress, Space, Tag, Typography } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { useParams } from 'react-router'
import { EmptyState, ErrorState, LoadingState } from '../../../shared/components/PageState'
import { Icon } from '../../../shared/components/Icon'
import type { Flashcard } from '../../teaching/ai/api'
import { getPublishedCards } from './api'
import './flashcard.css'

export function LearnFlashcardsPage() {
  const { lessonId = '' } = useParams()
  return <LearnFlashcardsContent key={lessonId} lessonId={lessonId} />
}

function LearnFlashcardsContent({ lessonId }: { lessonId: string }) {
  const [index, setIndex] = useState(0)
  const [flipped, setFlipped] = useState(false)
  const [knownIds, setKnownIds] = useState<Set<string>>(() => new Set())
  const [reviewIds, setReviewIds] = useState<Set<string>>(() => new Set())
  const [isCompleted, setIsCompleted] = useState(false)
  const [reviewOnlyList, setReviewOnlyList] = useState<Flashcard[] | null>(null)

  const query = useQuery({
    queryKey: ['published-flashcards', lessonId],
    enabled: Boolean(lessonId),
    queryFn: async () => {
      const first = await getPublishedCards(lessonId, 1, 100)
      const total = first?.total ?? 0
      const pageCount = Math.ceil(total / 100)
      const rest = await Promise.all(
        Array.from({ length: Math.max(0, pageCount - 1) }, (_, offset) =>
          getPublishedCards(lessonId, offset + 2, 100),
        ),
      )
      return [first, ...rest]
        .flatMap(page => page?.items ?? [])
        .filter(card => card.status === 'PUBLISHED')
    },
  })

  const allCards = useMemo(() => query.data ?? [], [query.data])
  const activeCards = reviewOnlyList ?? allCards
  const currentIndex = Math.min(index, Math.max(0, activeCards.length - 1))
  const card = activeCards[currentIndex]

  const flip = useCallback(() => setFlipped(value => !value), [])

  const move = useCallback((direction: number) => {
    setIndex(value => Math.max(0, Math.min(activeCards.length - 1, value + direction)))
    setFlipped(false)
  }, [activeCards.length])

  const markCard = useCallback((status: 'known' | 'review') => {
    if (!card) return
    const id = card.id
    if (status === 'known') {
      setKnownIds(prev => new Set(prev).add(id))
      setReviewIds(prev => {
        const next = new Set(prev)
        next.delete(id)
        return next
      })
    } else {
      setReviewIds(prev => new Set(prev).add(id))
      setKnownIds(prev => {
        const next = new Set(prev)
        next.delete(id)
        return next
      })
    }

    if (currentIndex >= activeCards.length - 1) {
      setIsCompleted(true)
    } else {
      setIndex(value => value + 1)
      setFlipped(false)
    }
  }, [activeCards.length, card, currentIndex])

  const restartAll = () => {
    setReviewOnlyList(null)
    setKnownIds(new Set())
    setReviewIds(new Set())
    setIndex(0)
    setFlipped(false)
    setIsCompleted(false)
  }

  const startReviewMissed = () => {
    const missed = allCards.filter(c => reviewIds.has(c.id))
    if (!missed.length) return
    setReviewOnlyList(missed)
    setIndex(0)
    setFlipped(false)
    setIsCompleted(false)
  }

  useEffect(() => {
    if (isCompleted || !card) return
    const onKeyDown = (event: KeyboardEvent) => {
      if (
        event.target instanceof HTMLElement
        && event.target.closest('input, textarea, select, [contenteditable="true"]')
      ) return

      if (event.code === 'Space') {
        event.preventDefault()
        if (event.repeat) return
        flip()
        return
      }

      if (flipped) {
        if (event.key === '1') {
          event.preventDefault()
          markCard('review')
          return
        }
        if (event.key === '2') {
          event.preventDefault()
          markCard('known')
          return
        }
      }

      if (event.key === 'ArrowLeft') {
        event.preventDefault()
        move(-1)
      }
      if (event.key === 'ArrowRight') {
        event.preventDefault()
        move(1)
      }
    }

    window.addEventListener('keydown', onKeyDown)
    return () => {
      window.removeEventListener('keydown', onKeyDown)
    }
  }, [card, flip, flipped, isCompleted, markCard, move])

  if (query.isPending) return <LoadingState />
  if (query.isError) {
    return <ErrorState error={query.error} onRetry={() => void query.refetch()} />
  }
  if (!allCards.length) {
    return <EmptyState description="Giảng viên chưa xuất bản flashcard cho bài này" />
  }

  if (isCompleted) {
    const totalCount = activeCards.length
    const rememberedCount = activeCards.filter(c => knownIds.has(c.id)).length
    const missedCount = totalCount - rememberedCount
    const percent = Math.round((rememberedCount / (totalCount || 1)) * 100)

    return (
      <Card style={{ textAlign: 'center', padding: '24px 16px' }}>
        <Space direction="vertical" size="large" style={{ width: '100%', maxWidth: 500, margin: '0 auto' }}>
          <div style={{ fontSize: 56, color: 'var(--primary)' }}>
            <Icon name="checkc" size={56} />
          </div>
          <div>
            <Typography.Title level={3} style={{ marginBottom: 8 }}>
              {reviewOnlyList ? 'Hoàn thành lượt ôn tập!' : 'Hoàn thành lượt học flashcard!'}
            </Typography.Title>
            <Typography.Text type="secondary">
              Bạn đã ôn tập xong tất cả {totalCount} thẻ trong đợt này.
            </Typography.Text>
          </div>

          <Card style={{ background: 'var(--surface)' }}>
            <Space size="large" style={{ justifyContent: 'space-around', width: '100%' }}>
              <div>
                <Typography.Title level={2} style={{ color: 'var(--success)', margin: 0 }}>
                  {rememberedCount}
                </Typography.Title>
                <Typography.Text type="secondary">Đã nhớ</Typography.Text>
              </div>
              <div>
                <Typography.Title level={2} style={{ color: 'var(--warning)', margin: 0 }}>
                  {missedCount}
                </Typography.Title>
                <Typography.Text type="secondary">Cần ôn lại</Typography.Text>
              </div>
              <div>
                <Typography.Title level={2} style={{ color: 'var(--primary)', margin: 0 }}>
                  {percent}%
                </Typography.Title>
                <Typography.Text type="secondary">Ghi nhớ</Typography.Text>
              </div>
            </Space>
          </Card>

          <Space wrap style={{ justifyContent: 'center', marginTop: 8 }}>
            {reviewIds.size > 0 && (
              <Button
                type="primary"
                icon={<Icon name="refresh" />}
                onClick={startReviewMissed}
              >
                Ôn lại {reviewIds.size} thẻ chưa nhớ
              </Button>
            )}
            <Button
              onClick={restartAll}
              icon={<Icon name="refresh" />}
            >
              Học lại toàn bộ ({allCards.length} thẻ)
            </Button>
          </Space>
        </Space>
      </Card>
    )
  }

  const rememberedInActive = activeCards.filter(c => knownIds.has(c.id)).length
  const progressPercent = Math.round(((currentIndex + 1) / activeCards.length) * 100)

  return (
    <Card>
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        <Space style={{ width: '100%', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8 }}>
          <Space>
            <Typography.Text strong>
              Thẻ {currentIndex + 1} / {activeCards.length}
            </Typography.Text>
            {reviewOnlyList && (
              <Tag color="warning">Đang ôn lại thẻ chưa nhớ</Tag>
            )}
          </Space>
          <Space>
            <Tag color="success">Đã nhớ: {rememberedInActive}</Tag>
            <Tag color="warning">Cần ôn lại: {reviewIds.size}</Tag>
            <Progress
              percent={progressPercent}
              showInfo={false}
              style={{ width: 120, margin: 0 }}
            />
          </Space>
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
                Nhấn Space để lật đáp án
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
                Tự đánh giá ghi nhớ bên dưới hoặc nhấn Space để lật lại
              </span>
            </span>
          </span>
        </button>

        {flipped ? (
          <Space direction="vertical" align="center" style={{ width: '100%' }}>
            <Typography.Text type="secondary" style={{ textAlign: 'center' }}>
              Bạn có nhớ đáp án này không?
            </Typography.Text>
            <Space wrap size="middle" style={{ justifyContent: 'center' }}>
              <Button
                danger
                size="large"
                icon={<Icon name="x" />}
                onClick={() => markCard('review')}
                style={{ minWidth: 140 }}
              >
                Chưa nhớ
              </Button>
              <Button
                type="primary"
                size="large"
                icon={<Icon name="checkc" />}
                onClick={() => markCard('known')}
                style={{
                  minWidth: 140,
                  background: 'var(--success)',
                  borderColor: 'var(--success)',
                  color: 'var(--on-primary)',
                }}
              >
                Đã nhớ
              </Button>
            </Space>
            <Button type="text" size="small" onClick={flip}>
              Lật lại câu hỏi
            </Button>
          </Space>
        ) : (
          <Space style={{ width: '100%', justifyContent: 'center' }}>
            <Button
              disabled={currentIndex === 0}
              icon={<Icon name="left" />}
              onClick={() => move(-1)}
            >
              Trước
            </Button>
            <Button type="primary" onClick={flip}>
              Lật xem đáp án
            </Button>
            <Button
              disabled={currentIndex === activeCards.length - 1}
              onClick={() => move(1)}
            >
              Sau <Icon name="right" />
            </Button>
          </Space>
        )}

        <Typography.Text type="secondary" style={{ textAlign: 'center', display: 'block' }}>
          {flipped ? 'Phím 1: Chưa nhớ · Phím 2: Đã nhớ · Space: Lật lại' : 'Space: Lật thẻ · ← →: Chuyển thẻ'}
        </Typography.Text>
      </Space>
    </Card>
  )
}
