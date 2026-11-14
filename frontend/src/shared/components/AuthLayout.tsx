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
          <h2>Nền tảng học tập trực tuyến tích hợp AI</h2>
          <p>Giảng viên tạo khóa học, bài học, quiz và flashcard.</p>
          <ul>
            <li><Icon name="check" /><span>AI sinh bản nháp quiz/flashcard để giảng viên sửa và duyệt; không bao giờ tự xuất bản.</span></li>
            <li><Icon name="check" /><span>Sinh viên ghi danh, học, làm quiz, xem kết quả và ôn flashcard.</span></li>
          </ul>
        </div>
      </section>
      <main className="side">
        <Outlet />
      </main>
    </div>
  )
}
