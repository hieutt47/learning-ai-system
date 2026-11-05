import { isRouteErrorResponse, useRouteError } from 'react-router'
import { ErrorState, NotFoundState } from './PageState'

/** Trang lỗi của router: route không tồn tại hoặc trang ném lỗi khi render. */
export function RouteErrorPage() {
  const error = useRouteError()
  if (isRouteErrorResponse(error) && error.status === 404) {
    return <NotFoundState />
  }
  return <ErrorState error={error} onRetry={() => window.location.reload()} />
}
