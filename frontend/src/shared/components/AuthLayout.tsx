import { Link, Outlet } from 'react-router'
import { paths } from '../routing/paths'
import '../styles/auth.css'
import { Icon } from './Icon'

export function AuthLayout() {
  return (
    <div className="auth">
      <section className="intro" aria-label="Giới thiệu Learning AI">
        <span className="auth-shape auth-shape-ring" aria-hidden="true" />
        <span className="auth-shape auth-shape-square" aria-hidden="true" />
        <span className="auth-shape auth-shape-circle" aria-hidden="true" />
        <Link className="auth-logo" to={paths.explore}>
          <span className="auth-logo-mark"><Icon name="cap" size={20} /></span>
          <b>Learning AI</b>
        </Link>
        <div className="auth-introduction">
          <h2>Học miễn phí. Giảng viên soạn bài nhanh hơn với AI.</h2>
          <p>Video, tài liệu, quiz và flashcard trong một nơi. AI chỉ tạo bản nháp — giảng viên luôn là người duyệt trước khi xuất bản.</p>
          <ul>
            <li><Icon name="check" />Mọi khóa học đều miễn phí</li>
            <li><Icon name="check" />Học theo tiến độ của riêng bạn</li>
            <li><Icon name="check" />Ôn tập bằng quiz và flashcard</li>
          </ul>
        </div>
      </section>
      <main className="side">
        <Outlet />
      </main>
    </div>
  )
}
