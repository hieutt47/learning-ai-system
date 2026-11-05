import { Link, Outlet } from 'react-router'
import { paths } from '../routing/paths'
import { Icon } from './Icon'

/** Khung trang đăng nhập/đăng ký: không có sidebar, nội dung ở giữa (người 1 làm chi tiết theo prototype). */
export function AuthLayout() {
  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 24, padding: 32 }}>
      <Link className="logo" to={paths.explore}>
        <span className="logo-mk">
          <Icon name="cap" size={18} />
        </span>
        <b>Learning AI</b>
      </Link>
      <div style={{ width: '100%', maxWidth: 440 }}>
        <Outlet />
      </div>
    </div>
  )
}
