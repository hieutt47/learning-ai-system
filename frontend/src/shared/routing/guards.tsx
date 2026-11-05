import { Navigate, Outlet, useLocation } from 'react-router'
import { useCurrentUser, type ViewerRole } from '../auth/currentUser'
import { ForbiddenState } from '../components/PageState'
import { homePathFor, paths } from './paths'

export function HomeRedirect() {
  const { role } = useCurrentUser()
  return <Navigate to={homePathFor(role)} replace />
}

/**
 * Chặn trang theo vai trò (bảng ACCESS của prototype). Chỉ là chặn phía UI; backend vẫn kiểm tra quyền.
 * - allow="auth": mọi người đã đăng nhập
 * - allow=['TEACHER']...: chỉ các vai trò này
 * Khách vào trang cần đăng nhập → chuyển sang /login, giữ trang đích; sai vai trò → trang 403.
 */
export function RequireAccess({ allow }: { allow: 'auth' | Exclude<ViewerRole, 'GUEST'>[] }) {
  const { role } = useCurrentUser()
  const { pathname, search } = useLocation()

  if (role === 'GUEST') {
    return <Navigate to={`${paths.login}?redirect=${encodeURIComponent(pathname + search)}`} replace />
  }
  if (allow !== 'auth' && !allow.includes(role)) {
    return <ForbiddenState message="Trang này dành cho vai trò khác với tài khoản của bạn." />
  }
  return <Outlet />
}

/** Trang chỉ cho khách (đăng nhập, đăng ký): đã đăng nhập thì về trang chủ. */
export function GuestOnly() {
  const { role } = useCurrentUser()
  return role === 'GUEST' ? <Outlet /> : <HomeRedirect />
}
