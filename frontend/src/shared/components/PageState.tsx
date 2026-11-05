import { Button, Empty, Result, Spin } from 'antd'
import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { isApiError } from '../api/client'
import { paths } from '../routing/paths'

/**
 * Bốn trạng thái mọi màn hình phải có (design-brief mục 2.4 và mục 8):
 * loading, empty, error, forbidden (+ not found).
 */

export function LoadingState({ tip = 'Đang tải…' }: { tip?: string }) {
  return (
    <div style={{ padding: 48, textAlign: 'center' }}>
      <Spin size="large" />
      <div style={{ marginTop: 12, color: 'rgba(0,0,0,0.45)' }}>{tip}</div>
    </div>
  )
}

export function EmptyState({ description, action }: { description: ReactNode; action?: ReactNode }) {
  return (
    <Empty style={{ padding: 48 }} description={description}>
      {action}
    </Empty>
  )
}

export function ForbiddenState({ message, action }: { message?: string; action?: ReactNode }) {
  return (
    <Result
      status="403"
      title="Bạn không có quyền truy cập"
      subTitle={message ?? 'Liên hệ giảng viên hoặc quản trị viên nếu bạn nghĩ đây là nhầm lẫn.'}
      extra={action}
    />
  )
}

export function NotFoundState() {
  return (
    <Result
      status="404"
      title="Không tìm thấy"
      subTitle="Trang hoặc dữ liệu bạn tìm không tồn tại."
      extra={
        <Link to={paths.home}>
          <Button type="primary">Về trang chủ</Button>
        </Link>
      }
    />
  )
}

/** Hiển thị lỗi từ API theo mã: 403 → không có quyền, 404 → không tìm thấy, còn lại → lỗi + thử lại. */
export function ErrorState({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  if (isApiError(error)) {
    if (error.status === 403) {
      return <ForbiddenState message={error.message} />
    }
    if (error.status === 404) {
      return <NotFoundState />
    }
  }
  const message = error instanceof Error ? error.message : 'Đã có lỗi xảy ra.'
  return (
    <Result
      status="error"
      title="Không tải được dữ liệu"
      subTitle={message}
      extra={
        onRetry && (
          <Button type="primary" onClick={onRetry}>
            Thử lại
          </Button>
        )
      }
    />
  )
}
