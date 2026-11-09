import { Navigate, Outlet, useLocation } from 'react-router'
import { useCurrentUser, type ViewerRole } from '../auth/currentUser'
import { restoreSession } from '../auth/session'
import { ErrorState, ForbiddenState, LoadingState } from '../components/PageState'
import { homePathFor, paths } from './paths'

export function HomeRedirect() {
  const { role, isLoading, error } = useCurrentUser()
  if (isLoading) {
    return <LoadingState tip="Đang kiểm tra phiên đăng nhập…" />
  }
  if (error) {
    return <ErrorState error={error} onRetry={() => void restoreSession()} />
  }
  return <Navigate to={homePathFor(role)} replace />
}

/**
 * Chặn trang theo vai trò (bảng ACCESS của prototype). Chỉ là chặn phía UI; backend vẫn kiểm tra quyền.
 * - allow="auth": mọi người đã đăng nhập
 * - allow=['TEACHER']...: chỉ các vai trò này
 * Khách vào trang cần đăng nhập → chuyển sang /login, giữ trang đích; sai vai trò → trang 403.
 */
export function RequireAccess({ allow }: { allow: 'auth' | Exclude<ViewerRole, 'GUEST'>[] }) {
  const { role, isLoading, error } = useCurrentUser()
  const { pathname, search } = useLocation()

  if (isLoading) {
    return <LoadingState tip="Đang kiểm tra phiên đăng nhập…" />
  }
  if (error) {
    return <ErrorState error={error} onRetry={() => void restoreSession()} />
  }
  if (role === 'GUEST') {
    return <Navigate to={`${paths.login}?redirect=${encodeURIComponent(pathname + search)}`} replace />
  }
  if (allow !== 'auth' && !allow.includes(role)) {
    return <ForbiddenState message="Trang này dành cho vai trò khác với tài khoản của bạn." />
  }
  return <Outlet />
}

function loginDestination(search: string, role: ViewerRole): string {
  const redirect = new URLSearchParams(search).get('redirect')
  if (redirect?.startsWith('/')) {
    try {
      const url = new URL(redirect, window.location.origin)
      const pathname = url.pathname.replace(/\/+$/, '').toLowerCase()
      if (url.origin === window.location.origin && pathname !== paths.login && pathname !== paths.register) {
        return url.pathname + url.search + url.hash
      }
    } catch {
      return homePathFor(role)
    }
  }
  return homePathFor(role)
}

export function GuestOnly() {
  const { role, isLoading } = useCurrentUser()
  const { search } = useLocation()
  if (isLoading) {
    return <LoadingState tip="Đang kiểm tra phiên đăng nhập…" />
  }
  return role === 'GUEST' ? <Outlet /> : <Navigate to={loginDestination(search, role)} replace />
}
